package com.seequid.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.hardware.input.InputManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.seequid.app.R
import com.seequid.app.container
import com.seequid.app.data.AppSettings
import com.seequid.app.domain.HydrationState
import com.seequid.app.ui.MainActivity
import com.seequid.app.ui.QuickLogActivity
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * Hosts the two overlay windows:
 *  - the water: sized to the water height, never touchable, window alpha < 0.8
 *    so taps pass through to the app underneath (Android 12+ occlusion rule);
 *  - the drop handle: a small touchable bubble shown only once the user is
 *    behind, which opens the quick-log sheet.
 */
class OverlayService : LifecycleService() {

    private lateinit var windowManager: WindowManager
    private lateinit var water: WaterView
    private lateinit var handle: ImageView
    private lateinit var waterParams: WindowManager.LayoutParams
    private lateinit var handleParams: WindowManager.LayoutParams

    private var screenOn = true
    private var last: Triple<HydrationState, AppSettings, Boolean>? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            screenOn = intent.action != Intent.ACTION_SCREEN_OFF
            last?.let { (state, settings, pro) -> render(state, settings, pro) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WindowManager::class.java)
        createChannel()
        // Must precede any stopSelf(): a service started with startForegroundService()
        // that stops before calling startForeground() crashes the app.
        promoteToForeground()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        addWindows()
        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        observe()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        // Every startForegroundService() call must be answered, even when already running.
        promoteToForeground()
        when (intent?.action) {
            ACTION_LOG -> lifecycleScope.launch { container.hydration.log(QUICK_LOG_ML) }
            ACTION_PAUSE -> lifecycleScope.launch {
                container.settings.setOverlayEnabled(false)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun promoteToForeground() {
        val progress = last?.first?.let { it.loggedMl to it.goalMl }
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(progress),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
    }

    private fun observe() {
        val c = container
        lifecycleScope.launch {
            combine(c.hydration.state, c.settings.settings, c.billing.isPro) { state, settings, pro ->
                Triple(state, settings, pro)
            }.collect { (state, settings, pro) ->
                last = Triple(state, settings, pro)
                render(state, settings, pro)
            }
        }
        lifecycleScope.launch {
            c.hydration.state
                .map { it.loggedMl to it.goalMl }
                .distinctUntilChanged()
                .collect { (logged, goal) ->
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(logged to goal))
                }
        }
    }

    private fun addWindows() {
        water = WaterView(this)
        waterParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            alpha = AppSettings.DEFAULT_OPACITY
            title = "seequid water"
        }
        water.visibility = View.GONE
        windowManager.addView(water, waterParams)

        val size = dp(52)
        handle = ImageView(this).apply {
            setImageResource(R.drawable.ic_drop)
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xF2FFFFFF.toInt())
                setStroke(dp(2), 0xFF3D9BFF.toInt())
            }
            elevation = dp(6).toFloat()
            contentDescription = getString(R.string.handle_description)
            visibility = View.GONE
            setOnClickListener { openQuickLog() }
        }
        handleParams = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = dp(16)
            title = "seequid handle"
        }
        windowManager.addView(handle, handleParams)
    }

    private fun render(state: HydrationState, settings: AppSettings, isPro: Boolean) {
        if (!::water.isInitialized) return
        val skin = if (settings.skin.isPro && !isPro) LiquidSkin.WATER else settings.skin
        val visible = !state.quiet && state.level > 0.005f
        val screenHeight = screenHeight()
        val waterPx = (state.level * screenHeight).toInt()

        water.setSkin(skin)
        water.animating = visible && screenOn
        water.visibility = if (visible) View.VISIBLE else View.GONE

        val targetHeight = if (visible) waterPx + water.crestPaddingPx else 1
        val targetAlpha = safeOpacity(settings.opacity)
        if (abs(waterParams.height - targetHeight) >= 2 || waterParams.alpha != targetAlpha) {
            waterParams.height = targetHeight
            waterParams.alpha = targetAlpha
            windowManager.updateViewLayout(water, waterParams)
        }

        val showHandle = visible && state.level >= settings.handleThreshold
        handle.visibility = if (showHandle) View.VISIBLE else View.GONE
        val handleY = (waterPx + dp(20)).coerceAtMost(screenHeight - dp(160))
        if (showHandle && handleParams.y != handleY) {
            handleParams.y = handleY
            windowManager.updateViewLayout(handle, handleParams)
        }
    }

    /**
     * Android discards touches that pass through an untrusted overlay whose
     * *window* alpha exceeds the system maximum (0.8 by default, lower on some
     * OEMs). Stay safely beneath whatever this device reports.
     */
    private fun safeOpacity(requested: Float): Float {
        val max = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(InputManager::class.java).maximumObscuringOpacityForTouch - 0.05f
        } else {
            AppSettings.MAX_OPACITY
        }
        return min(requested, max).coerceAtLeast(0.1f)
    }

    private fun screenHeight(): Int =
        if (Build.VERSION.SDK_INT >= 30) windowManager.currentWindowMetrics.bounds.height()
        else resources.displayMetrics.heightPixels

    private fun openQuickLog() {
        startActivity(
            Intent(this, QuickLogActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        last?.let { (state, settings, pro) -> render(state, settings, pro) }
    }

    override fun onDestroy() {
        if (::water.isInitialized) {
            runCatching { unregisterReceiver(screenReceiver) }
            windowManager.removeView(water)
            windowManager.removeView(handle)
        }
        super.onDestroy()
    }

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(progress: Pair<Int, Int>?): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val log = PendingIntent.getService(
            this, 1, Intent(this, OverlayService::class.java).setAction(ACTION_LOG),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val pause = PendingIntent.getService(
            this, 2, Intent(this, OverlayService::class.java).setAction(ACTION_PAUSE),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val text = progress?.let { (logged, goal) -> getString(R.string.notification_progress, logged, goal) }
            ?: getString(R.string.notification_text)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_drop)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(text)
            .setContentIntent(open)
            .addAction(R.drawable.ic_drop, getString(R.string.action_log, QUICK_LOG_ML), log)
            .addAction(R.drawable.ic_drop, getString(R.string.action_pause), pause)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        private const val CHANNEL_ID = "overlay"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_LOG = "com.seequid.app.action.LOG"
        private const val ACTION_PAUSE = "com.seequid.app.action.PAUSE"
        private const val QUICK_LOG_ML = 250

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, OverlayService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, OverlayService::class.java))
        }
    }
}
