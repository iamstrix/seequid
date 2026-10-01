package com.seequid.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** The launcher icon's blue. */
val Aqua = Color(0xFF2E86F5)
val Cyan = Color(0xFF00D4F0)
private val Deep = Color(0xFF071A2E)
private val DeepSurface = Color(0xFF0E2640)

private val DarkColors = darkColorScheme(
    primary = Aqua,
    onPrimary = Color.White,
    secondary = Cyan,
    background = Deep,
    onBackground = Color(0xFFE3F2FF),
    surface = DeepSurface,
    onSurface = Color(0xFFE3F2FF),
    surfaceVariant = Color(0xFF16324F),
    onSurfaceVariant = Color(0xFFA9C6E3),
    // Material's defaults for these are neutral grey/purple; keep every container in the deep-water family.
    secondaryContainer = Color(0xFF16324F),
    onSecondaryContainer = Color(0xFFE3F2FF),
    surfaceContainerLowest = Color(0xFF0A1F36),
    surfaceContainerLow = DeepSurface,
    surfaceContainer = DeepSurface,
    surfaceContainerHigh = Color(0xFF122D4A),
    surfaceContainerHighest = Color(0xFF122D4A),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6FD9),
    onPrimary = Color.White,
    secondary = Color(0xFF0097A7),
    background = Color(0xFFF3F8FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE1ECFA),
    secondaryContainer = Color(0xFFDCEBFF),
    onSecondaryContainer = Color(0xFF0A1F36),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFEAF3FF),
    surfaceContainerHighest = Color(0xFFEAF3FF),
)

@Composable
fun SeequidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
