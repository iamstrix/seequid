package com.seequid.app.overlay

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.os.SystemClock
import android.view.View
import kotlin.math.PI
import kotlin.math.sin

/**
 * Two offset sine waves filling the whole view. The overlay window is sized to
 * the water height, so this view only ever draws water — nothing above it is
 * composited or redrawn.
 */
class WaterView(context: Context) : View(context) {

    /** Space above the resting surface that the wave crests can reach. */
    val crestPaddingPx: Int = (14 * resources.displayMetrics.density).toInt()

    private val frontPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val frontPath = Path()
    private val backPath = Path()

    private var phase = 0f
    private var lastFrameAt = 0L

    /** Runs only while the screen is on and water is showing. */
    var animating: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) scheduleFrame()
        }

    init {
        // Decorative: screen readers should never land on the water.
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setSkin(LiquidSkin.WATER)
    }

    fun setSkin(skin: LiquidSkin) {
        if (frontPaint.color == skin.front && backPaint.color == skin.back) return
        frontPaint.color = skin.front
        backPaint.color = skin.back
        invalidate()
    }

    private fun scheduleFrame() {
        // Respect the system "remove animations" setting: draw still water.
        if (!ValueAnimator.areAnimatorsEnabled()) {
            invalidate()
            return
        }
        postOnAnimationDelayed(frame, FRAME_MS)
    }

    private val frame = Runnable {
        if (!animating || !isAttachedToWindow) return@Runnable
        val now = SystemClock.uptimeMillis()
        val dt = if (lastFrameAt == 0L) FRAME_MS else (now - lastFrameAt).coerceAtMost(200)
        lastFrameAt = now
        phase = (phase + dt * RADIANS_PER_MS) % (2 * PI).toFloat()
        invalidate()
        scheduleFrame()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return
        val amplitude = crestPaddingPx * 0.6f
        val surface = crestPaddingPx.toFloat()
        val k = 1.6f * PI.toFloat() / w
        drawWave(canvas, backPath, backPaint, w, h, surface, amplitude, k, phase + PI.toFloat() / 2)
        drawWave(canvas, frontPath, frontPaint, w, h, surface, amplitude, k, -phase)
    }

    private fun drawWave(
        canvas: Canvas, path: Path, paint: Paint,
        w: Float, h: Float, surface: Float, amplitude: Float, k: Float, offset: Float,
    ) {
        path.reset()
        path.moveTo(0f, h)
        var x = 0f
        while (x <= w) {
            path.lineTo(x, surface + amplitude * sin(k * x + offset))
            x += STEP_PX
        }
        path.lineTo(w, surface + amplitude * sin(k * w + offset))
        path.lineTo(w, h)
        path.close()
        canvas.drawPath(path, paint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(frame)
    }

    private companion object {
        // ~24 fps: the water only needs to look alive, not smooth.
        const val FRAME_MS = 42L
        const val RADIANS_PER_MS = (2 * PI / 3_500).toFloat()
        const val STEP_PX = 12f
    }
}
