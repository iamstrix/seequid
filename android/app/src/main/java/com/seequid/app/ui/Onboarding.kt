package com.seequid.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WaterDrop
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
    val pager = rememberPagerState { PAGES }
    fun goTo(page: Int) = scope.launch { pager.animateScrollToPage(page) }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    BackHandler(enabled = pager.currentPage > 0 || replay) {
        if (pager.currentPage > 0) goTo(pager.currentPage - 1) else onFinished()
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        // Back arrow and progress dots; the arrow keeps its space on page 1 so the dots don't jump.
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
            if (pager.currentPage > 0) {
                IconButton(onClick = { goTo(pager.currentPage - 1) }, Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
            StepDots(pager.currentPage, total = PAGES, Modifier.align(Alignment.Center).padding(vertical = 20.dp))
        }
        HorizontalPager(pager, Modifier.weight(1f).padding(horizontal = 24.dp, vertical = 8.dp), pageSpacing = 48.dp) { page ->
            when (page) {
                0 -> Page(
                    title = "Your screen is thirsty",
                    body = "Seequid slowly covers your screen with water during the day. " +
                        "If you don't drink enough, the water rises higher. " +
                        "When you drink and log it, the water goes down.",
                    illustration = { RisingWaterDemo() },
                    primary = "Show me how" to { goTo(1) },
                )
                1 -> Page(
                    title = "No annoying reminders",
                    body = "Seequid doesn't send reminder notifications. Instead, the water stays on your screen. " +
                        "You can see through it and tap through it, so you can use your phone normally. " +
                        "When the water gets high, tap the drop button to log a drink.",
                    illustration = { TapToDrainDemo() },
                    primary = "Set my goal" to { goTo(2) },
                )
                2 -> GoalPage(
                    settings,
                    // Saved as it changes, so swiping past this page never loses it.
                    onGoal = { goal -> scope.launch { context.container.settings.setGoal(goal) } },
                    onNext = { goTo(3) },
                )
                else -> PermissionPage(
                    granted = canDrawOverlays,
                    finishLabel = if (replay) "Done" else "Start Seequid",
                    onGrant = { context.requestOverlayPermission(scope) },
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

private const val PAGES = 4

@Composable
private fun Page(
    title: String,
    body: String,
    illustration: @Composable () -> Unit,
    primary: Pair<String, () -> Unit>,
) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        illustration()
        Spacer(Modifier.height(32.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        JellyButton(onClick = primary.second, modifier = Modifier.fillMaxWidth()) { Text(primary.first) }
    }
}

/** Page 1: the water keeps rising over a phone while the squid gets more and more worried. */
@Composable
private fun RisingWaterDemo() {
    val t by rememberInfiniteTransition(label = "rise").animateFloat(
        0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "t",
    )
    // Rise for most of the loop, then hold at the top for a beat before starting over.
    val fill = 0.1f + 0.7f * (t / 0.8f).coerceAtMost(1f)
    val mood = when {
        fill < 0.35f -> SquidMood.HAPPY
        fill < 0.6f -> SquidMood.WORRIED
        else -> SquidMood.THIRSTY
    }
    PhoneMock(fill, mood)
}

/**
 * Page 2 acts out its text: the water is high, a tap lands on the drop button, the water drains away
 * and the squid cheers up.
 */
@Composable
private fun TapToDrainDemo() {
    val t by rememberInfiniteTransition(label = "tap").animateFloat(
        0f, 1f, infiniteRepeatable(tween(4500, easing = LinearEasing)), label = "t",
    )
    val tapAt = 0.3f
    val drainStart = 0.42f
    val drainEnd = 0.72f
    val high = 0.75f
    val low = 0.12f
    val fill = when {
        t < drainStart -> high
        t < drainEnd -> {
            val k = (t - drainStart) / (drainEnd - drainStart)
            high + (low - high) * (1 - (1 - k) * (1 - k)) // ease out
        }
        else -> low
    }
    Box {
        // The trigger rises once per loop as the drain starts, which makes the squid hop.
        PhoneMock(fill, if (t < 0.5f) SquidMood.THIRSTY else SquidMood.HAPPY, drinkTrigger = if (t >= drainStart) 1 else 0)
        if (t < drainEnd) {
            // The drop button rides just above the waterline, on the right, like the real one.
            val tap = ((t - tapAt) / (drainStart - tapAt)).coerceIn(0f, 1f)
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 60.dp)
                    .size(34.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (t >= tapAt) {
                    // A finger-tap ripple.
                    Box(
                        Modifier
                            .size(34.dp)
                            .graphicsLayer { scaleX = 1f + tap; scaleY = 1f + tap; alpha = 1f - tap }
                            .background(Coral, CircleShape)
                    )
                }
                Box(
                    Modifier
                        .size(30.dp)
                        .graphicsLayer { val press = if (t in tapAt..drainStart) 0.85f else 1f; scaleX = press; scaleY = press }
                        .background(Color.White, CircleShape)
                        .border(2.dp, Aqua, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_drop), contentDescription = null, Modifier.size(16.dp), tint = Aqua)
                }
            }
        }
    }
}

/** A tiny phone with fake app rows and water over them. */
@Composable
private fun PhoneMock(fill: Float, mood: SquidMood, drinkTrigger: Int? = null) {
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
            Glass(fill, LiquidSkin.WATER, Modifier.fillMaxSize().padding(2.dp), mood = mood, drinkTrigger = drinkTrigger)
        }
    }
}

@Composable
private fun GoalPage(settings: AppSettings, onGoal: (goal: Int) -> Unit, onNext: () -> Unit) {
    var goal by remember { mutableFloatStateOf(settings.hydration.dailyGoalMl.toFloat()) }
    val goalMl = (goal / 50).roundToInt() * 50
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
        Spacer(Modifier.height(36.dp))
        Glass(
            goal / HydrationCalculator.MAX_GOAL_ML, LiquidSkin.WATER,
            Modifier.size(width = 96.dp, height = 128.dp).align(Alignment.CenterHorizontally),
        )
        Spacer(Modifier.height(24.dp))
        Text("Your daily goal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Choose how much water you want to drink each day. If you drink less than you should " +
            "by that time of day, the water on your screen rises.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        Text("Daily goal: ${liters(goalMl)}", style = MaterialTheme.typography.titleMedium)
        Slider(goal, { goal = it }, colors = waterSliderColors(), onValueChangeFinished = { onGoal(goalMl) },
            valueRange = HydrationCalculator.MIN_GOAL_ML.toFloat()..HydrationCalculator.MAX_GOAL_ML.toFloat())
        Text("The maximum is 4 L, because drinking too much water can be harmful. This is not medical advice. " +
            "If you have a health condition, ask your doctor.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        JellyButton(onClick = { onGoal(goalMl); onNext() }, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
    }
}

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
        Text("Allow Seequid to show water", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "To show water on top of your other apps, Seequid needs a permission called “Display over other apps”.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 8.dp)) {
                Reassurance(Icons.Default.VisibilityOff, "Seequid never reads or records your screen")
                Reassurance(Icons.Default.TouchApp, "Your taps still work. They pass through the water.")
                Reassurance(Icons.Default.PauseCircle, "You can pause the water anytime from the notification")
                Reassurance(Icons.Default.WaterDrop, "If you see blue water on your screen, it's Seequid, not a problem with your phone")
            }
        }
        Spacer(Modifier.height(28.dp))
        if (!granted) {
            JellyButton(onClick = onGrant, modifier = Modifier.fillMaxWidth()) { Text("Open phone settings") }
            Spacer(Modifier.height(8.dp))
            Text("In the list, find Seequid and turn it on. Seequid will bring you back here. If it doesn't, press the Back button.",
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
            JellyButton(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(finishLabel) }
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
