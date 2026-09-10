package com.personal.financetracker.ui.transactions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.ItemTransactionBinding
import com.personal.financetracker.ui.common.color
import com.personal.financetracker.ui.common.tintTile
import com.personal.financetracker.ui.common.visible
import com.personal.financetracker.util.Formatters

/** Binds a transaction into the shared row layout; used by every list in the app. */
object TransactionRow {
    fun bind(b: ItemTransactionBinding, tx: Transaction, showDate: Boolean = true, showDivider: Boolean = true) {
        val ctx = b.root.context
        b.tvEmoji.text = tx.categoryEmoji
        b.tvEmoji.tintTile(tx.categoryColor)
        b.tvTitle.text = tx.note.ifBlank { tx.categoryName }
        b.tvSub.text = if (showDate) "${tx.categoryName} · ${Formatters.formatDateShort(tx.date)}" else tx.categoryName
        val isIncome = tx.type == "income"
        b.tvAmount.text = (if (isIncome) "+" else "−") + Formatters.formatAmount(tx.amount)
        b.tvAmount.setTextColor(ctx.color(if (isIncome) R.color.income else R.color.text_primary))
        b.divider.visible(showDivider)
    }
}

class TransactionAdapter(
    private val showDate: Boolean = true,
    private val onClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tx = getItem(position)
        TransactionRow.bind(holder.b, tx, showDate, showDivider = position < itemCount - 1)
        holder.b.root.setOnClickListener { onClick(tx) }
    }

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Transaction>() {
            override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
            override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
        }
    }
}
