package com.personal.financetracker.ui.common

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.personal.financetracker.R

/** A pill-style segmented control: [ Spending | Income | Cash flow ]. */
class SegmentedControl @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    var onSelected: ((Int) -> Unit)? = null
    var selectedIndex: Int = 0
        private set

    private val items = ArrayList<TextView>()

    init {
        orientation = HORIZONTAL
        setBackgroundResource(R.drawable.seg_container)
        val p = dpInt(4)
        setPadding(p, p, p, p)
        val labels = attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.SegmentedControl)
            val s = ta.getString(R.styleable.SegmentedControl_segments)
            ta.recycle()
            s
        }
        labels?.split("|")?.let { setItems(it) }
    }

    fun setItems(labels: List<String>) {
        removeAllViews(); items.clear()
        labels.forEachIndexed { i, label ->
            val tv = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                layoutParams = LayoutParams(0, dpInt(36), 1f)
                setOnClickListener { select(i, notify = true) }
            }
            items.add(tv); addView(tv)
        }
        select(selectedIndex.coerceIn(0, (labels.size - 1).coerceAtLeast(0)), notify = false)
    }

    fun select(index: Int, notify: Boolean = false) {
        selectedIndex = index
        if (items.isEmpty()) return
        items.forEachIndexed { i, tv ->
            if (i == index) {
                tv.setBackgroundResource(R.drawable.seg_selected)
                tv.setTextColor(context.color(R.color.text_primary))
            } else {
                tv.background = null
                tv.setTextColor(context.color(R.color.text_secondary))
            }
        }
        if (notify) onSelected?.invoke(index)
    }
}
