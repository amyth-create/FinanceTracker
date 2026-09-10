package com.personal.financetracker.domain

import com.personal.financetracker.data.Transaction
import java.util.Calendar
import kotlin.math.abs

const val TYPE_INCOME = "income"
const val TYPE_EXPENSE = "expense"

data class PeriodSummary(
    val period: Period,
    val income: Double,
    val expense: Double,
    val txCount: Int,
    val expenseCount: Int,
    val incomeCount: Int,
) {
    val net: Double get() = income - expense
    val savingsRate: Double? get() = if (income > 0) (income - expense) / income else null
    val avgExpensePerDay: Double
        get() = period.daysElapsed().let { if (it > 0) expense / it else 0.0 }
    val avgExpensePerTx: Double get() = if (expenseCount > 0) expense / expenseCount else 0.0
    val avgIncomePerTx: Double get() = if (incomeCount > 0) income / incomeCount else 0.0
    /** Expected spend if the current daily pace continues. */
    val projectedExpense: Double
        get() = if (period.isCurrent()) avgExpensePerDay * period.dayCount else expense
}

data class CategoryStat(
    val name: String,
    val emoji: String,
    val color: String,
    val amount: Double,
    val count: Int,
    val share: Double,          // 0..1 of the period total for this type
    val compareAmount: Double,  // same category in the comparison period
) {
    val delta: Double get() = amount - compareAmount
    val deltaPct: Double? get() = if (compareAmount > 0) delta / compareAmount else null
    val isNew: Boolean get() = compareAmount == 0.0 && amount > 0
}

data class TrendPoint(val period: Period, val income: Double, val expense: Double) {
    val net: Double get() = income - expense
}

data class NoteStat(val label: String, val emoji: String, val amount: Double, val count: Int)

data class WeekdayStat(val dayOfWeek: Int, val label: String, val average: Double, val total: Double)

data class SankeyData(
    val sources: List<SankeyNode>,   // income sources
    val targets: List<SankeyNode>,   // expense categories (+ "Saved" when income > expense)
    val deficit: Double,             // when expense > income, amount covered from savings
)

data class SankeyNode(val label: String, val emoji: String, val amount: Double, val color: String)

data class RecurringItem(val emoji: String, val name: String, val amount: Double, val nextDue: Long, val color: String)

/** Pure, UI-free calculations over a transaction list. */
object Analytics {

    fun inPeriod(txs: List<Transaction>, p: Period): List<Transaction> {
        val s = p.start; val e = p.end
        return txs.filter { it.date in s..e }
    }

    fun summary(txs: List<Transaction>, p: Period): PeriodSummary {
        val inP = inPeriod(txs, p)
        val inc = inP.filter { it.type == TYPE_INCOME }
        val exp = inP.filter { it.type == TYPE_EXPENSE }
        return PeriodSummary(
            period = p,
            income = inc.sumOf { it.amount },
            expense = exp.sumOf { it.amount },
            txCount = inP.size,
            expenseCount = exp.size,
            incomeCount = inc.size,
        )
    }

    fun categories(
        txs: List<Transaction>,
        p: Period,
        type: String,
        compare: Period? = null,
    ): List<CategoryStat> {
        val cur = inPeriod(txs, p).filter { it.type == type }
        val cmp = compare?.let { inPeriod(txs, it).filter { t -> t.type == type } }.orEmpty()
        val total = cur.sumOf { it.amount }
        val cmpByName = cmp.groupBy { it.categoryName }.mapValues { (_, v) -> v.sumOf { it.amount } }
        return cur.groupBy { it.categoryName }.map { (name, items) ->
            val amount = items.sumOf { it.amount }
            val newest = items.maxByOrNull { it.date }!!
            CategoryStat(
                name = name,
                emoji = newest.categoryEmoji,
                color = newest.categoryColor,
                amount = amount,
                count = items.size,
                share = if (total > 0) amount / total else 0.0,
                compareAmount = cmpByName[name] ?: 0.0,
            )
        }.sortedByDescending { it.amount }
    }

    /** Last [count] periods ending at [p] (inclusive), oldest first. */
    fun trend(txs: List<Transaction>, p: Period, count: Int): List<TrendPoint> =
        (count - 1 downTo 0).map { back ->
            val per = p.shift(-back)
            val s = summary(txs, per)
            TrendPoint(per, s.income, s.expense)
        }

    /** Average amount per period over the [count] periods before [p] (excludes p). */
    fun averageBefore(txs: List<Transaction>, p: Period, type: String, count: Int): Double? {
        val pts = (1..count).map { back -> summary(txs, p.shift(-back)) }
            .filter { it.txCount > 0 }
        if (pts.isEmpty()) return null
        return pts.map { if (type == TYPE_EXPENSE) it.expense else it.income }.average()
    }

    /**
     * Cumulative daily totals for [type] across the period. Index 0 = day 1.
     * For the current period the list stops at today.
     */
    fun cumulativeByDay(txs: List<Transaction>, p: Period, type: String, clampToToday: Boolean): List<Double> {
        val days = if (clampToToday) p.daysElapsed() else p.dayCount
        if (days <= 0) return emptyList()
        val perDay = DoubleArray(p.dayCount)
        inPeriod(txs, p).filter { it.type == type }.forEach {
            val idx = p.dayIndexOf(it.date)
            perDay[idx] += it.amount
        }
        var run = 0.0
        return (0 until days).map { run += perDay[it]; run }
    }

