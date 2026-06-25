package com.personal.financetracker.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.ItemTransactionBinding
import com.personal.financetracker.util.Formatters

class TransactionAdapter(
    private val showDelete: Boolean = true,
    private val onDelete: (Transaction) -> Unit,
    private val onClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemTransactionBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(tx: Transaction) {
            b.root.setOnClickListener { onClick(tx) }
            b.tvEmoji.text = tx.categoryEmoji
            b.tvNote.text = tx.note.ifEmpty { tx.categoryName }
            b.tvCategory.text = "${tx.categoryName} · ${Formatters.formatDateShort(tx.date)}"

            val sign = if (tx.type == "income") "+" else "−"
            b.tvAmount.text = "$sign${Formatters.formatAmount(tx.amount)}"

            val color = try { Color.parseColor(tx.categoryColor) } catch (e: Exception) { Color.GRAY }
            val bg = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
            b.tvEmoji.backgroundTintList = android.content.res.ColorStateList.valueOf(bg)

            b.tvAmount.setTextColor(
                if (tx.type == "income") Color.parseColor("#2DD4A0")
                else Color.parseColor("#FF5C7A")
            )

            b.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
            b.btnDelete.setOnClickListener { onDelete(tx) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }
    }
}
