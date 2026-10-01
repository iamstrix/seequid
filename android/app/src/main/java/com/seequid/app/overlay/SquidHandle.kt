package com.seequid.app.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.PathParser
import com.seequid.app.R
import kotlin.math.hypot

/**
 * The squid on the screen edge while the water is up, replacing a plain drop button.
 *  - Tap it to log a drink.
 *  - Drag it anywhere; it snaps to the nearer side and remembers where you put it.
 *  - Left alone for a few seconds it squishes into the edge, leaving one eye peeking out, so it
 *    never sits on top of another app's buttons. Tap the squished squid to pop it back out.
 * The same idea as AssistiveTouch or chat heads.
 */
class SquidHandle(
    private val context: Context,
    private val windowManager: WindowManager,
    private val onTap: () -> Unit,
    /** Where the squid may sit, in raw display px (inside the status and navigation bars). */
    private val bounds: () -> Rect,
) {
    private val prefs = context.getSharedPreferences("squid_handle", Context.MODE_PRIVATE)
    private val density = context.resources.displayMetrics.density
    private fun dp(v: Float) = (v * density).toInt()

    private val bubble = dp(56f)
    private val squished = dp(22f)
    private val margin = dp(8f)
    private val slop = ViewConfiguration.get(context).scaledTouchSlop

    private var onRight = prefs.getBoolean(KEY_RIGHT, true)
    private var yFraction = prefs.getFloat(KEY_Y, 0.55f)
    private var tucked = false
    private var shown = false
    private var attached = false
    private val handler = Handler(Looper.getMainLooper())
    private val tuck = Runnable { setTucked(true) }
    private var slide: ValueAnimator? = null

    val view = SquidBubbleView(context)

    private val params = WindowManager.LayoutParams(
        bubble, bubble,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        title = "seequid squid"
        // Positions are raw display coordinates; we keep clear of the system bars ourselves.
        if (Build.VERSION.SDK_INT >= 30) fitInsetsTypes = 0
        else flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
    }

    init {
        view.contentDescription = context.getString(R.string.handle_description)
        view.visibility = View.GONE
        // Click (also what TalkBack's double-tap triggers): pop out if squished, otherwise log.
        view.setOnClickListener {
            if (tucked) setTucked(false) else onTap()
            scheduleTuck()
        }
        view.setOnTouchListener(DragListener())
    }

    fun attach() {
        if (attached) return
        place()
        windowManager.addView(view, params)
        attached = true
    }

    fun detach() {
        handler.removeCallbacksAndMessages(null)
        slide?.cancel()
        if (attached) windowManager.removeView(view)
        attached = false
    }

    /** Show while the water is high enough; each time it appears it starts out, then squishes. */
    fun setVisible(visible: Boolean) {
        if (visible == shown) return
        shown = visible
        view.visibility = if (visible) View.VISIBLE else View.GONE
        if (visible) {
            tucked = false
            place()
            scheduleTuck()
        } else {
            handler.removeCallbacks(tuck)
        }
    }

    fun setThirsty(thirsty: Boolean) {
        if (view.thirsty != thirsty) {
            view.thirsty = thirsty
            view.invalidate()
        }
    }

    /** Screen size or rotation changed. */
    fun relayout() = place()

    private fun scheduleTuck() {
        handler.removeCallbacks(tuck)
        handler.postDelayed(tuck, TUCK_DELAY_MS)
    }

    private fun setTucked(value: Boolean) {
        if (tucked == value) return
        tucked = value
        place(animate = true)
    }

    private fun restingX(b: Rect, width: Int) =
        if (onRight) b.right - width - (if (tucked) 0 else margin) else b.left + (if (tucked) 0 else margin)

    private fun place(animate: Boolean = false) {
        val b = bounds()
        val width = if (tucked) squished else bubble
        params.width = width
        params.height = bubble
        params.y = b.top + (yFraction * (b.height() - bubble).coerceAtLeast(1)).toInt()
        view.tucked = tucked
        view.onRight = onRight
        view.invalidate()
        val targetX = restingX(b, width)
        slide?.cancel()
        if (animate && attached) {
            slide = ValueAnimator.ofInt(params.x, targetX).apply {
                duration = 220
                interpolator = DecelerateInterpolator()
                addUpdateListener {
                    params.x = it.animatedValue as Int
                    if (attached) windowManager.updateViewLayout(view, params)
                }
                start()
            }
        } else {
            params.x = targetX
            if (attached) windowManager.updateViewLayout(view, params)
        }
    }

    private inner class DragListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var dragging = false

        @SuppressLint("ClickableViewAccessibility") // taps go through performClick() below
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params.x; startY = params.y
                    dragging = false
                    handler.removeCallbacks(tuck)
                    slide?.cancel()
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!dragging && hypot(dx, dy) > slop) {
                        dragging = true
                        if (tucked) {
                            // Pop out to full size under the finger before following it.
                            tucked = false
                            view.tucked = false
                            params.width = bubble
                            startX = if (onRight) startX - (bubble - squished) else startX
                        }
                    }
                    if (dragging) {
                        params.x = (startX + dx).toInt()
                        params.y = (startY + dy).toInt()
                        windowManager.updateViewLayout(view, params)
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val b = bounds()
                        onRight = e.rawX > b.centerX()
                        yFraction = ((params.y - b.top).toFloat() / (b.height() - bubble).coerceAtLeast(1)).coerceIn(0f, 1f)
                        prefs.edit().putBoolean(KEY_RIGHT, onRight).putFloat(KEY_Y, yFraction).apply()
                        place(animate = true)
                        scheduleTuck()
                    } else {
                        v.performClick()
                    }
                }
                MotionEvent.ACTION_CANCEL -> if (dragging) { place(animate = true); scheduleTuck() }
            }
            return true
        }
    }

    private companion object {
        const val KEY_RIGHT = "on_right"
        const val KEY_Y = "y_fraction"
        const val TUCK_DELAY_MS = 3000L
    }
}

