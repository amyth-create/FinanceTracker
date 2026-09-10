package com.personal.financetracker.ui.planned

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.content.res.ColorStateList
import com.personal.financetracker.R
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.databinding.ItemDateHeaderBinding
import com.personal.financetracker.databinding.ItemPlannedBinding
import com.personal.financetracker.ui.common.color
import com.personal.financetracker.ui.common.tintTile
import com.personal.financetracker.ui.common.visible
import com.personal.financetracker.util.Formatters
import java.util.concurrent.TimeUnit

sealed class PlannedRow {
    data class Header(val title: String, val count: Int) : PlannedRow()
    data class Item(val p: PlannedPayment, val last: Boolean) : PlannedRow()
}

class PlannedAdapter(
    private val onToggle: (PlannedPayment) -> Unit,
) : ListAdapter<PlannedRow, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int) = if (getItem(position) is PlannedRow.Header) TYPE_HEADER else TYPE_ITEM

    fun paymentAt(position: Int): PlannedPayment? = (getItem(position) as? PlannedRow.Item)?.p

    inner class HeaderVH(val b: ItemDateHeaderBinding) : RecyclerView.ViewHolder(b.root)
    inner class ItemVH(val b: ItemPlannedBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderVH(ItemDateHeaderBinding.inflate(inf, parent, false))
        else ItemVH(ItemPlannedBinding.inflate(inf, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is PlannedRow.Header -> (holder as HeaderVH).b.apply {
                tvDate.text = row.title; tvTotal.text = row.count.toString()
            }
            is PlannedRow.Item -> bind((holder as ItemVH).b, row)
        }
    }

    private fun bind(b: ItemPlannedBinding, row: PlannedRow.Item) {
        val p = row.p
        val ctx = b.root.context
        b.tvEmoji.text = p.categoryEmoji; b.tvEmoji.tintTile(p.categoryColor)
        b.tvTitle.text = p.note.ifBlank { p.categoryName }
        val today = Formatters.startOfDay(System.currentTimeMillis())
        val days = TimeUnit.MILLISECONDS.toDays(Formatters.startOfDay(p.plannedDate) - today).toInt()
        val rel = when {
            p.isDone -> "Paid"
            days < 0 -> ctx.getString(R.string.overdue)
            days == 0 -> ctx.getString(R.string.due_today)
            days == 1 -> ctx.getString(R.string.tomorrow)
            else -> ctx.getString(R.string.in_days, days)
        }
        b.tvSub.text = "$rel · ${Formatters.formatDate(p.plannedDate)} · ${p.categoryName}"
        b.tvSub.setTextColor(ctx.color(if (!p.isDone && days < 0) R.color.expense else R.color.text_muted))
        val isIncome = p.type == "income"
        b.tvAmount.text = (if (isIncome) "+" else "−") + Formatters.formatAmount(p.amount)
        b.tvAmount.setTextColor(ctx.color(if (isIncome) R.color.income else R.color.text_primary))

        b.btnCheck.setImageResource(if (p.isDone) R.drawable.ic_check_circle else R.drawable.ic_circle)
        ImageViewCompat.setImageTintList(b.btnCheck, ColorStateList.valueOf(ctx.color(if (p.isDone) R.color.accent else R.color.text_muted)))
        b.root.alpha = if (p.isDone) 0.55f else 1f
        b.tvTitle.paintFlags = if (p.isDone) b.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else b.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        b.divider.visible(!row.last)
        b.btnCheck.setOnClickListener { onToggle(p) }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        val DIFF = object : DiffUtil.ItemCallback<PlannedRow>() {
            override fun areItemsTheSame(a: PlannedRow, b: PlannedRow) = when {
                a is PlannedRow.Header && b is PlannedRow.Header -> a.title == b.title
                a is PlannedRow.Item && b is PlannedRow.Item -> a.p.id == b.p.id
                else -> false
            }
            override fun areContentsTheSame(a: PlannedRow, b: PlannedRow) = a == b
        }
    }
}
