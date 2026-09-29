package com.seequid.app.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
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
                title = { Text("seequid", fontWeight = FontWeight.Bold) },
                actions = {
                    if (!isPro) TextButton(onClick = onUpgrade) { Text("Go Pro") }
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

            Spacer(Modifier.height(8.dp))
            Glass(logged.toFloat() / goal, skin, Modifier.size(width = 150.dp, height = 210.dp))
            Spacer(Modifier.height(16.dp))
            Text("$logged / $goal ml", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(tideLine(state), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = { onLog(250) }) { Text("+250 ml") }
                Button(onClick = { onLog(500) }) { Text("+500 ml") }
                FilledTonalButton(onClick = onUndo) {
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo last drink")
                }
            }

            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = { Text("Water overlay") },
                    supportingContent = {
                        Text(if (canDrawOverlays) "Rises over every app when you fall behind"
                        else "Needs “Display over other apps” permission")
                    },
                    trailingContent = { Switch(settings.overlayEnabled, onOverlayToggle) },
                )
                ListItem(
                    headlineContent = { Text("Demo mode") },
                    supportingContent = { Text("A whole day's pace in 10 minutes") },
                    trailingContent = { Switch(settings.hydration.demoMode, onDemoToggle) },
                )
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
                            Text("Unlock your history with Pro", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun tideLine(state: HydrationState?): String = when {
    state == null -> ""
    state.quiet -> "Quiet hours — the tide is out"
    state.level <= 0.005f -> "On pace. Your screen is dry."
    else -> {
        val behind = (state.expectedMl - state.loggedMl).coerceAtLeast(0)
        "The tide is at ${(state.level * 100).toInt()}% — about $behind ml to clear it"
    }
}
