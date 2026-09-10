package com.personal.financetracker.domain

import com.personal.financetracker.data.Transaction
import com.personal.financetracker.domain.Period.Companion.toEpochMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AnalyticsTest {

    private var nextId = 1L
    private fun tx(type: String, amount: Double, cat: String, day: LocalDate, note: String = "") = Transaction(
        id = nextId++, type = type, amount = amount, categoryId = 1, categoryName = cat,
        categoryEmoji = "x", categoryColor = "#FFFFFF", note = note,
        date = day.toEpochMillis() + 12 * 3_600_000L,
    )

    private val sep = Period(Granularity.MONTH, 2026, 8)
    private val aug = sep.previous()

    private val txs = listOf(
        tx(TYPE_INCOME, 3000.0, "Salary", LocalDate.of(2026, 9, 1)),
        tx(TYPE_EXPENSE, 100.0, "Food", LocalDate.of(2026, 9, 2), "lunch"),
        tx(TYPE_EXPENSE, 50.0, "Food", LocalDate.of(2026, 9, 3), "lunch"),
        tx(TYPE_EXPENSE, 850.0, "Rent", LocalDate.of(2026, 9, 5), "flat"),
        tx(TYPE_INCOME, 3000.0, "Salary", LocalDate.of(2026, 8, 1)),
        tx(TYPE_EXPENSE, 200.0, "Food", LocalDate.of(2026, 8, 10)),
        tx(TYPE_EXPENSE, 850.0, "Rent", LocalDate.of(2026, 8, 5), "flat"),
        tx(TYPE_EXPENSE, 850.0, "Rent", LocalDate.of(2026, 7, 5), "flat"),
    )

    @Test
    fun summary() {
        val s = Analytics.summary(txs, sep)
        assertEquals(3000.0, s.income, 0.001)
        assertEquals(1000.0, s.expense, 0.001)
        assertEquals(2000.0, s.net, 0.001)
        assertEquals(2.0 / 3.0, s.savingsRate!!, 0.001)
        assertEquals(4, s.txCount)
        assertEquals(3, s.expenseCount)
        assertEquals(1000.0 / 3, s.avgExpensePerTx, 0.001)
    }

    @Test
    fun categoriesWithComparison() {
        val cats = Analytics.categories(txs, sep, TYPE_EXPENSE, aug)
        assertEquals(listOf("Rent", "Food"), cats.map { it.name })
        val food = cats.first { it.name == "Food" }
        assertEquals(150.0, food.amount, 0.001)
        assertEquals(0.15, food.share, 0.001)
        assertEquals(200.0, food.compareAmount, 0.001)
        assertEquals(-50.0, food.delta, 0.001)
        assertEquals(-0.25, food.deltaPct!!, 0.001)
        assertEquals(2, food.count)
    }

    @Test
    fun trendIsOldestFirstAndEndsAtPeriod() {
        val t = Analytics.trend(txs, sep, 3)
        assertEquals(listOf(Period(Granularity.MONTH, 2026, 6), aug, sep), t.map { it.period })
        assertEquals(listOf(0.0, 3000.0, 3000.0), t.map { it.income })
    }

    @Test
    fun averageBeforeSkipsEmptyPeriods() {
        // Aug: 1050 expense, Jul: 850, Jun: nothing -> average of the two non-empty months
        assertEquals(950.0, Analytics.averageBefore(txs, sep, TYPE_EXPENSE, 3)!!, 0.001)
        assertNull(Analytics.averageBefore(emptyList(), sep, TYPE_EXPENSE, 3))
    }

    @Test
    fun cumulativeByDay() {
        val c = Analytics.cumulativeByDay(txs, sep, TYPE_EXPENSE, clampToToday = false)
        assertEquals(30, c.size)
        assertEquals(0.0, c[0], 0.001)
        assertEquals(100.0, c[1], 0.001)
        assertEquals(150.0, c[2], 0.001)
        assertEquals(1000.0, c[4], 0.001)
        assertEquals(1000.0, c[29], 0.001)
    }

    @Test
    fun topNotesGroupsCaseInsensitively() {
        val notes = Analytics.topNotes(txs, sep, TYPE_EXPENSE)
        assertEquals("flat", notes[0].label)
        assertEquals("lunch", notes[1].label)
        assertEquals(150.0, notes[1].amount, 0.001)
        assertEquals(2, notes[1].count)
    }

    @Test
    fun sankeyAddsSavedNodeWhenIncomeExceedsSpending() {
        val s = Analytics.sankey(txs, sep)
        assertEquals(listOf("Salary"), s.sources.map { it.label })
        assertEquals(listOf("Rent", "Food", "Saved"), s.targets.map { it.label })
        assertEquals(2000.0, s.targets.last().amount, 0.001)
        assertEquals(0.0, s.deficit, 0.001)
    }

    @Test
    fun sankeyReportsDeficit() {
        val only = txs.filter { it.type == TYPE_EXPENSE }
        val s = Analytics.sankey(only, sep)
        assertTrue(s.sources.isEmpty())
        assertEquals(1000.0, s.deficit, 0.001)
    }

    @Test
    fun balanceOverTimeAccumulates() {
        val b = Analytics.balanceOverTime(txs)
        assertEquals(3, b.size)
        assertEquals(-850.0, b[0].second, 0.001)
        assertEquals(-850.0 + 1950.0, b[1].second, 0.001)
        assertEquals(-850.0 + 1950.0 + 2000.0, b[2].second, 0.001)
    }

    @Test
    fun recurringNeedsThreeMonths() {
        val r = Analytics.recurring(txs, now = LocalDate.of(2026, 9, 10).toEpochMillis())
        assertEquals(1, r.size)
        assertEquals("flat", r[0].name)
        assertEquals(850.0, r[0].amount, 0.001)
    }

    @Test
    fun recurringDropsStaleItems() {
        assertTrue(Analytics.recurring(txs, now = LocalDate.of(2027, 3, 1).toEpochMillis()).isEmpty())
    }

    @Test
    fun median() {
        assertEquals(2.0, Analytics.median(listOf(3.0, 1.0, 2.0)), 0.001)
        assertEquals(2.5, Analytics.median(listOf(1.0, 4.0, 2.0, 3.0)), 0.001)
        assertEquals(0.0, Analytics.median(emptyList()), 0.001)
    }
}
