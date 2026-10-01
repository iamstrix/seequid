package com.seequid.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import com.seequid.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seequid.app.container
import com.seequid.app.data.AppSettings
import com.seequid.app.domain.HydrationCalculator
import com.seequid.app.overlay.LiquidSkin
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Value first, permission second, paywall last: the user sees the water
 * working on their own phone before being asked to pay.
 */
@Composable
fun OnboardingScreen(
    settings: AppSettings,
    canDrawOverlays: Boolean,
    /** Opened from Settings: same pages, prefilled, and it simply ends instead of starting the app. */
    replay: Boolean = false,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableIntStateOf(0) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    BackHandler(enabled = step > 0 || replay) { if (step > 0) step-- else onFinished() }

    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        StepDots(step, total = 4, Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
        AnimatedContent(targetState = step, label = "onboarding", modifier = Modifier.padding(top = 24.dp)) { current ->
            when (current) {
                0 -> Page(
                    title = "Your screen is thirsty",
                    body = "Seequid slowly fills your phone with water as the day goes on. " +
                        "Fall behind and the tide rises over everything you do. Drink, and it drains away.",
                    demoFill = 0.45f,
                    primary = "Show me how" to { step = 1 },
                )
                1 -> Page(
                    title = "No nagging. Just a tide.",
                    body = "No notifications to swipe away. The water is always there, " +
                        "see-through and tap-through, so you can keep using your phone. " +
                        "When it gets high, tap the drop to log a drink.",
                    demoFill = 0.7f,
                    primary = "Set my goal" to { step = 2 },
                )
                2 -> GoalPage(settings) { goal, wake, sleep ->
                    scope.launch {
                        context.container.settings.setGoal(goal)
                        context.container.settings.setWakeHours(wake, sleep)
                        step = 3
                    }
                }
                else -> PermissionPage(
                    granted = canDrawOverlays,
                    finishLabel = if (replay) "Done" else "Start the tide",
                    onGrant = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}"),
                            )
                        )
                    },
                    onStart = {
                        if (replay) {
                            onFinished()
                            return@PermissionPage
                        }
                        if (Build.VERSION.SDK_INT >= 33) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scope.launch {
                            context.container.settings.setOverlayEnabled(true)
                            context.container.settings.setOnboardingDone()
                            onFinished()
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun Page(title: String, body: String, demoFill: Float, primary: Pair<String, () -> Unit>) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        PhoneMock(demoFill)
        Spacer(Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Button(onClick = primary.second, modifier = Modifier.fillMaxWidth()) { Text(primary.first) }
    }
}

/** A tiny phone with fake app rows and water over them. */
@Composable
private fun PhoneMock(fill: Float) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(width = 150.dp, height = 260.dp),
    ) {
        Box {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(6) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)) {}
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                            modifier = Modifier.height(10.dp).fillMaxWidth()) {}
                    }
                }
            }
            Glass(fill, LiquidSkin.WATER, Modifier.fillMaxSize().padding(2.dp).then(Modifier))
        }
    }
}

@Composable
private fun GoalPage(settings: AppSettings, onNext: (goal: Int, wake: Int, sleep: Int) -> Unit) {
    var goal by remember { mutableFloatStateOf(settings.hydration.dailyGoalMl.toFloat()) }
    var wake by remember { mutableFloatStateOf(settings.hydration.wakeMinute / 60f) }
    var sleep by remember { mutableFloatStateOf(settings.hydration.sleepMinute / 60f) }
    val goalMl = (goal / 50).roundToInt() * 50
    // Blue sets an amount of water; the time sliders are plain controls. Coral stays on the one action.
    val waterSlider = SliderDefaults.colors(
        thumbColor = Aqua, activeTrackColor = Aqua,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    )
    val neutralSlider = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.onSurface,
        activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
        activeTickColor = MaterialTheme.colorScheme.surfaceVariant,
        inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(36.dp))
        Glass(
            goal / HydrationCalculator.MAX_GOAL_ML, LiquidSkin.WATER,
            Modifier.size(width = 96.dp, height = 128.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(24.dp))
        Text("Your daily pace", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("The water rises when you fall behind this pace, and stays away while you sleep.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Daily goal: ${liters(goalMl)}", style = MaterialTheme.typography.titleMedium)
        Slider(goal, { goal = it }, colors = waterSlider,
            valueRange = HydrationCalculator.MIN_GOAL_ML.toFloat()..HydrationCalculator.MAX_GOAL_ML.toFloat())
        Text("Capped at 4 L — more isn't healthier. Not medical advice; ask a doctor if you have a condition.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text("Awake from ${clock(wake.roundToInt())} to ${clock(sleep.roundToInt())}",
            style = MaterialTheme.typography.titleMedium)
        Text("Wake", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(wake, { wake = it.coerceAtMost(sleep - 4) }, valueRange = 4f..12f, steps = 7, colors = neutralSlider)
        Text("Sleep", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Slider(sleep, { sleep = it.coerceAtLeast(wake + 4) }, valueRange = 18f..24f, steps = 5, colors = neutralSlider)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onNext(goalMl, wake.roundToInt() * 60, sleep.roundToInt() * 60) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Continue") }
    }
}

/** 7 -> "7:00 AM", 23 -> "11:00 PM", 24 -> "12:00 AM". */
private fun clock(hour: Int): String =
    LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

@Composable
private fun PermissionPage(granted: Boolean, finishLabel: String, onGrant: () -> Unit, onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null,
            Modifier.size(96.dp).clip(RoundedCornerShape(28.dp)).graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
        )
        Spacer(Modifier.height(24.dp))
        Text("Let the water in", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Seequid needs “Display over other apps” to draw the water above your screen.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Reassurance(Icons.Default.VisibilityOff, "It never reads what's on your screen")
                Reassurance(Icons.Default.TouchApp, "Your taps go straight through the water")
                Reassurance(Icons.Default.PauseCircle, "Pause it any time from the notification")
            }
        }
        Spacer(Modifier.height(28.dp))
        if (!granted) {
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("Open settings") }
            Spacer(Modifier.height(8.dp))
            Text("Find Seequid in the list and switch it on, then come back.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text("Skip for now") }
        } else {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.width(6.dp))
                    Text("Permission granted", color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(finishLabel) }
        }
    }
}

@Composable
private fun Reassurance(icon: ImageVector, text: String) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Aqua)
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StepDots(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(total) { i ->
            val active = i == step
            Surface(
                shape = CircleShape,
                color = if (i <= step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.height(8.dp).width(if (active) 24.dp else 8.dp),
            ) {}
        }
    }
}
