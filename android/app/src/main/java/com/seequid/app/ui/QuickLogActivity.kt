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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                            Text("How much did you drink?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            // Today's progress, right where the decision is made.
                            val state by container.hydration.state.collectAsStateWithLifecycle(initialValue = null)
                            // The real amount that clears the screen (never capped, so it never over-promises),
                            // plus today's progress.
                            val toClear = state?.takeIf { it.level > 0.005f }?.let { roundedSip(it.expectedMl - it.loggedMl) }
                            state?.let {
                                Text(
                                    if (toClear != null) "Drink about ${amount(toClear)} to clear your screen"
                                    else "Your screen is clear",
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${amount(it.loggedMl)} of ${amount(it.goalMl)} today",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("Log it and the water on your screen will go down.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(16.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val sizes = listOf(150, 250, 330, 500)
                                // The smallest drink that clears the screen (or the biggest, if none does) is the suggestion.
                                val suggested = toClear?.let { need -> sizes.firstOrNull { it >= need } ?: sizes.last() }
                                sizes.forEach { ml ->
                                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        JellyButton(
                                            onClick = { log(ml) },
                                            modifier = Modifier.fillMaxWidth(),
                                            glow = ml == suggested,
                                            haptic = true,
                                            contentPadding = PaddingValues(horizontal = 4.dp),
                                        ) { Text("$ml") }
                                        Text(
                                            if (ml == suggested) (if (ml >= toClear!!) "Clears it" else "Best start") else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.padding(top = 6.dp),
                                        )
                                    }
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

    // Hide the overlay while the sheet is up, so the water doesn't tint it.
    override fun onStart() {
        super.onStart()
        container.screenVisible(true)
    }

    override fun onStop() {
        container.screenVisible(false)
        super.onStop()
    }

    private fun log(ml: Int) {
        lifecycleScope.launch {
            container.hydration.log(ml)
            Toast.makeText(this@QuickLogActivity, "+$ml ml", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
