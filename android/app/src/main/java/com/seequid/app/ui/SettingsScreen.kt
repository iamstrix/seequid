package com.seequid.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seequid.app.BuildConfig
import com.seequid.app.data.AppSettings
import com.seequid.app.domain.HydrationCalculator
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    isPro: Boolean,
    onGoal: (Int) -> Unit,
    onOpacity: (Float) -> Unit,
    onThreshold: (Float) -> Unit,
    onManageSubscription: () -> Unit,
    onRestore: () -> Unit,
    onUpgrade: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            // Sliders keep a local value while dragging and save on release.
            var goal by remember(settings.hydration.dailyGoalMl) { mutableFloatStateOf(settings.hydration.dailyGoalMl.toFloat()) }
            var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
            var threshold by remember(settings.handleThreshold) { mutableFloatStateOf(settings.handleThreshold) }

            SectionTitle("Pace")
            SliderRow("Daily goal: ${(goal / 50).roundToInt() * 50} ml", goal,
                HydrationCalculator.MIN_GOAL_ML.toFloat()..HydrationCalculator.MAX_GOAL_ML.toFloat(),
                { goal = it }, { onGoal((goal / 50).roundToInt() * 50) })
            Text(
                "Waking hours: ${settings.hydration.wakeMinute / 60}:00–${settings.hydration.sleepMinute / 60}:00 " +
                    "(no water outside them)",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionTitle("Water")
            SliderRow("Opacity: ${(opacity * 100).roundToInt()}%", opacity,
                AppSettings.MIN_OPACITY..AppSettings.MAX_OPACITY, { opacity = it }, { onOpacity(opacity) })
            Text(
                "Kept below 80% on purpose — above that, Android stops your taps from reaching the app underneath.",
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SliderRow("Show the drop at ${(threshold * 100).roundToInt()}% water", threshold,
                0.1f..0.9f, { threshold = it }, { onThreshold(threshold) })

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            SectionTitle("seequid Pro")
            if (isPro) {
                ListItem(
                    headlineContent = { Text("Manage subscription") },
                    supportingContent = { Text("Change plan, cancel or get help") },
                    modifier = Modifier.clickableRow(onManageSubscription),
                )
            } else {
                ListItem(
                    headlineContent = { Text("Upgrade to Pro") },
                    supportingContent = { Text("All liquids and your drinking history") },
                    modifier = Modifier.clickableRow(onUpgrade),
                )
            }
            ListItem(
                headlineContent = { Text("Restore purchases") },
                modifier = Modifier.clickableRow(onRestore),
            )

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                "seequid is a habit tool, not medical advice. Daily goals are capped at 4 L because drinking " +
                    "far beyond your needs can be harmful. If you have a heart or kidney condition, " +
                    "ask your doctor how much to drink.\n\nVersion ${BuildConfig.VERSION_NAME}",
                Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value, onChange, valueRange = range, onValueChangeFinished = onDone)
    }
}

private fun Modifier.clickableRow(onClick: () -> Unit): Modifier = clickable(onClick = onClick)
