package com.seequid.app.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.seequid.app.container
import kotlinx.coroutines.launch

/** Opened from the overlay's drop handle: one tap to log, then straight back to what you were doing. */
class QuickLogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SeequidTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(remember { MutableInteractionSource() }, indication = null) { finish() },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .navigationBarsPadding()
                            .clickable(remember { MutableInteractionSource() }, indication = null) { },
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Log a drink", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Watch the tide go out.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(150, 250, 330, 500).forEach { ml ->
                                    Button(onClick = { log(ml) }, modifier = Modifier.weight(1f)) { Text("$ml") }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            TextButton(onClick = { finish() }, modifier = Modifier.align(Alignment.End)) { Text("Not now") }
                        }
                    }
                }
            }
        }
    }

    private fun log(ml: Int) {
        lifecycleScope.launch {
            container.hydration.log(ml)
            Toast.makeText(this@QuickLogActivity, "+$ml ml", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
