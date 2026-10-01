package com.seequid.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

/** Which plan was just bought; the celebration grows with the commitment. */
enum class ProPlan { MONTHLY, YEARLY, LIFETIME }

/** Maps a purchased product id (e.g. "pro_yearly") to its plan. */
fun proPlanFor(productIds: List<String>): ProPlan {
    val id = productIds.joinToString().lowercase()
    return when {
        "lifetime" in id -> ProPlan.LIFETIME
        "year" in id || "annual" in id -> ProPlan.YEARLY
        else -> ProPlan.MONTHLY
    }
}

private val Gold = Color(0xFFFFD166)
private val GoldDeep = Color(0xFFF4A21B)
private val crown = PathParser().parsePathString(
    "M436,196 L448,126 L486,164 L512,108 L538,164 L576,126 L588,196 Z"
).toPath()

private class Particle(
    val x: Float, val delay: Float, val speed: Float, val drift: Float,
    val size: Float, val spin: Float, val color: Color,
)

/**
 * Full-screen thank-you after a purchase. Monthly: bubbles. Yearly: confetti. Lifetime: a crowned squid in gold.
 * Dismisses itself after a few seconds, or on tap.
 */
@Composable
fun ProCelebration(plan: ProPlan, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val view = LocalView.current
    val particles = remember(plan) {
        val rnd = Random(plan.ordinal + 7)
        val palette = when (plan) {
            ProPlan.MONTHLY -> listOf(Color.White, Aqua, Cyan)
            ProPlan.YEARLY -> listOf(Coral, Aqua, Leaf, Color.White, Gold)
            ProPlan.LIFETIME -> listOf(Gold, GoldDeep, Color.White, Gold)
        }
        val count = when (plan) { ProPlan.MONTHLY -> 28; ProPlan.YEARLY -> 90; ProPlan.LIFETIME -> 140 }
        List(count) {
            Particle(
                x = rnd.nextFloat(), delay = rnd.nextFloat() * 0.35f, speed = 0.6f + rnd.nextFloat() * 0.6f,
                drift = rnd.nextFloat() * 2f - 1f, size = 6f + rnd.nextFloat() * 8f,
                spin = rnd.nextFloat() * 720f - 360f, color = palette[it % palette.size],
            )
        }
    }
    LaunchedEffect(plan) {
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
        launch { progress.animateTo(1f, tween(4000, easing = LinearEasing)) }.join()
        onDone()
    }

    val (title, subtitle) = when (plan) {
        ProPlan.MONTHLY -> "Welcome to Pro!" to "All the drinks and your water history are unlocked."
        ProPlan.YEARLY -> "A whole year of Pro. Thank you!" to "All the drinks and your water history are yours."
        ProPlan.LIFETIME -> "Pro forever. You're a legend!" to "Everything is unlocked, for good. Thank you!"
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDone),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val t = progress.value
            particles.forEach { p ->
                val local = ((t - p.delay) / (1f - p.delay)).coerceIn(0f, 1f)
                if (local <= 0f || local >= 1f) return@forEach
                val fade = if (local > 0.8f) (1f - local) / 0.2f else 1f
                if (plan == ProPlan.MONTHLY) {
                    // Bubbles rise from the bottom and wobble.
                    val y = size.height * (1.05f - local * p.speed * 1.2f)
                    val x = size.width * p.x + sin(local * 12f + p.drift * 3f) * 14.dp.toPx()
                    drawCircle(p.color.copy(alpha = 0.55f * fade), p.size.dp.toPx() * 0.9f, Offset(x, y))
                } else {
                    // Confetti bursts upward from below the squid, then falls with gravity.
                    val launch = size.height * 0.62f
                    val y = launch - (1.6f * p.speed * local - 1.9f * local * local) * size.height * 0.55f
                    val x = size.width * (0.5f + (p.x - 0.5f) * 1.4f * local) + p.drift * 30.dp.toPx() * local
                    rotate(p.spin * local, Offset(x, y)) {
                        drawRect(
                            p.color.copy(alpha = fade),
                            Offset(x - p.size.dp.toPx() / 2, y - p.size.dp.toPx() / 4),
                            Size(p.size.dp.toPx(), p.size.dp.toPx() / 2),
                        )
                    }
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Canvas(Modifier.size(180.dp)) {
                val t = progress.value
                val bounce = -sin((t * 6f).coerceAtMost(3.14f)) * 18.dp.toPx()
                val waterline = Offset(size.width / 2, size.height * 0.78f + bounce)
                drawSquid(waterline, size.width * 0.62f, SquidMood.CELEBRATING, time = t * 3f % 1f)
                if (plan == ProPlan.LIFETIME) drawCrown(waterline, size.width * 0.62f)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (plan == ProPlan.LIFETIME) Gold else Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(subtitle, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
        }
    }
}

/** A gold crown sitting on the squid's head, in the same 1024-unit space as [drawSquid]. */
private fun DrawScope.drawCrown(waterline: Offset, width: Float) {
    val s = width / 424f
    withTransform({
        translate(waterline.x - 512f * s, waterline.y - 640f * s)
        scale(s, s, pivot = Offset.Zero)
    }) {
        drawPath(crown, Gold)
        listOf(448f, 512f, 576f).forEachIndexed { i, x ->
            drawCircle(GoldDeep, 9f, Offset(x, listOf(126f, 108f, 126f)[i]))
        }
    }
}
