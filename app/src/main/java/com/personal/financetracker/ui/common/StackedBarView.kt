package com.personal.financetracker.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.personal.financetracker.R

/** A single rounded bar made of coloured segments (category shares). */
class StackedBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    data class Segment(val fraction: Float, val color: Int)

    var segments: List<Segment> = emptyList()
        set(v) { field = v; invalidate() }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rect = RectF()
    private val clip = android.graphics.Path()

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat(); val r = h / 2
        rect.set(0f, 0f, w, h)
        clip.reset(); clip.addRoundRect(rect, r, r, android.graphics.Path.Direction.CW)
        canvas.save(); canvas.clipPath(clip)
        paint.color = context.color(R.color.bg_elevated)
        canvas.drawRect(rect, paint)
        var x = 0f
        val gap = dp(2f)
        segments.forEach { s ->
            val sw = w * s.fraction
            if (sw > 0f) {
                paint.color = s.color
                canvas.drawRect(x, 0f, maxOf(x + sw - gap, x + 1f), h, paint)
                x += sw
            }
        }
        canvas.restore()
    }
}
