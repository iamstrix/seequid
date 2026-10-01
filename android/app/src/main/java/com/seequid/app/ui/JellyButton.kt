package com.seequid.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/*
 * The app's primary action button: a warm coral "jelly" with light from above (gradient + top highlight),
 * a soft coral glow underneath, a squish when pressed and an optional haptic tick.
 * Dark text on the coral keeps roughly 6-7:1 contrast, so it stays readable in sunlight.
 */

private val JellyTop = Color(0xFFFF8A7A)
private val JellyBottom = Color(0xFFF2546A)
private val JellyGlow = Color(0xFFF2546A)
private val OnJelly = Color(0xFF2B0B10)

@Composable
fun JellyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** The soft glow underneath. Off for secondary placements (e.g. the top bar) so they don't compete. */
    glow: Boolean = true,
    height: Dp = 56.dp,
    contentPadding: PaddingValues = PaddingValues(horizontal = 22.dp),
    /** A light tick on tap; meant for the rewarding actions, like logging a drink. */
    haptic: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.96f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "squish",
    )
    val view = LocalView.current
    Row(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(if (glow) Modifier.shadow(10.dp, CircleShape, ambientColor = JellyGlow, spotColor = JellyGlow) else Modifier)
            .height(height)
            .defaultMinSize(minWidth = 64.dp)
            .clip(CircleShape)
            .background(Brush.verticalGradient(listOf(JellyTop, JellyBottom)))
            // Highlight along the top edge, fading out by the middle: light hitting the top of the jelly.
            .border(1.dp, Brush.verticalGradient(0f to Color.White.copy(alpha = 0.45f), 0.5f to Color.Transparent), CircleShape)
            .drawWithContent {
                drawContent()
                if (pressed) drawRect(Color.Black.copy(alpha = 0.08f))
            }
            .clickable(interaction, ripple(color = Color.White), role = Role.Button) {
                if (haptic) {
                    view.performHapticFeedback(
                        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
                    )
                }
                onClick()
            }
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides OnJelly) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold)) {
                content()
            }
        }
    }
}

private fun icon(name: String, pathData: String) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
    .addPath(PathParser().parsePathString(pathData).toNodes(), fill = androidx.compose.ui.graphics.SolidColor(Color.Black))
    .build()

/** A small tumbler, for the smaller drink. */
val CupIcon: ImageVector = icon("Cup", "M5,4 L19,4 L17.2,20.2 C17.1,21.2 16.3,22 15.3,22 L8.7,22 C7.7,22 6.9,21.2 6.8,20.2 Z M7.2,8 L7.9,15 L16.1,15 L16.8,8 Z")

/** A reusable bottle, for the bigger drink. */
val BottleIcon: ImageVector = icon(
    "Bottle",
    "M9.5,1.5 L14.5,1.5 L14.5,4.5 C16.5,5.3 17.5,7 17.5,9 L17.5,20 C17.5,21.4 16.4,22.5 15,22.5 L9,22.5 " +
        "C7.6,22.5 6.5,21.4 6.5,20 L6.5,9 C6.5,7 7.5,5.3 9.5,4.5 Z M8.5,11 L8.5,17 L15.5,17 L15.5,11 Z",
)
