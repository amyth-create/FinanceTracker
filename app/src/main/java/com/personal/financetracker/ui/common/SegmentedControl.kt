package com.personal.financetracker.ui.common

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.personal.financetracker.R

/** A pill-style segmented control with a sliding selection indicator: [ Spending | Income | Cash flow ]. */
class SegmentedControl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    var onSelected: ((Int) -> Unit)? = null
    var selectedIndex: Int = 0
        private set

    private val indicator = View(context).apply { setBackgroundResource(R.drawable.seg_selected) }
    private val row = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL }
    private val items = ArrayList<TextView>()
    private var laidOut = false

    init {
        setBackgroundResource(R.drawable.seg_container)
        val p = dpInt(4)
        setPadding(p, p, p, p)
        addView(indicator, LayoutParams(0, dpInt(36)))
        addView(row, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        val labels = attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SegmentedControl)
            val s = ta.getString(R.styleable.SegmentedControl_segments)
            ta.recycle()
            s
        }
        labels?.split("|")?.let { setItems(it) }
    }

    fun setItems(labels: List<String>) {
        row.removeAllViews(); items.clear()
        labels.forEachIndexed { i, label ->
            val tv = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, dpInt(36), 1f)
                setOnClickListener { select(i, notify = true) }
            }
            items.add(tv); row.addView(tv)
        }
        select(selectedIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), notify = false)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (items.isNotEmpty()) {
            val w = (right - left - paddingLeft - paddingRight) / items.size
            if (indicator.layoutParams.width != w) {
                indicator.layoutParams = LayoutParams(w, dpInt(36))
                indicator.layout(paddingLeft, paddingTop, paddingLeft + w, paddingTop + dpInt(36))
            }
            moveIndicator(animated = laidOut)
            laidOut = true
        }
    }

    fun select(index: Int, notify: Boolean = false) {
        val changed = index != selectedIndex
        selectedIndex = index
        if (items.isEmpty()) return
        items.forEachIndexed { i, tv ->
            tv.setTextColor(context.color(if (i == index) R.color.text_primary else R.color.text_secondary))
        }
        if (width > 0) moveIndicator(animated = changed && laidOut)
        if (notify) onSelected?.invoke(index)
    }

    private fun moveIndicator(animated: Boolean) {
        val w = indicator.layoutParams.width.takeIf { it > 0 } ?: return
        val target = (selectedIndex * w).toFloat()
        indicator.animate().cancel()
        if (animated) {
            indicator.animate().translationX(target).setDuration(Anim.NORMAL).setInterpolator(Anim.decel).start()
        } else {
            indicator.translationX = target
        }
    }
}
