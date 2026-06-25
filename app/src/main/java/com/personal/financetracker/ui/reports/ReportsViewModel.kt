package com.personal.financetracker.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository
import com.personal.financetracker.util.Formatters
import kotlinx.coroutines.flow.map
import java.util.*

class ReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val allTransactions = repo.getAllTransactions().asLiveData()

    // Last 6 months data for the bar chart
    val last6Months = repo.getAllTransactions().map { txs ->
        val cal = Calendar.getInstance()
        (5 downTo 0).map { monthsBack ->
            cal.time = Date()
            cal.add(Calendar.MONTH, -monthsBack)
            val y = cal.get(Calendar.YEAR)
            val m = cal.get(Calendar.MONTH)
            val start = Formatters.monthStartFor(y, m)
            val end = Formatters.monthEndFor(y, m)
            val inRange = txs.filter { it.date in start..end }
            MonthData(
                label = Formatters.formatMonthYear(start),
                income = inRange.filter { it.type == "income" }.sumOf { it.amount },
                expense = inRange.filter { it.type == "expense" }.sumOf { it.amount }
            )
        }
    }.asLiveData()
}

data class MonthData(val label: String, val income: Double, val expense: Double)
