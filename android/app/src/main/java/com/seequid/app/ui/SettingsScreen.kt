package com.seequid.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import com.seequid.app.R
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
    onReplayIntro: () -> Unit,
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
        Column(Modifier.padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState())) {
            // Sliders keep a local value while dragging and save on release.
            var goal by remember(settings.hydration.dailyGoalMl) { mutableFloatStateOf(settings.hydration.dailyGoalMl.toFloat()) }
            var opacity by remember(settings.opacity) { mutableFloatStateOf(settings.opacity) }
            var threshold by remember(settings.handleThreshold) { mutableFloatStateOf(settings.handleThreshold) }

            ProCard(isPro, onUpgrade = onUpgrade, onManage = onManageSubscription, onRestore = onRestore)

            SectionTitle("Pace")
            SettingsCard {
                SliderRow("Daily goal: ${liters((goal / 50).roundToInt() * 50)}", goal,
                    HydrationCalculator.MIN_GOAL_ML.toFloat()..HydrationCalculator.MAX_GOAL_ML.toFloat(),
                    { goal = it }, { onGoal((goal / 50).roundToInt() * 50) })
                Hint("Waking hours: ${settings.hydration.wakeMinute / 60}:00–${settings.hydration.sleepMinute / 60}:00 " +
                    "(no water outside them)")
            }

            SectionTitle("Water")
            SettingsCard {
                SliderRow("Opacity: ${(opacity * 100).roundToInt()}%", opacity,
                    AppSettings.MIN_OPACITY..AppSettings.MAX_OPACITY, { opacity = it }, { onOpacity(opacity) })
                Hint("Kept below 80% on purpose — above that, Android stops your taps from reaching the app underneath.")
                Spacer(Modifier.height(12.dp))
                SliderRow("Show the drop at ${(threshold * 100).roundToInt()}% water", threshold,
                    0.1f..0.9f, { threshold = it }, { onThreshold(threshold) })
            }

            SectionTitle("Help")
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("How Seequid works") },
                    supportingContent = { Text("Replay the intro — your settings stay as they are") },
                    leadingContent = { Icon(Icons.Default.School, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable(onClick = onReplayIntro),
                )
            }

            Text(
                "Seequid is a habit tool, not medical advice. Daily goals are capped at 4 L because drinking " +
                    "far beyond your needs can be harmful. If you have a heart or kidney condition, " +
                    "ask your doctor how much to drink.\n\nVersion ${BuildConfig.VERSION_NAME}",
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProCard(isPro: Boolean, onUpgrade: () -> Unit, onManage: () -> Unit, onRestore: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painterResource(R.mipmap.ic_launcher_foreground), contentDescription = null,
                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).graphicsLayer(scaleX = 1.5f, scaleY = 1.5f),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Seequid Pro", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (isPro) "Active ✓ — thanks for supporting a student dev" else "All liquids and your drinking history",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isPro) {
                FilledTonalButton(onClick = onManage) { Text("Manage subscription") }
            } else {
                Button(onClick = onUpgrade) { Text("Go Pro") }
            }
            TextButton(onClick = onRestore) { Text("Restore purchases") }
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(vertical = 12.dp), content = content) }
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

