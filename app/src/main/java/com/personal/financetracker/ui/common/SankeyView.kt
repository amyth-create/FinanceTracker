package com.personal.financetracker.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.personal.financetracker.R
import com.personal.financetracker.domain.SankeyData
import com.personal.financetracker.domain.SankeyNode
import com.personal.financetracker.util.Formatters

/**
 * Two-column flow diagram: income sources on the left, spending categories (and "Saved") on the right,
 * joined by ribbons whose thickness is proportional to money. Flows are allocated proportionally
 * (each source feeds each target in proportion to the target's share), which reads naturally
 * for personal cash flow without needing per-transaction links.
 */
class SankeyView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var data: SankeyData? = null
        set(v) { field = v; requestLayout(); reveal() }

    /** 0..1 – ribbons and nodes are revealed left-to-right when data changes. */
    private var progress = 1f
    private var animator: android.animation.ValueAnimator? = null

    private fun reveal() {
        animator?.cancel()
        if (!isAttachedToWindow) { progress = 1f; invalidate(); return }
        animator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 900
            interpolator = Anim.decel
            addUpdateListener { progress = it.animatedValue as Float; invalidate() }
            start()
        }
    }

    private val nodePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ribbonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.color(R.color.text_primary)
        textSize = dp(12f)
        typeface = Typeface.DEFAULT_BOLD
    }
    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.color(R.color.text_secondary)
        textSize = dp(11f)
    }
    private val path = Path()
    private val rect = RectF()

    private val nodeWidth get() = dp(10f)
    private val gap get() = dp(10f)
    private val rowMin get() = dp(30f)
    private val labelWidth get() = dp(104f)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val d = data
        val rows = maxOf(d?.sources?.size ?: 0, d?.targets?.size ?: 0)
        val h = if (rows == 0) dp(80f) else maxOf(dp(200f), rows * rowMin + (rows - 1) * gap + dp(24f))
        setMeasuredDimension(w, h.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val d = data ?: return
        if (d.sources.isEmpty() && d.targets.isEmpty()) return
        val w = width.toFloat(); val h = height.toFloat()
        val top = dp(12f); val usable = h - dp(24f)
        // Reveal sweep: clip everything to the right of the progress edge
        if (progress < 1f) canvas.clipRect(0f, 0f, w * progress, h)

        val leftX = labelWidth
        val rightX = w - labelWidth - nodeWidth

        val leftTotal = d.sources.sumOf { it.amount }.toFloat().coerceAtLeast(0.01f)
        val rightTotal = d.targets.sumOf { it.amount }.toFloat().coerceAtLeast(0.01f)
        val scaleTotal = maxOf(leftTotal, rightTotal)

        val leftRects = layoutColumn(d.sources, leftX, top, usable, scaleTotal)
        val rightRects = layoutColumn(d.targets, rightX, top, usable, scaleTotal)

        // Ribbons: each source feeds every target in proportion to the target's share.
        val availL = usable - gap * (leftRects.size - 1).coerceAtLeast(0)
        val availR = usable - gap * (rightRects.size - 1).coerceAtLeast(0)
        val leftOffsets = FloatArray(leftRects.size)
        val rightOffsets = FloatArray(rightRects.size)
        d.sources.forEachIndexed { i, src ->
            d.targets.forEachIndexed { j, tgt ->
                val flow = (src.amount / leftTotal) * tgt.amount * (minOf(leftTotal, rightTotal) / rightTotal)
                val tL = (flow / scaleTotal * availL).toFloat()
                val tR = (flow / scaleTotal * availR).toFloat()
                if (tL < 0.5f && tR < 0.5f) return@forEachIndexed
                val l = leftRects[i]; val r = rightRects[j]
                val y0 = l.top + leftOffsets[i]; val y1 = r.top + rightOffsets[j]
                leftOffsets[i] += tL; rightOffsets[j] += tR
                drawRibbon(canvas, l.right, y0, tL, r.left, y1, tR, parseColor(tgt.color))
            }
        }

        // Nodes + labels (labels get a minimum vertical step so tiny nodes don't overlap)
        val leftLabelY = labelPositions(leftRects)
        val rightLabelY = labelPositions(rightRects)
        d.sources.forEachIndexed { i, n -> drawNode(canvas, leftRects[i], leftLabelY[i], n, leftAligned = true) }
        d.targets.forEachIndexed { i, n -> drawNode(canvas, rightRects[i], rightLabelY[i], n, leftAligned = false) }
    }

    private fun labelPositions(rects: List<RectF>): List<Float> {
        val step = rowMin
        val out = ArrayList<Float>(rects.size)
        var prev = -Float.MAX_VALUE
        rects.forEach { r ->
            val y = maxOf(r.centerY(), prev + step)
            out.add(y); prev = y
        }
        // Keep the last label inside the view
        val overflow = (out.lastOrNull() ?: 0f) + step / 2 - height
        if (overflow > 0) for (i in out.indices) out[i] -= overflow
        return out
    }

    private fun layoutColumn(nodes: List<SankeyNode>, x: Float, top: Float, usable: Float, scaleTotal: Float): List<RectF> {
        if (nodes.isEmpty()) return emptyList()
        val available = usable - gap * (nodes.size - 1)
        var y = top
        return nodes.map { n ->
            val hh = maxOf((n.amount / scaleTotal * available).toFloat(), dp(4f))
            val r = RectF(x, y, x + nodeWidth, y + hh)
            y += hh + gap
            r
        }
    }

    private fun drawRibbon(c: Canvas, x0: Float, y0: Float, t0: Float, x1: Float, y1: Float, t1: Float, color: Int) {
        val mid = (x0 + x1) / 2
        path.reset()
        path.moveTo(x0, y0)
        path.cubicTo(mid, y0, mid, y1, x1, y1)
        path.lineTo(x1, y1 + t1)
        path.cubicTo(mid, y1 + t1, mid, y0 + t0, x0, y0 + t0)
        path.close()
        ribbonPaint.color = color.withAlpha(80)
        c.drawPath(path, ribbonPaint)
    }

    private fun drawNode(c: Canvas, r: RectF, cy: Float, n: SankeyNode, leftAligned: Boolean) {
        nodePaint.color = parseColor(n.color)
        rect.set(r); c.drawRoundRect(rect, dp(3f), dp(3f), nodePaint)
        val label = "${n.emoji} ${n.label}"
        val amount = Formatters.formatCompact(n.amount)
        val maxW = labelWidth - dp(8f)
        val txt = ellipsize(label, textPaint, maxW)
        if (leftAligned) {
            textPaint.textAlign = Paint.Align.RIGHT; subPaint.textAlign = Paint.Align.RIGHT
            c.drawText(txt, r.left - dp(6f), cy - dp(1f), textPaint)
            c.drawText(amount, r.left - dp(6f), cy + dp(12f), subPaint)
        } else {
            textPaint.textAlign = Paint.Align.LEFT; subPaint.textAlign = Paint.Align.LEFT
            c.drawText(txt, r.right + dp(6f), cy - dp(1f), textPaint)
            c.drawText(amount, r.right + dp(6f), cy + dp(12f), subPaint)
        }
    }

    private fun ellipsize(s: String, p: Paint, maxW: Float): String {
        if (p.measureText(s) <= maxW) return s
        var t = s
        while (t.length > 1 && p.measureText("$t…") > maxW) t = t.dropLast(1)
        return "$t…"
    }
}