    fun byWeekday(txs: List<Transaction>, p: Period, type: String): List<WeekdayStat> {
        val cal = Calendar.getInstance()
        val totals = DoubleArray(8)
        inPeriod(txs, p).filter { it.type == type }.forEach {
            cal.timeInMillis = it.date
            totals[cal.get(Calendar.DAY_OF_WEEK)] += it.amount
        }
        // Count occurrences of each weekday within the elapsed part of the period
        val occurrences = IntArray(8)
        cal.timeInMillis = p.start
        val elapsed = p.daysElapsed()
        repeat(elapsed) {
            occurrences[cal.get(Calendar.DAY_OF_WEEK)]++
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDay = Calendar.getInstance().firstDayOfWeek
        val order = (0 until 7).map { ((firstDay - 1 + it) % 7) + 1 }
        val labels = arrayOf("", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return order.map { d ->
            WeekdayStat(d, labels[d], if (occurrences[d] > 0) totals[d] / occurrences[d] else 0.0, totals[d])
        }
    }

    fun topNotes(txs: List<Transaction>, p: Period, type: String, limit: Int = 5): List<NoteStat> =
        inPeriod(txs, p).filter { it.type == type && it.note.isNotBlank() }
            .groupBy { it.note.trim().lowercase() }
            .map { (_, items) ->
                val newest = items.maxByOrNull { it.date }!!
                NoteStat(newest.note.trim(), newest.categoryEmoji, items.sumOf { it.amount }, items.size)
            }
            .sortedByDescending { it.amount }
            .take(limit)

    fun largest(txs: List<Transaction>, p: Period, type: String, limit: Int = 5): List<Transaction> =
        inPeriod(txs, p).filter { it.type == type }.sortedByDescending { it.amount }.take(limit)

    fun sankey(txs: List<Transaction>, p: Period): SankeyData {
        val inP = inPeriod(txs, p)
        val sources = inP.filter { it.type == TYPE_INCOME }.groupBy { it.categoryName }
            .map { (n, items) ->
                val t = items.first()
                SankeyNode(n, t.categoryEmoji, items.sumOf { it.amount }, t.categoryColor)
            }.sortedByDescending { it.amount }
        val targets = inP.filter { it.type == TYPE_EXPENSE }.groupBy { it.categoryName }
            .map { (n, items) ->
                val t = items.first()
                SankeyNode(n, t.categoryEmoji, items.sumOf { it.amount }, t.categoryColor)
            }.sortedByDescending { it.amount }.toMutableList()
        val income = sources.sumOf { it.amount }
        val expense = targets.sumOf { it.amount }
        var deficit = 0.0
        if (income > expense && income > 0) {
            targets.add(SankeyNode("Saved", "💰", income - expense, "#1FC8A8"))
        } else if (expense > income) {
            deficit = expense - income
        }
        return SankeyData(sources, targets, deficit)
    }

    /** Cumulative net balance at the end of every month with data, oldest first. */
    fun balanceOverTime(txs: List<Transaction>): List<Pair<Period, Double>> {
        if (txs.isEmpty()) return emptyList()
        val first = Period.of(Granularity.MONTH, txs.minOf { it.date })
        val last = Period.of(Granularity.MONTH, txs.maxOf { it.date })
        val out = ArrayList<Pair<Period, Double>>()
        var run = 0.0
        var cur = first
        while (cur.start <= last.start) {
            val s = summary(txs, cur)
            run += s.net
            out.add(cur to run)
            cur = cur.next()
        }
        return out
    }

    /** Payments with the same category + note seen in 3+ distinct months and still active (seen in the last ~2 months). */
    fun recurring(txs: List<Transaction>, limit: Int = 6, now: Long = System.currentTimeMillis()): List<RecurringItem> {
        val cal = Calendar.getInstance()
        val activeSince = now - 75L * 86_400_000L
        fun ym(date: Long): Int { cal.timeInMillis = date; return cal.get(Calendar.YEAR) * 12 + cal.get(Calendar.MONTH) }
        return txs.filter { it.type == TYPE_EXPENSE }
            .groupBy { it.categoryName + "|" + it.note.trim().lowercase() }
            .mapNotNull { (_, items) ->
                if (items.map { ym(it.date) }.toSet().size < 3) return@mapNotNull null
                val last = items.maxByOrNull { it.date }!!
                if (last.date < activeSince) return@mapNotNull null
                cal.timeInMillis = last.date
                cal.add(Calendar.MONTH, 1)
                RecurringItem(
                    emoji = last.categoryEmoji,
                    name = last.note.ifBlank { last.categoryName },
                    amount = median(items.map { it.amount }),
                    nextDue = cal.timeInMillis,
                    color = last.categoryColor,
                )
            }
            .sortedByDescending { it.amount }
            .take(limit)
    }

    fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val s = values.sorted()
        val m = s.size / 2
        return if (s.size % 2 == 1) s[m] else (s[m - 1] + s[m]) / 2
    }

    fun pctChange(current: Double, previous: Double): Double? =
        if (previous > 0) (current - previous) / previous else null

    fun signed(v: Double): String = if (v >= 0) "+" else "−"
    fun absOf(v: Double) = abs(v)
}
