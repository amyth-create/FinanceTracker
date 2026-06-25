package com.personal.financetracker.ui.planned

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.R
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.databinding.ItemPlannedBinding
import com.personal.financetracker.util.Formatters

class PlannedAdapter(
    private val onToggle: (PlannedPayment) -> Unit,
    private val onDelete: (PlannedPayment) -> Unit,
) : ListAdapter<PlannedPayment, PlannedAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemPlannedBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(p: PlannedPayment) {
            val ctx = b.root.context
            b.tvEmoji.text = p.categoryEmoji
            b.tvNote.text = p.note.ifEmpty { p.categoryName }
            b.tvDate.text = ctx.getString(R.string.planned_item_subtitle, p.categoryName, Formatters.formatDate(p.plannedDate))

            val sign = if (p.type == "income") "+" else "−"
            b.tvAmount.text = "$sign${Formatters.formatAmount(p.amount)}"
            b.tvAmount.setTextColor(
                ContextCompat.getColor(ctx, if (p.type == "income") R.color.income else R.color.expense)
            )

            val color = try { Color.parseColor(p.categoryColor) } catch (_: Exception) { Color.GRAY }
            val bg = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
            b.tvEmoji.backgroundTintList = ColorStateList.valueOf(bg)

            b.btnCheck.setImageResource(
                if (p.isDone) R.drawable.ic_check_circle else R.drawable.ic_circle
            )
            b.root.alpha = if (p.isDone) 0.55f else 1f
            b.tvNote.paintFlags =
                if (p.isDone) b.tvNote.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                else b.tvNote.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()

            b.btnCheck.setOnClickListener { onToggle(p) }
            b.btnDelete.setOnClickListener { onDelete(p) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemPlannedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<PlannedPayment>() {
            override fun areItemsTheSame(a: PlannedPayment, b: PlannedPayment) = a.id == b.id
            override fun areContentsTheSame(a: PlannedPayment, b: PlannedPayment) = a == b
        }
    }
}
