package com.seequid.app.ui

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.coroutines.delay
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.seequid.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.seequid.app.data.AppSettings
import com.seequid.app.data.DayTotal
import com.seequid.app.domain.HydrationState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HydrationState?,
    settings: AppSettings,
    isPro: Boolean,
    week: List<DayTotal>,
    canDrawOverlays: Boolean,
    onLog: (Int) -> Unit,
    onUndo: () -> Unit,
    onOverlayToggle: (Boolean) -> Unit,
    onDemoToggle: (Boolean) -> Unit,
    onOpenSkins: () -> Unit,
    onOpenSettings: () -> Unit,
    onUpgrade: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The mipmap is the 108dp adaptive canvas; zoom to its 72dp visible area.
                        Image(
                            painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null,
                            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    if (!isPro) {
                        // Same coral as the drink buttons, but smaller and without the glow, so it doesn't compete.
                        JellyButton(
                            onClick = onUpgrade,
                            glow = false,
                            height = 36.dp,
                            contentPadding = PaddingValues(horizontal = 14.dp),
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, Modifier.size(16.dp))
                            Text("Go Pro", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                    IconButton(onClick = onOpenSkins) { Icon(Icons.Default.Palette, "Skins") }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Settings") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val logged = state?.loggedMl ?: 0
            val goal = state?.goalMl ?: settings.hydration.dailyGoalMl
            val skin = if (settings.skin.isPro && !isPro) com.seequid.app.overlay.LiquidSkin.WATER else settings.skin

            val mood = squidMood(state, logged, goal)
            var tip by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(tip) {
                if (tip != null) {
                    delay(3500)
                    tip = null
                }
            }
            // The home water means the same as the overlay: it rises when you're behind and drains as you drink.
            // On track it's a calm, shallow pool (never empty, so the squid still has somewhere to float).
            val tide = if (state == null || state.quiet) 0f else state.level
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Glass(
                        0.3f + 0.7f * tide, skin,
                        // Break out of the screen padding so the water runs edge to edge.
                        Modifier.requiredWidth(screenWidth).height(260.dp),
                        mood = mood,
                        drinkTrigger = logged,
                        framed = false,
                        onSquidTap = { tip = squidTip(mood, state) },
                    )
                    // A soft glow under the water so it fades into the stats instead of ending on a hard edge.
                    Box(
                        Modifier
                            .requiredWidth(screenWidth)
                            .height(48.dp)
                            .background(Brush.verticalGradient(listOf(Color(skin.front).copy(alpha = 0.45f), Color.Transparent)))
                    )
                }
                // Keep the bubble off the squid's face: below it when the water is high, above it when low.
                val bubbleAt = if (tide > 0.5f) Alignment.BottomCenter else Alignment.TopCenter
                androidx.compose.animation.AnimatedVisibility(
                    tip != null, Modifier.align(bubbleAt).padding(bottom = 16.dp), enter = fadeIn(), exit = fadeOut(),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shadowElevation = 4.dp,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    ) {
                        Text(
                            tip.orEmpty(),
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
            Text(
                liters(logged),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
            )
            // Progress towards the goal is green, never water: blue only ever means "you need to drink".
            LinearProgressIndicator(
                progress = { (logged.toFloat() / goal).coerceIn(0f, 1f) },
                modifier = Modifier.padding(vertical = 8.dp).width(220.dp).height(8.dp),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
            )
            Text(
                "${logged * 100 / goal.coerceAtLeast(1)}% of your ${liters(goal)} goal",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            if (logged >= goal) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                    Text(
                        "Goal reached 🎉",
                        Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        color = MaterialTheme.colorScheme.background,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Text(
                tideLine(state),
                Modifier.padding(horizontal = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                JellyButton(onClick = { onLog(250) }, haptic = true, contentPadding = PaddingValues(horizontal = 18.dp)) {
                    Icon(CupIcon, contentDescription = null, Modifier.size(20.dp))
                    Text("+250 ml")
                }
                JellyButton(onClick = { onLog(500) }, haptic = true, contentPadding = PaddingValues(horizontal = 18.dp)) {
                    Icon(BottleIcon, contentDescription = null, Modifier.size(20.dp))
                    Text("+500 ml")
                }
                FilledTonalButton(onClick = onUndo, modifier = Modifier.height(56.dp)) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last drink")
                }
            }

            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Water overlay") },
                    supportingContent = {
                        Text(if (canDrawOverlays) "Shows water on top of your other apps when you need to drink"
                        else "Turn on the “Display over other apps” permission to use this")
                    },
                    trailingContent = { Switch(settings.overlayEnabled, onOverlayToggle) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Demo mode", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "Speeds up a full day into 10 minutes, for testing",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(settings.hydration.demoMode, onDemoToggle, Modifier.scale(0.8f))
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Last 7 days", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        if (!isPro) Icon(Icons.Default.Lock, contentDescription = "Pro feature")
                    }
                    Spacer(Modifier.height(12.dp))
                    if (isPro) {
                        WeekBars(week, goal)
                    } else {
                        Box(Modifier.clickable(onClick = onUpgrade), contentAlignment = Alignment.Center) {
                            WeekBars(week.map { it.copy(totalMl = (goal * 0.4 + it.date.dayOfMonth * 97 % goal * 0.6).toInt()) },
                                goal, Modifier.blur(10.dp))
                            Text("See your last 7 days with Pro", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** What the squid says when tapped: plain advice that matches its mood. */
private fun squidTip(mood: SquidMood, state: HydrationState?): String {
    val behind = state?.let { ((it.expectedMl - it.loggedMl).coerceAtLeast(0) + 25) / 50 * 50 } ?: 0
    val toGoal = state?.let { ((it.goalMl - it.loggedMl).coerceAtLeast(0) + 25) / 50 * 50 } ?: 0
    val almostThere = state != null && state.loggedMl >= state.goalMl * ALMOST_THERE
    return when (mood) {
        SquidMood.HAPPY ->
            if (almostThere) "Almost there! Drink about ${liters(toGoal)} more to reach your goal."
            else "You're doing great! Keep drinking a little at a time."
        SquidMood.WORRIED -> "The water is starting to rise. Drink about ${liters(behind)} to bring it down."
        SquidMood.THIRSTY -> "The water is rising fast! Drink about ${liters(behind)} to clear your screen."
        SquidMood.SLEEPING -> "Shh... it's sleep time. See you in the morning!"
        SquidMood.CELEBRATING -> "You reached your goal today! Thank you for the water!"
    }
}

private fun tideLine(state: HydrationState?): String = when {
    state == null -> ""
    state.quiet -> "It's your sleep time, so the water is turned off."
    state.level <= 0.005f -> "You're on track. Your screen is clear."
    else -> {
        // Rounded to 50 ml: "about 2.4 L" reads better than "about 2420 ml".
        val behind = ((state.expectedMl - state.loggedMl).coerceAtLeast(0) + 25) / 50 * 50
        "Water level: ${(state.level * 100).toInt()}%. Drink about ${liters(behind)} to clear your screen."
    }
}
