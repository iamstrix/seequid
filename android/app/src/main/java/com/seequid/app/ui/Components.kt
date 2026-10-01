package com.seequid.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import com.seequid.app.data.DayTotal
import com.seequid.app.overlay.LiquidSkin
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

/**
 * A glass that fills as the user drinks, with the squid riding the surface.
 * With [framed] = false it becomes edge-to-edge water with no glass (the "full-width" home style).
 */
@Composable
fun Glass(
    fill: Float,
    skin: LiquidSkin,
    modifier: Modifier = Modifier,
    showSquid: Boolean = true,
    mood: SquidMood = if (fill >= 1f) SquidMood.CELEBRATING else SquidMood.HAPPY,
    /** Logged ml (or any counter): when it goes up, the squid hops and the water splashes. */
    drinkTrigger: Int = 0,
    framed: Boolean = true,
    onSquidTap: (() -> Unit)? = null,
) {
    // Real glasses aren't filled to the brim: 100% sits at 85% height, over-goal tops out a little higher.
    // Full width keeps more sky above the water: its layer clips, and the squid's head and hop need the room.
    val brim = if (framed) 0.85f else 0.6f
    val animatedFill by animateFloatAsState(fill.coerceIn(0f, 1.08f) * brim, tween(700), label = "fill")
    val phase by rememberInfiniteTransition(label = "wave").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    // 0..1 progress of the last splash; 1 means idle.
    val splash = remember { Animatable(1f) }
    var lastTrigger by remember { mutableIntStateOf(drinkTrigger) }
    LaunchedEffect(drinkTrigger) {
        if (drinkTrigger > lastTrigger) {
            splash.snapTo(0f)
            splash.animateTo(1f, tween(900, easing = LinearOutSlowInEasing))
        }
        lastTrigger = drinkTrigger
    }
    val wiggle = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val onTap by rememberUpdatedState(onSquidTap)
    val tappable = if (onSquidTap == null) Modifier else Modifier.pointerInput(Unit) {
        detectTapGestures {
            scope.launch { wiggle.snapTo(0f); wiggle.animateTo(1f, tween(700)) }
            onTap?.invoke()
        }
    }

    val outline = MaterialTheme.colorScheme.onSurfaceVariant
    val front = Color(skin.front)
    val back = Color(skin.back)
    // Full width draws offscreen so the bottom of the water can be faded out with a mask.
    val layer = if (framed) Modifier else Modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    Canvas(modifier.then(layer).then(tappable)) {
        val corner = CornerRadius(if (framed) 28.dp.toPx() else 0f)
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
            val time = phase / (2 * PI).toFloat()
            val squidWidth = minOf(size.width * 0.42f, 76.dp.toPx())
            val p = splash.value
            val hop = if (p < 1f) -sin(p * PI.toFloat()) * 22.dp.toPx() else 0f
            val bob = amp * 0.6f * sin(phase)
            // In shallow water the squid rests on the bottom instead of hanging out of the glass.
            val floor = size.height - SQUID_DEPTH * squidWidth / 424f - 4.dp.toPx()
            val y = minOf(surface + bob, floor) + hop
            // Full width: the squid drifts back and forth instead of staying centred.
            val x = if (framed) size.width / 2 else size.width / 2 + sin(phase / 2) * size.width * 0.25f
            val w = wiggle.value
            val tilt = if (w < 1f) sin(w * 4 * PI.toFloat()) * 14f * (1f - w) else 0f
            drawSquid(Offset(x, y), squidWidth, mood, tilt, time)
            if (p < 1f) {
                // Splash: a ring spreading on the surface and a few bubbles bursting upward.
                val ring = squidWidth * (0.5f + 1.2f * p)
                drawOval(
                    Color.White.copy(alpha = 0.6f * (1f - p)),
                    Offset(x - ring, surface - ring * 0.18f),
                    Size(ring * 2, ring * 0.36f),
                    style = Stroke(3.dp.toPx()),
                )
                listOf(-0.6f, -0.25f, 0.2f, 0.55f).forEachIndexed { i, dx ->
                    drawCircle(
                        Color.White.copy(alpha = 0.8f * (1f - p)),
                        radius = (3 + i).dp.toPx(),
                        center = Offset(x + dx * squidWidth, surface - p * (30 + 12 * i).dp.toPx()),
                    )
                }
            }
        }
        clipPath(glass) { drawPath(wave(-phase), front.copy(alpha = 0.88f)) }
        if (framed) {
            drawRoundRect(outline, Offset.Zero, Size(size.width, size.height), corner, style = Stroke(3.dp.toPx()))
        } else {
            // Fade the bottom of the water down to the strength of the glow drawn below it.
            drawRect(
                Brush.verticalGradient(0.85f to Color.Black, 1f to Color.Black.copy(alpha = 0.45f)),
                blendMode = BlendMode.DstIn,
            )
        }
    }
}

/** 1750 -> "1.75 L", 1600 -> "1.6 L", 250 -> "0.25 L". */
fun liters(ml: Int): String =
    String.format(Locale.US, "%.2f", ml / 1000f).trimEnd('0').trimEnd('.') + " L"

/** 7 -> "7:00 AM", 23 -> "11:00 PM", 24 -> "12:00 AM". */
fun clock(hour: Int): String =
    LocalTime.of(hour % 24, 0).format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

/** Sliders that set an amount of water are blue; coral is reserved for actions. */
@Composable
fun waterSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = Aqua, activeTrackColor = Aqua,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
)

/** Plain controls (times, thresholds): neutral, so the screen keeps one accent. */
@Composable
fun neutralSliderColors(): SliderColors = SliderDefaults.colors(
    thumbColor = MaterialTheme.colorScheme.onSurface,
    activeTrackColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
    activeTickColor = MaterialTheme.colorScheme.surfaceVariant,
    inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

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
