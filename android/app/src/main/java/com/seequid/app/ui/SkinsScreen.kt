package com.seequid.app.ui

import androidx.compose.foundation.BorderStroke
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
            items(LiquidSkin.entries) { skin ->
                val locked = skin.isPro && !isPro
                val isSelected = skin == selected && !locked
                Card(
                    onClick = { if (locked) onLocked() else onSelect(skin) },
                    border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Glass(0.6f, skin, Modifier.fillMaxWidth().height(120.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(skin.displayName, style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.width(6.dp))
                            when {
                                locked -> Icon(Icons.Default.Lock, "Pro", Modifier.height(16.dp))
                                isSelected -> Icon(Icons.Default.CheckCircle, "Selected", Modifier.height(16.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        if (skin.isPro) {
                            Text("Pro", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
