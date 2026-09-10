package com.personal.financetracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

data class DashboardData(
    val period: Period,
    val balance: Double,
    val summary: PeriodSummary,
    val previous: PeriodSummary,
    val topCategories: List<CategoryStat>,
    val upcoming: List<PlannedPayment>,
    val recurring: List<RecurringItem>,
    val recent: List<Transaction>,
    val totalCount: Int,
)

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))
    private val period = MutableStateFlow(Period.currentMonth())

    val data = combine(repo.getAllTransactions(), repo.getPlannedPayments(), period) { txs, planned, p ->
        val now = System.currentTimeMillis()
        val horizon = now + 30L * 86_400_000L
        DashboardData(
            period = p,
            balance = txs.sumOf { if (it.type == TYPE_INCOME) it.amount else -it.amount },
            summary = Analytics.summary(txs, p),
            previous = Analytics.summary(txs, p.previous()),
            topCategories = Analytics.categories(txs, p, TYPE_EXPENSE, p.previous()).take(4),
            upcoming = planned.filter { !it.isDone && it.plannedDate <= horizon }.sortedBy { it.plannedDate }.take(5),
            recurring = Analytics.recurring(txs, 5),
            recent = txs.take(5),
            totalCount = txs.size,
        )
    }.flowOn(Dispatchers.Default).asLiveData()

    fun previous() { period.value = period.value.previous() }
    fun next() { period.value = period.value.next() }
}
