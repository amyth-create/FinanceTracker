package com.personal.financetracker.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import androidx.lifecycle.viewModelScope

data class ReportQuery(
    val period: Period = Period.currentMonth(),
    val compareMode: CompareMode = CompareMode.PREVIOUS,
)

data class ReportData(
    val query: ReportQuery,
    val comparePeriod: Period,
    val summary: PeriodSummary,
    val compareSummary: PeriodSummary,
    val spendingCats: List<CategoryStat>,
    val incomeCats: List<CategoryStat>,
    val trend: List<TrendPoint>,
    val avgExpenseBefore: Double?,
    val avgIncomeBefore: Double?,
    val paceCurrent: List<Double>,
    val paceCompare: List<Double>,
    val weekdays: List<WeekdayStat>,
    val topExpenseNotes: List<NoteStat>,
    val topIncomeNotes: List<NoteStat>,
    val largestExpenses: List<Transaction>,
    val largestIncome: List<Transaction>,
    val sankey: SankeyData,
    val cashFlow: List<TrendPoint>,
    val balance: List<Pair<Period, Double>>,
) {
    val period get() = query.period
    val hasData get() = summary.txCount > 0
}

class ReportsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    private val query = MutableStateFlow(ReportQuery())
    val queryLive = query.asLiveData()

    /** Selected tab: 0 spending, 1 income, 2 cash flow. Survives rotation. */
    var tab: Int = 0

    val transactions = repo.getAllTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val report = combine(transactions, query) { txs, q -> build(txs, q) }
        .flowOn(Dispatchers.Default)
        .asLiveData()

    fun setPeriod(p: Period) { query.value = query.value.copy(period = p) }
    fun previous() = setPeriod(query.value.period.previous())
    fun next() = setPeriod(query.value.period.next())
    fun setGranularity(g: Granularity) = setPeriod(query.value.period.withGranularity(g))
    fun setCompareMode(m: CompareMode) { query.value = query.value.copy(compareMode = m) }
    fun currentQuery() = query.value

    /** Everything the category sheet needs, computed on demand. */
    fun categoryDetail(name: String, type: String): CategoryDetail {
        val txs = transactions.value
        val q = query.value
        val p = q.period
        val cmp = p.compareTarget(q.compareMode)
        val inP = Analytics.inPeriod(txs, p).filter { it.type == type && it.categoryName == name }
            .sortedByDescending { it.date }
        val typeTotal = Analytics.inPeriod(txs, p).filter { it.type == type }.sumOf { it.amount }
        val total = inP.sumOf { it.amount }
        val cmpTotal = Analytics.inPeriod(txs, cmp).filter { it.type == type && it.categoryName == name }.sumOf { it.amount }
        val history = (5 downTo 0).map { back ->
            val per = p.shift(-back)
            per to Analytics.inPeriod(txs, per).filter { it.type == type && it.categoryName == name }.sumOf { it.amount }
        }
        return CategoryDetail(
            name = name, type = type, period = p, comparePeriod = cmp,
            total = total, share = if (typeTotal > 0) total / typeTotal else 0.0,
            count = inP.size, compareTotal = cmpTotal, history = history, transactions = inP,
            emoji = inP.firstOrNull()?.categoryEmoji ?: "", color = inP.firstOrNull()?.categoryColor ?: "#94A3B8",
        )
    }

    private fun build(txs: List<Transaction>, q: ReportQuery): ReportData {
        val p = q.period
        val cmp = p.compareTarget(q.compareMode)
        val trendCount = when (p.granularity) {
            Granularity.MONTH -> 6; Granularity.QUARTER -> 6; Granularity.YEAR -> 5
        }
        val cashCount = when (p.granularity) {
            Granularity.MONTH -> 12; Granularity.QUARTER -> 8; Granularity.YEAR -> 5
        }
        val isCurrent = p.isCurrent()
        return ReportData(
            query = q,
            comparePeriod = cmp,
            summary = Analytics.summary(txs, p),
            compareSummary = Analytics.summary(txs, cmp),
            spendingCats = Analytics.categories(txs, p, TYPE_EXPENSE, cmp),
            incomeCats = Analytics.categories(txs, p, TYPE_INCOME, cmp),
            trend = Analytics.trend(txs, p, trendCount),
            avgExpenseBefore = Analytics.averageBefore(txs, p, TYPE_EXPENSE, trendCount),
            avgIncomeBefore = Analytics.averageBefore(txs, p, TYPE_INCOME, trendCount),
            paceCurrent = Analytics.cumulativeByDay(txs, p, TYPE_EXPENSE, clampToToday = isCurrent),
            paceCompare = Analytics.cumulativeByDay(txs, cmp, TYPE_EXPENSE, clampToToday = false),
            weekdays = Analytics.byWeekday(txs, p, TYPE_EXPENSE),
            topExpenseNotes = Analytics.topNotes(txs, p, TYPE_EXPENSE),
            topIncomeNotes = Analytics.topNotes(txs, p, TYPE_INCOME),
            largestExpenses = Analytics.largest(txs, p, TYPE_EXPENSE),
            largestIncome = Analytics.largest(txs, p, TYPE_INCOME),
            sankey = Analytics.sankey(txs, p),
            cashFlow = Analytics.trend(txs, p, cashCount),
            balance = Analytics.balanceOverTime(txs),
        )
    }
}

data class CategoryDetail(
    val name: String,
    val type: String,
    val emoji: String,
    val color: String,
    val period: Period,
    val comparePeriod: Period,
    val total: Double,
    val share: Double,
    val count: Int,
    val compareTotal: Double,
    val history: List<Pair<Period, Double>>,
    val transactions: List<Transaction>,
)
