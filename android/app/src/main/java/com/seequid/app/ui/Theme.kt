package com.seequid.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Aqua = Color(0xFF3D9BFF)
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
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6FD9),
    onPrimary = Color.White,
    secondary = Color(0xFF0097A7),
    background = Color(0xFFF3F8FF),
    surface = Color.White,
    surfaceVariant = Color(0xFFE1ECFA),
)

@Composable
fun SeequidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
