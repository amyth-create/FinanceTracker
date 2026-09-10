package com.personal.financetracker.domain

import com.personal.financetracker.domain.Period.Companion.toEpochMillis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodTest {

    @Test
    fun monthBoundsAndDayCount() {
        val feb = Period(Granularity.MONTH, 2024, 1) // Feb 2024 (leap year)
        assertEquals(29, feb.dayCount)
        assertEquals(LocalDate.of(2024, 2, 1), feb.startDate)
        assertEquals(LocalDate.of(2024, 3, 1), feb.endDateExclusive)
        assertTrue(feb.contains(LocalDate.of(2024, 2, 29).toEpochMillis() + 5_000))
        assertFalse(feb.contains(LocalDate.of(2024, 3, 1).toEpochMillis()))
    }

    @Test
    fun dayCountSurvivesDstChange() {
        // March in Europe includes a DST switch; must still be 31 days.
        assertEquals(31, Period(Granularity.MONTH, 2026, 2).dayCount)
        assertEquals(31, Period(Granularity.MONTH, 2026, 9).dayCount)
    }

    @Test
    fun quarterAndYear() {
        val q4 = Period.of(Granularity.QUARTER, LocalDate.of(2026, 11, 15).toEpochMillis())
        assertEquals(Period(Granularity.QUARTER, 2026, 9), q4)
        assertEquals("Q4 2026", q4.label)
        assertEquals(92, q4.dayCount)
        assertEquals(Period(Granularity.QUARTER, 2027, 0), q4.next())
        assertEquals(Period(Granularity.QUARTER, 2026, 6), q4.previous())

        val y = Period(Granularity.YEAR, 2024)
        assertEquals(366, y.dayCount)
        assertEquals(Period(Granularity.YEAR, 2023), y.lastYear())
    }

    @Test
    fun shiftAcrossYearBoundary() {
        val jan = Period(Granularity.MONTH, 2026, 0)
        assertEquals(Period(Granularity.MONTH, 2025, 11), jan.previous())
        assertEquals(Period(Granularity.MONTH, 2025, 0), jan.lastYear())
        assertEquals(Period(Granularity.MONTH, 2026, 6), jan.shift(6))
    }

    @Test
    fun dayIndexAndElapsed() {
        val p = Period(Granularity.MONTH, 2026, 8) // Sep 2026
        val day10 = LocalDate.of(2026, 9, 10).toEpochMillis() + 3_600_000L * 13
        assertEquals(9, p.dayIndexOf(day10))
        assertEquals(10, p.daysElapsed(day10))
        assertEquals(30, p.daysElapsed(LocalDate.of(2026, 12, 1).toEpochMillis()))
        assertEquals(0, p.daysElapsed(LocalDate.of(2026, 1, 1).toEpochMillis()))
    }

    @Test
    fun withGranularityKeepsStart() {
        val m = Period(Granularity.MONTH, 2026, 4) // May
        assertEquals(Period(Granularity.QUARTER, 2026, 3), m.withGranularity(Granularity.QUARTER))
        assertEquals(Period(Granularity.YEAR, 2026, 0), m.withGranularity(Granularity.YEAR))
    }
}