/**
 * Draws the squid bubble: a blue circle with the white squid's head when out, or a narrow
 * squished pill with one eye peeking when tucked into the edge.
 */
class SquidBubbleView(context: Context) : View(context) {
    var tucked = false
    var onRight = true
    var thirsty = false

    private val density = resources.displayMetrics.density
    private val blue = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF2E86F5.toInt() }
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE; color = 0xFFFFFFFF.toInt(); strokeWidth = 2f * density
    }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF0A1A3A.toInt() }
    private val inkLine = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0A1A3A.toInt(); style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND; strokeWidth = 16f
    }
    private val blush = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB3FF7470.toInt() }
    private val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x40000000 }

    // Same geometry as the app's squid (1024-unit canvas).
    private val mantle = PathParser.createPathFromPathData(
        "M512,200 C610,250 668,350 668,470 L668,600 Q512,650 356,600 L356,470 C356,350 414,250 512,200 Z"
    )
    private val fins = PathParser.createPathFromPathData(
        "M410,330 Q320,310 300,370 Q350,405 400,400 Z M614,330 Q704,310 724,370 Q674,405 624,400 Z"
    )
    private val smile = PathParser.createPathFromPathData("M482,545 Q512,572 542,545")
    private val tiredEyes = PathParser.createPathFromPathData("M434,484 L490,484 M534,484 L590,484")

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val inset = 2f * density
        if (tucked) {
            // A squished squid: a narrow pill hugging the edge, one eye looking into the screen.
            val pill = RectF(inset, inset, w - inset, h - inset)
            canvas.drawRoundRect(pill, w / 2, w / 2, blue)
            canvas.drawRoundRect(pill, w / 2, w / 2, rim)
            val eyeX = if (onRight) w * 0.42f else w * 0.58f
            val eyeY = h * 0.45f
            canvas.drawCircle(eyeX, eyeY, w * 0.24f, white)
            canvas.drawCircle(eyeX + (if (onRight) -1 else 1) * w * 0.05f, eyeY, w * 0.13f, ink)
            return
        }
        val r = minOf(w, h) / 2 - inset
        canvas.drawCircle(w / 2, h / 2 + density, r, shadow)
        canvas.drawCircle(w / 2, h / 2, r, blue)
        canvas.drawCircle(w / 2, h / 2, r, rim)
        // The squid's head, fitted into the bubble.
        val s = (r * 1.25f) / 424f
        canvas.save()
        canvas.translate(w / 2, h / 2 + r * 0.12f)
        canvas.scale(s, s)
        canvas.translate(-512f, -440f)
        canvas.drawPath(fins, white)
        canvas.drawPath(mantle, white)
        if (thirsty) {
            canvas.drawPath(tiredEyes, inkLine)
        } else {
            listOf(462f, 562f).forEach { x ->
                canvas.drawCircle(x, 480f, 34f, ink)
                canvas.drawCircle(x + 12f, 468f, 11f, white)
            }
        }
        canvas.drawOval(RectF(390f, 518f, 438f, 542f), blush)
        canvas.drawOval(RectF(586f, 518f, 634f, 542f), blush)
        canvas.drawPath(smile, inkLine)
        canvas.restore()
    }
}
