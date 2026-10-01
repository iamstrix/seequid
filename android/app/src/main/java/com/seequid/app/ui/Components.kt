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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.seequid.app.data.DayTotal
import com.seequid.app.overlay.LiquidSkin
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/** A glass that fills as the user drinks — the in-app mirror of the overlay, with the squid riding the surface. */
@Composable
fun Glass(fill: Float, skin: LiquidSkin, modifier: Modifier = Modifier, showSquid: Boolean = true) {
    // Real glasses aren't filled to the brim: 100% sits at 85% height, over-goal tops out a little higher.
    val animatedFill by animateFloatAsState(fill.coerceIn(0f, 1.08f) * 0.85f, tween(700), label = "fill")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val front = Color(skin.front)
    val back = Color(skin.back)
    val goalHit = fill >= 1f
    Canvas(modifier) {
        val corner = CornerRadius(28.dp.toPx())
        val glass = Path().apply {
            addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, size.width, size.height, corner))
        }
        val surface = size.height * (1f - animatedFill)
        val amp = if (animatedFill > 0.01f) 7.dp.toPx() else 0f
        fun wave(offset: Float) = Path().apply {
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
        clipPath(glass) {
            drawPath(wave(phase + 1.5f), back)
            // Bubbles drift up through the water column.
            val column = size.height - surface
            if (column > 24.dp.toPx()) {
                listOf(0.22f to 0f, 0.7f to 0.35f, 0.45f to 0.7f, 0.85f to 0.5f).forEach { (fx, off) ->
                    val t = ((phase / (2 * PI).toFloat()) + off) % 1f
                    drawCircle(
                        Color.White.copy(alpha = 0.35f * (1f - t)),
                        radius = (3 + 3 * fx).dp.toPx(),
                        center = Offset(size.width * fx, size.height - column * t),
                    )
                }
            }
        }
        // The squid isn't clipped, so when the glass is full it peeks over the rim.
        if (showSquid) {
            val bob = amp * 0.6f * sin(phase)
            drawSquid(Offset(size.width / 2, surface + bob), size.width * 0.42f, goalHit)
        }
        clipPath(glass) { drawPath(wave(-phase), front.copy(alpha = 0.88f)) }
        drawRoundRect(outline, Offset.Zero, Size(size.width, size.height), corner, style = Stroke(3.dp.toPx()))
    }
}

private val squidInk = Color(0xFF0A1A3A)
private val squidMantle = PathParser().parsePathString(
    "M512,200 C610,250 668,350 668,470 L668,600 Q512,650 356,600 L356,470 C356,350 414,250 512,200 Z"
).toPath()
private val squidFins = PathParser().parsePathString(
    "M410,330 Q320,310 300,370 Q350,405 400,400 Z M614,330 Q704,310 724,370 Q674,405 624,400 Z"
).toPath()
private val squidTentacles = PathParser().parsePathString(
    "M405,600 C405,680 397,728 362,754 C336,773 341,802 370,797 " +
        "M478,615 C478,690 458,730 468,800 M546,615 C546,690 566,730 556,800 " +
        "M619,600 C619,680 627,728 662,754 C688,773 683,802 654,797"
).toPath()
private val squidHappyEyes = PathParser().parsePathString("M430,490 Q462,445 494,490 M530,490 Q562,445 594,490").toPath()
private val squidSmile = PathParser().parsePathString("M482,545 Q512,572 542,545").toPath()

/**
 * The seequid mascot, drawn from the launcher icon's geometry (a 1024-unit canvas).
 * [waterline] is where the water surface crosses it — just under the smile — and [width] is the mantle-and-fins width.
 */
fun DrawScope.drawSquid(waterline: Offset, width: Float, happy: Boolean) {
    val s = width / 424f
    withTransform({
        translate(waterline.x - 512f * s, waterline.y - 610f * s)
        scale(s, s, pivot = Offset.Zero)
    }) {
        drawPath(squidTentacles, Color.White, style = Stroke(40f, cap = StrokeCap.Round))
        drawPath(squidFins, Color.White)
        drawPath(squidMantle, Color.White)
        if (happy) {
            drawPath(squidHappyEyes, squidInk, style = Stroke(18f, cap = StrokeCap.Round))
        } else {
            listOf(462f, 562f).forEach { x ->
                drawCircle(squidInk, 34f, Offset(x, 480f))
                drawCircle(Color.White, 11f, Offset(x + 12f, 468f))
                drawCircle(Color.White.copy(alpha = 0.8f), 5f, Offset(x - 10f, 493f))
            }
        }
        drawOval(Coral.copy(alpha = 0.7f), Offset(390f, 518f), Size(48f, 24f))
        drawOval(Coral.copy(alpha = 0.7f), Offset(586f, 518f), Size(48f, 24f))
        drawPath(squidSmile, squidInk, style = Stroke(16f, cap = StrokeCap.Round))
    }
}

@Composable
fun WeekBars(days: List<DayTotal>, goalMl: Int, modifier: Modifier = Modifier) {
    val max = maxOf(goalMl, days.maxOfOrNull { it.totalMl } ?: 0).coerceAtLeast(1)
    val bar = Aqua
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
