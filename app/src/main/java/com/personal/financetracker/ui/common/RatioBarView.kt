package com.personal.financetracker.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.personal.financetracker.R

/** A rounded horizontal bar filled to [fraction] (0..1). Optionally shows a second ghost fraction. */
class RatioBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    /** Sets the fill immediately (no animation). */
    var fraction: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); animator?.cancel(); invalidate() }
    var ghostFraction: Float = 0f
        set(v) { field = v.coerceIn(0f, 1f); invalidate() }
    var barColor: Int = context.color(R.color.accent)
        set(v) { field = v; invalidate() }
    var trackColor: Int = context.color(R.color.bg_elevated)
        set(v) { field = v; invalidate() }

    private var animator: ValueAnimator? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()

    /** Animates the fill from its current value to [target]. */
    fun animateTo(target: Float, delay: Long = 0, duration: Long = Anim.SLOW) {
        animator?.cancel()
        val from = fraction
        val to = target.coerceIn(0f, 1f)
        if (!isAttachedToWindow || from == to) { fraction = to; return }
        animator = ValueAnimator.ofFloat(from, to).apply {
            this.duration = duration
            startDelay = delay
            interpolator = Anim.decel
            addUpdateListener { fractionInternal = it.animatedValue as Float }
            start()
        }
    }

    private var fractionInternal: Float
        get() = fraction
        set(v) { val a = animator; animator = null; fraction = v; animator = a }

    override fun onDraw(canvas: Canvas) {
        val h = height.toFloat(); val w = width.toFloat(); val r = h / 2
        paint.color = trackColor
        rect.set(0f, 0f, w, h); canvas.drawRoundRect(rect, r, r, paint)
        if (ghostFraction > 0f) {
            paint.color = barColor.withAlpha(70)
            rect.set(0f, 0f, maxOf(w * ghostFraction, h), h); canvas.drawRoundRect(rect, r, r, paint)
        }
        if (fraction > 0f) {
            paint.color = barColor
            rect.set(0f, 0f, maxOf(w * fraction, h), h); canvas.drawRoundRect(rect, r, r, paint)
        }
    }
}
