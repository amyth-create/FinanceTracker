package com.personal.financetracker.ui.transactions

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.ItemDateHeaderBinding
import com.personal.financetracker.databinding.ItemTransactionBinding
import com.personal.financetracker.util.Formatters

sealed class TxRow {
    data class Header(val label: String, val net: Double) : TxRow()
    data class Item(val tx: Transaction) : TxRow()
}

class TransactionSectionAdapter(
    private val showDelete: Boolean = true,
    private val onDelete: (Transaction) -> Unit,
    private val onClick: (Transaction) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var rows: List<TxRow> = emptyList()

    fun submit(newRows: List<TxRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int =
        if (rows[position] is TxRow.Header) TYPE_HEADER else TYPE_ITEM

    override fun getItemCount(): Int = rows.size

    inner class HeaderVH(val b: ItemDateHeaderBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(h: TxRow.Header) {
            b.tvDate.text = h.label
            val sign = if (h.net >= 0) "+" else "−"
            b.tvTotal.text = "$sign${Formatters.formatAmount(kotlin.math.abs(h.net))}"
            b.tvTotal.setTextColor(
                if (h.net >= 0) Color.parseColor("#2DD4A0") else Color.parseColor("#FF5C7A")
            )
        }
    }

    inner class ItemVH(val b: ItemTransactionBinding) : RecyclerView.ViewHolder(b.root) {
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
                if (tx.type == "income") Color.parseColor("#2DD4A0") else Color.parseColor("#FF5C7A")
            )

            b.btnDelete.visibility = if (showDelete) View.VISIBLE else View.GONE
            b.btnDelete.setOnClickListener { onDelete(tx) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemDateHeaderBinding.inflate(inflater, parent, false))
        } else {
            ItemVH(ItemTransactionBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is TxRow.Header -> (holder as HeaderVH).bind(row)
            is TxRow.Item -> (holder as ItemVH).bind(row.tx)
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1
    }
}
