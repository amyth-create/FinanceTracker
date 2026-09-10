package com.personal.financetracker.ui.transactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.ItemDateHeaderBinding
import com.personal.financetracker.databinding.ItemTransactionBinding
import com.personal.financetracker.ui.common.color
import com.personal.financetracker.util.Formatters

sealed class TxRow {
    data class Header(val day: Long, val date: Long, val net: Double) : TxRow()
    data class Item(val tx: Transaction, val last: Boolean) : TxRow()
}

class TransactionSectionAdapter(
    private val onClick: (Transaction) -> Unit
) : ListAdapter<TxRow, RecyclerView.ViewHolder>(DIFF) {

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is TxRow.Header) TYPE_HEADER else TYPE_ITEM

    fun transactionAt(position: Int): Transaction? = (getItem(position) as? TxRow.Item)?.tx

    inner class HeaderVH(val b: ItemDateHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: TxRow.Header) {
            b.tvDate.text = Formatters.formatRelativeDay(h.date)
            b.tvTotal.text = Formatters.formatSigned(h.net)
            b.tvTotal.setTextColor(b.root.context.color(if (h.net >= 0) R.color.income else R.color.text_secondary))
        }
    }

    inner class ItemVH(val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) HeaderVH(ItemDateHeaderBinding.inflate(inflater, parent, false))
        else ItemVH(ItemTransactionBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is TxRow.Header -> (holder as HeaderVH).bind(row)
            is TxRow.Item -> {
                val b = (holder as ItemVH).b
                TransactionRow.bind(b, row.tx, showDate = false, showDivider = !row.last)
                b.root.setOnClickListener { onClick(row.tx) }
            }
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        val DIFF = object : DiffUtil.ItemCallback<TxRow>() {
            override fun areItemsTheSame(a: TxRow, b: TxRow): Boolean = when {
                a is TxRow.Header && b is TxRow.Header -> a.day == b.day
                a is TxRow.Item && b is TxRow.Item -> a.tx.id == b.tx.id
                else -> false
            }
            override fun areContentsTheSame(a: TxRow, b: TxRow) = a == b
        }
    }
}
