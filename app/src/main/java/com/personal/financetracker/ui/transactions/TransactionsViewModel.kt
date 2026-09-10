package com.personal.financetracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import com.personal.financetracker.domain.Period.Companion.toEpochDay

class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val filter = MutableStateFlow("all")
    val search = MutableStateFlow("")

    data class Listing(val rows: List<TxRow>, val count: Int, val total: Int)

    val listing = combine(repo.getAllTransactions(), filter, search) { txs, f, q ->
        val filtered = txs.asSequence()
            .filter { f == "all" || it.type == f }
            .filter { q.isBlank() || it.note.contains(q, true) || it.categoryName.contains(q, true) }
            .toList()
        Listing(buildRows(filtered), filtered.size, txs.size)
    }.flowOn(Dispatchers.Default).asLiveData()

    private fun buildRows(txs: List<Transaction>): List<TxRow> {
        val rows = ArrayList<TxRow>(txs.size + 32)
        txs.sortedByDescending { it.date }
            .groupBy { it.date.toEpochDay() }
            .forEach { (day, items) ->
                val net = items.sumOf { if (it.type == "income") it.amount else -it.amount }
                rows.add(TxRow.Header(day, items.first().date, net))
                items.forEachIndexed { i, tx -> rows.add(TxRow.Item(tx, last = i == items.lastIndex)) }
            }
        return rows
    }

    fun delete(tx: Transaction) = viewModelScope.launch { repo.deleteTransaction(tx) }
    fun restore(tx: Transaction) = viewModelScope.launch { repo.insertTransaction(tx) }
}
