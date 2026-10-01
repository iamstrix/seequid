package com.seequid.app.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import com.seequid.app.domain.HydrationState
import kotlin.math.PI
import kotlin.math.sin

/** Something for the squid to wear. Everything except [NONE] is Pro. */
enum class SquidOutfit(val displayName: String) {
    NONE("No outfit"), CROWN("Crown"), SUNGLASSES("Sunglasses"), PARTY_HAT("Party hat"), HEADBAND("Headband");

    val isPro: Boolean get() = this != NONE

    companion object {
        fun fromName(name: String?): SquidOutfit = entries.firstOrNull { it.name == name } ?: NONE
    }
}

/** How the squid feels, so the user can read their status at a glance. */
enum class SquidMood { HAPPY, WORRIED, THIRSTY, SLEEPING, CELEBRATING }

/** From this share of the goal on, the squid cheers you on even if you're a little behind the pace. */
const val ALMOST_THERE = 0.8f

fun squidMood(state: HydrationState?, loggedMl: Int, goalMl: Int): SquidMood {
    val progress = loggedMl.toFloat() / goalMl.coerceAtLeast(1)
    return when {
        loggedMl >= goalMl -> SquidMood.CELEBRATING
        state == null -> SquidMood.HAPPY
        state.quiet -> SquidMood.SLEEPING
        state.level < 0.05f -> SquidMood.HAPPY
        // Being close to the goal matters more than a small slip in pace.
        progress >= ALMOST_THERE && state.level < 0.6f -> SquidMood.HAPPY
        state.level < 0.5f -> SquidMood.WORRIED
        else -> SquidMood.THIRSTY
    }
}

/** Where the water surface crosses the squid (below the mouth, so wave crests don't cover it). */
private const val WATERLINE_Y = 640f
/** Waterline to tentacle tips (incl. stroke), in the squid's 1024-unit space. */
const val SQUID_DEPTH = 822f - WATERLINE_Y

private val ink = Color(0xFF0A1A3A)
private fun path(d: String) = PathParser().parsePathString(d).toPath()

private val mantle = path("M512,200 C610,250 668,350 668,470 L668,600 Q512,650 356,600 L356,470 C356,350 414,250 512,200 Z")
private val fins = path("M410,330 Q320,310 300,370 Q350,405 400,400 Z M614,330 Q704,310 724,370 Q674,405 624,400 Z")
private val tentacles = path(
    "M405,600 C405,680 397,728 362,754 C336,773 341,802 370,797 " +
        "M478,615 C478,690 458,730 468,800 M546,615 C546,690 566,730 556,800 " +
        "M619,600 C619,680 627,728 662,754 C688,773 683,802 654,797"
)
private val happyEyes = path("M430,490 Q462,445 494,490 M530,490 Q562,445 594,490")
private val sleepyEyes = path("M430,478 Q462,502 494,478 M530,478 Q562,502 594,478")
private val smile = path("M482,545 Q512,572 542,545")
private val bigSmile = path("M476,538 Q512,598 548,538 Z")
/** The smile, flipped. */
private val frown = path("M486,562 Q512,540 538,562")
/** Tired, half-shut eyes: one flat stroke each. */
private val tiredEyes = path("M434,484 L490,484 M534,484 L590,484")
private val restMouth = path("M500,556 Q512,563 524,556")
private val zee = path("M0,0 L26,0 L0,28 L26,28")

/**
 * The Seequid mascot, drawn from the launcher icon's geometry (a 1024-unit canvas).
 * [waterline] is where the water surface crosses it (just under the mouth), [width] is the mantle-and-fins width,
 * [tilt] rotates it around the waterline (for the tap wiggle) and [time] (0..1, looping) animates extras.
 */
fun DrawScope.drawSquid(
    waterline: Offset,
    width: Float,
    mood: SquidMood,
    tilt: Float = 0f,
    time: Float = 0f,
    outfit: SquidOutfit = SquidOutfit.NONE,
) {
    val s = width / 424f
    rotate(tilt, pivot = waterline) {
        withTransform({
            translate(waterline.x - 512f * s, waterline.y - WATERLINE_Y * s)
            scale(s, s, pivot = Offset.Zero)
        }) {
            drawPath(tentacles, Color.White, style = Stroke(40f, cap = StrokeCap.Round))
            drawPath(fins, Color.White)
            drawPath(mantle, Color.White)
            drawEyes(mood)
            drawOval(Coral.copy(alpha = 0.7f), Offset(390f, 518f), Size(48f, 24f))
            drawOval(Coral.copy(alpha = 0.7f), Offset(586f, 518f), Size(48f, 24f))
            drawMouth(mood)
            drawOutfit(outfit)
            drawExtras(mood, time)
        }
    }
}

