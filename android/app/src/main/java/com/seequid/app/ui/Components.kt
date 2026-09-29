package com.seequid.app.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.seequid.app.data.DayTotal
import com.seequid.app.overlay.LiquidSkin
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** A glass that fills as the user drinks — the in-app mirror of the overlay. */
@Composable
fun Glass(fill: Float, skin: LiquidSkin, modifier: Modifier = Modifier) {
    val animatedFill by animateFloatAsState(fill.coerceIn(0f, 1f), tween(700), label = "fill")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val front = Color(skin.front)
    val back = Color(skin.back)
    Canvas(modifier) {
        val corner = CornerRadius(28.dp.toPx())
        val glass = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner))
        }
        clipPath(glass) {
            val surface = size.height * (1f - animatedFill)
            val amp = if (animatedFill in 0.01f..0.99f) 7.dp.toPx() else 0f
            listOf(back to phase + 1.5f, front to -phase).forEach { (color, offset) ->
                val wave = Path().apply {
                    moveTo(0f, size.height)
                    var x = 0f
                    while (x <= size.width) {
                        lineTo(x, surface + amp * sin(x / size.width * 3.2f * PI.toFloat() + offset))
                        x += 6f
                    }
                    lineTo(size.width, surface)
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(wave, color)
            }
        }
        drawRoundRect(outline, Offset.Zero, Size(size.width, size.height), corner, style = Stroke(3.dp.toPx()))
    }
}

@Composable
fun WeekBars(days: List<DayTotal>, goalMl: Int, modifier: Modifier = Modifier) {
    val max = maxOf(goalMl, days.maxOfOrNull { it.totalMl } ?: 0).coerceAtLeast(1)
    val bar = MaterialTheme.colorScheme.primary
    val goalLine = MaterialTheme.colorScheme.secondary
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth().height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            days.forEach { day ->
                Canvas(Modifier.weight(1f).fillMaxHeight()) {
                    val h = size.height * day.totalMl / max
                    drawRoundRect(
                        color = if (day.totalMl >= goalMl) goalLine else bar,
                        topLeft = Offset(0f, size.height - h),
                        size = Size(size.width, h),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                    Modifier.weight(1f).padding(top = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
    }
}
