package com.personal.financetracker.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.personal.financetracker.databinding.ViewPeriodPickerBinding
import com.personal.financetracker.domain.Period

/** ‹  September 2026  › */
class PeriodPickerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private val b = ViewPeriodPickerBinding.inflate(LayoutInflater.from(context), this)

    var onPrevious: (() -> Unit)? = null
    var onNext: (() -> Unit)? = null
    var onLabelClick: (() -> Unit)? = null

    init {
        orientation = HORIZONTAL
        b.btnPrev.setOnClickListener { onPrevious?.invoke() }
        b.btnNext.setOnClickListener { onNext?.invoke() }
        b.tvLabel.setOnClickListener { onLabelClick?.invoke() }
    }

    private var last: Period? = null

    fun bind(period: Period) {
        val prev = last
        last = period
        if (prev != null && prev != period && isAttachedToWindow) {
            // Slide the label in from the side we're moving towards
            val dir = if (period.start > prev.start) 1f else -1f
            b.tvLabel.animate().cancel()
            b.tvLabel.translationX = dir * dp(22f); b.tvLabel.alpha = 0f
            b.tvLabel.animate().translationX(0f).alpha(1f).setDuration(Anim.NORMAL).setInterpolator(Anim.decel).start()
        }
        b.tvLabel.text = period.label
        b.tvSub.text = if (period.isCurrent()) "Current ${period.noun}" else ""
        b.tvSub.visible(period.isCurrent())
        b.btnNext.alpha = if (period.next().start > System.currentTimeMillis()) 0.35f else 1f
    }
}
