package com.seequid.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialog
import com.revenuecat.purchases.ui.revenuecatui.PaywallDialogOptions
import com.revenuecat.purchases.ui.revenuecatui.customercenter.CustomerCenter
import com.seequid.app.billing.BillingRepository
import com.seequid.app.container
import com.seequid.app.overlay.OverlayService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeequidTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SeequidRoot()
                }
            }
        }
    }
}

private enum class Screen { Home, Skins, Settings, CustomerCenter, Intro }

@Composable
private fun SeequidRoot() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()

    val settings by c.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val state by c.hydration.state.collectAsStateWithLifecycle(initialValue = null)
    val week by c.hydration.lastSevenDays.collectAsStateWithLifecycle(initialValue = emptyList())
    val isPro by c.billing.isPro.collectAsStateWithLifecycle()

    var canDrawOverlays by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    LifecycleResumeEffect(Unit) {
        canDrawOverlays = Settings.canDrawOverlays(context)
        scope.launch { c.billing.refresh() }
        onPauseOrDispose { }
    }

    var showPaywall by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }

    val s = settings ?: return

    // The service follows the user's switch and the permission, nothing else.
    LaunchedEffect(s.overlayEnabled, canDrawOverlays) {
        if (s.overlayEnabled && canDrawOverlays) OverlayService.start(context) else OverlayService.stop(context)
    }

    fun openOverlayPermission() {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
        )
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    if (!s.onboardingDone) {
        OnboardingScreen(s, canDrawOverlays) {
            // First paywall comes right after the water is live, and only once.
            if (!s.paywallSeen) showPaywall = true
        }
    } else {
        BackHandler(enabled = screen != Screen.Home) { screen = Screen.Home }
        when (screen) {
            Screen.Home -> HomeScreen(
                state = state,
                settings = s,
                isPro = isPro,
                week = week,
                canDrawOverlays = canDrawOverlays,
                onLog = { ml -> scope.launch { c.hydration.log(ml) } },
                onUndo = { scope.launch { c.hydration.undoLast() } },
                onOverlayToggle = { on ->
                    if (on && !canDrawOverlays) openOverlayPermission()
                    scope.launch { c.settings.setOverlayEnabled(on) }
                },
                onDemoToggle = { on -> scope.launch { c.settings.setDemoMode(on) } },
                onOpenSkins = { screen = Screen.Skins },
                onOpenSettings = { screen = Screen.Settings },
                onUpgrade = { showPaywall = true },
            )
            Screen.Skins -> SkinsScreen(
                selected = s.skin,
                isPro = isPro,
                onSelect = { skin -> scope.launch { c.settings.setSkin(skin) } },
                onLocked = { showPaywall = true },
                onBack = { screen = Screen.Home },
            )
            Screen.Settings -> SettingsScreen(
                settings = s,
                isPro = isPro,
                onGoal = { ml -> scope.launch { c.settings.setGoal(ml) } },
                onWakeHours = { wake, sleep -> scope.launch { c.settings.setWakeHours(wake, sleep) } },
                onScheduleEnabled = { on -> scope.launch { c.settings.setScheduleEnabled(on) } },
                onOpacity = { v -> scope.launch { c.settings.setOpacity(v) } },
                onThreshold = { v -> scope.launch { c.settings.setHandleThreshold(v) } },
                onManageSubscription = {
                    if (c.billing.isConfigured) screen = Screen.CustomerCenter else toast("Purchases are not set up in this build")
                },
                onRestore = {
                    scope.launch {
                        c.billing.restore()
                            .onSuccess { found -> toast(if (found) "Your Pro purchase was restored" else "We couldn't find any purchases to restore") }
                            .onFailure { toast("Couldn't restore purchases. Please try again. (${it.message})") }
                    }
                },
                onUpgrade = { showPaywall = true },
                onReplayIntro = { screen = Screen.Intro },
                onBack = { screen = Screen.Home },
            )
            Screen.Intro -> OnboardingScreen(s, canDrawOverlays, replay = true) { screen = Screen.Settings }
            Screen.CustomerCenter -> CustomerCenter(onDismiss = { screen = Screen.Settings })
        }
    }

    if (showPaywall) {
        val dismiss = {
            showPaywall = false
            scope.launch { c.settings.setPaywallSeen() }
            Unit
        }
        if (c.billing.isConfigured) {
            PaywallDialog(
                PaywallDialogOptions.Builder()
                    .setRequiredEntitlementIdentifier(BillingRepository.ENTITLEMENT_PRO)
                    .setDismissRequest(dismiss)
                    .build()
            )
        } else {
            AlertDialog(
                onDismissRequest = dismiss,
                confirmButton = { TextButton(onClick = dismiss) { Text("OK") } },
                title = { Text("Purchases not configured") },
                text = { Text("Add a RevenueCat key as revenuecat.apiKey in local.properties and rebuild.") },
            )
        }
    }
}
