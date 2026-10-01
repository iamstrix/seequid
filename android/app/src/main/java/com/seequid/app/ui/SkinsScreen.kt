package com.seequid.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.seequid.app.overlay.LiquidSkin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SkinsScreen(
    selected: LiquidSkin,
    isPro: Boolean,
    onSelect: (LiquidSkin) -> Unit,
    onLocked: () -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Liquids") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(padding),
        ) {
            if (!isPro) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        onClick = onLocked,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Unlock 4 more drinks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Change the water on your screen to matcha, cold brew, boba tea or night lagoon.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.width(12.dp))
                            Button(onClick = onLocked) { Text("Go Pro") }
                        }
                    }
                }
            }
            items(LiquidSkin.entries) { skin ->
                val locked = skin.isPro && !isPro
                val isSelected = skin == selected && !locked
                Card(
                    onClick = { if (locked) onLocked() else onSelect(skin) },
                    border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Box {
                        Column(
                            Modifier.padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Glass(0.6f, skin, Modifier.fillMaxWidth().height(120.dp).alpha(if (locked) 0.5f else 1f))
                            Spacer(Modifier.height(10.dp))
                            Text(skin.displayName, style = MaterialTheme.typography.titleSmall)
                        }
                        Row(
                            Modifier.align(Alignment.TopEnd).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (skin.isPro) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Text("PRO", Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            if (locked) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.Lock, "Locked", Modifier.size(18.dp))
                            }
                            if (isSelected) {
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.Default.CheckCircle, "Selected", Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}