private fun DrawScope.drawEyes(mood: SquidMood) {
    when (mood) {
        SquidMood.CELEBRATING -> drawPath(happyEyes, ink, style = Stroke(18f, cap = StrokeCap.Round))
        SquidMood.SLEEPING -> drawPath(sleepyEyes, ink, style = Stroke(16f, cap = StrokeCap.Round))
        SquidMood.THIRSTY -> drawPath(tiredEyes, ink, style = Stroke(16f, cap = StrokeCap.Round))
        SquidMood.HAPPY, SquidMood.WORRIED -> {
            sparklyEye(462f, 480f, 34f)
            sparklyEye(562f, 480f, 34f)
        }
    }
}

private fun DrawScope.sparklyEye(x: Float, y: Float, r: Float) {
    drawCircle(ink, r, Offset(x, y))
    drawCircle(Color.White, r * 0.32f, Offset(x + r * 0.35f, y - r * 0.35f))
    drawCircle(Color.White.copy(alpha = 0.8f), r * 0.15f, Offset(x - r * 0.3f, y + r * 0.38f))
}

private fun DrawScope.drawMouth(mood: SquidMood) {
    when (mood) {
        SquidMood.HAPPY -> drawPath(smile, ink, style = Stroke(16f, cap = StrokeCap.Round))
        SquidMood.CELEBRATING -> drawPath(bigSmile, ink)
        SquidMood.WORRIED -> drawPath(frown, ink, style = Stroke(16f, cap = StrokeCap.Round))
        SquidMood.SLEEPING -> drawPath(restMouth, ink, style = Stroke(12f, cap = StrokeCap.Round))
        SquidMood.THIRSTY -> drawCircle(ink, 16f, Offset(512f, 556f))
    }
}

private fun DrawScope.drawExtras(mood: SquidMood, time: Float) {
    when (mood) {
        SquidMood.SLEEPING -> listOf(0f, 0.5f).forEach { offset ->
            // Two z's drifting up and fading out.
            val t = (time + offset) % 1f
            val zs = 1f - 0.4f * offset
            withTransform({
                translate(650f + 40f * t, 330f - 150f * t)
                scale(zs, zs, pivot = Offset.Zero)
            }) {
                drawPath(zee, Color.White.copy(alpha = 1f - t), style = Stroke(9f, cap = StrokeCap.Round))
            }
        }
        SquidMood.CELEBRATING -> listOf(Offset(290f, 250f), Offset(740f, 290f), Offset(700f, 150f)).forEachIndexed { i, at ->
            val twinkle = 0.6f + 0.4f * sin((time + i / 3f) * 2 * PI.toFloat())
            sparkle(at, 34f * twinkle)
        }
        SquidMood.HAPPY, SquidMood.WORRIED, SquidMood.THIRSTY -> Unit
    }
}

private val gold = Color(0xFFFFD166)
private val goldDeep = Color(0xFFF4A21B)
private val crownPath = path("M436,196 L448,126 L486,164 L512,108 L538,164 L576,126 L588,196 Z")
private val partyHat = path("M512,70 L566,212 L458,212 Z")
private val hatStripes = path("M492,122 L532,122 M478,160 L546,160 M466,196 L558,196")
private val shadesBridge = path("M506,476 Q512,468 518,476")

private fun DrawScope.drawOutfit(outfit: SquidOutfit) {
    when (outfit) {
        SquidOutfit.NONE -> Unit
        SquidOutfit.CROWN -> {
            drawPath(crownPath, gold)
            listOf(448f to 126f, 512f to 108f, 576f to 126f).forEach { (x, y) -> drawCircle(goldDeep, 9f, Offset(x, y)) }
        }
        SquidOutfit.SUNGLASSES -> {
            listOf(418f, 518f).forEach { x ->
                drawRoundRect(ink, Offset(x, 452f), Size(88f, 56f), androidx.compose.ui.geometry.CornerRadius(22f))
                drawLine(Color.White.copy(alpha = 0.5f), Offset(x + 14f, 466f), Offset(x + 34f, 466f), 7f, StrokeCap.Round)
            }
            drawPath(shadesBridge, ink, style = Stroke(10f, cap = StrokeCap.Round))
        }
        SquidOutfit.PARTY_HAT -> {
            drawPath(partyHat, Coral)
            drawPath(hatStripes, Color.White.copy(alpha = 0.85f), style = Stroke(9f, cap = StrokeCap.Round))
            drawCircle(gold, 20f, Offset(512f, 70f))
        }
        SquidOutfit.HEADBAND -> {
            // A sweatband across the forehead, clipped to the head's outline.
            clipPath(mantle) {
                drawRect(Coral, Offset(340f, 336f), Size(344f, 46f))
                drawRect(Color.White.copy(alpha = 0.6f), Offset(340f, 352f), Size(344f, 8f))
            }
        }
    }
}

/** A four-point star. */
private fun DrawScope.sparkle(center: Offset, r: Float) {
    val star = androidx.compose.ui.graphics.Path().apply {
        moveTo(center.x, center.y - r)
        quadraticTo(center.x, center.y, center.x + r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + r)
        quadraticTo(center.x, center.y, center.x - r, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - r)
        close()
    }
    drawPath(star, Color(0xFFFFE08A))
}
