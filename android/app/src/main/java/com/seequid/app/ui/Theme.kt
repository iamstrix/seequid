package com.seequid.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/*
 * 60-30-10: a neutral ink base, blue reserved for water (glass, overlay, history bars),
 * and the squid's coral as the single accent for actions. Green from the icon's straw means "done".
 */

/** Water, and only water. Matches the launcher icon. */
val Aqua = Color(0xFF2E86F5)
val Cyan = Color(0xFF00D4F0)
/** The squid's blush; the one accent colour. */
val Coral = Color(0xFFFF7470)
/** The icon's straw; success states. */
val Leaf = Color(0xFF7ED957)

private val Ink = Color(0xFF101418)

private val DarkColors = darkColorScheme(
    primary = Coral,
    onPrimary = Color(0xFF2B0B10),
    primaryContainer = Color(0xFF3A1E23),
    onPrimaryContainer = Color(0xFFFFD9DC),
    secondary = Leaf,
    onSecondary = Color(0xFF0E2306),
    tertiary = Aqua,
    onTertiary = Color.White,
    background = Ink,
    onBackground = Color(0xFFE8ECF1),
    surface = Ink,
    onSurface = Color(0xFFE8ECF1),
    surfaceVariant = Color(0xFF2A313B),
    onSurfaceVariant = Color(0xFFA3ADBA),
    outline = Color(0xFF4A535F),
    // Material's defaults for these are tinted; keep every container neutral.
    secondaryContainer = Color(0xFF2A313B),
    onSecondaryContainer = Color(0xFFE8ECF1),
    surfaceContainerLowest = Color(0xFF0B0E11),
    surfaceContainerLow = Color(0xFF161B21),
    surfaceContainer = Color(0xFF1A1F26),
    surfaceContainerHigh = Color(0xFF1A1F26),
    surfaceContainerHighest = Color(0xFF1F252D),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFFE5576A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE3E6),
    onPrimaryContainer = Color(0xFF3A1E23),
    secondary = Color(0xFF3E9A1E),
    onSecondary = Color.White,
    tertiary = Aqua,
    onTertiary = Color.White,
    background = Color(0xFFF6F7F9),
    surface = Color(0xFFF6F7F9),
    surfaceVariant = Color(0xFFE6E9EE),
    onSurfaceVariant = Color(0xFF5A6370),
    secondaryContainer = Color(0xFFE6E9EE),
    onSecondaryContainer = Color(0xFF1A1F26),
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFF0F2F5),
)

@Composable
fun SeequidTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content,
    )
}
