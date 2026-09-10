package com.personal.financetracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class Granularity { MONTH, QUARTER, YEAR }

enum class CompareMode { PREVIOUS, LAST_YEAR }

/**
 * A reporting period: a calendar month, quarter, or year, in the device's time zone.
 * [month] is 0-based (like Calendar.MONTH); for QUARTER it's the first month of the quarter.
 */
data class Period(val granularity: Granularity, val year: Int, val month: Int = 0) {

    private val monthsSpan: Int
        get() = when (granularity) {
            Granularity.MONTH -> 1
            Granularity.QUARTER -> 3
            Granularity.YEAR -> 12
        }

    val startDate: LocalDate get() = LocalDate.of(year, month + 1, 1)
    /** Exclusive end date (first day after the period). */
    val endDateExclusive: LocalDate get() = startDate.plusMonths(monthsSpan.toLong())

    val start: Long get() = startDate.toEpochMillis()
    /** Inclusive end (last millisecond of the period). */
    val end: Long get() = endDateExclusive.toEpochMillis() - 1

    fun contains(ts: Long): Boolean = ts in start..end

    val dayCount: Int get() = ChronoUnit.DAYS.between(startDate, endDateExclusive).toInt()

    /** 0-based day index of a timestamp inside this period, clamped to the period. */
    fun dayIndexOf(ts: Long): Int =
        ChronoUnit.DAYS.between(startDate, ts.toLocalDate()).toInt().coerceIn(0, dayCount - 1)

    /** Days elapsed so far if the period contains "now", else the full day count (0 if in the future). */
    fun daysElapsed(now: Long = System.currentTimeMillis()): Int = when {
        now < start -> 0
        now > end -> dayCount
        else -> dayIndexOf(now) + 1
    }

    fun isCurrent(now: Long = System.currentTimeMillis()) = contains(now)

    fun shift(steps: Int): Period {
        val d = startDate.plusMonths((steps * monthsSpan).toLong())
        return Period(granularity, d.year, d.monthValue - 1)
    }

    fun previous() = shift(-1)
    fun next() = shift(1)
    fun lastYear() = Period(granularity, year - 1, month)

    fun compareTarget(mode: CompareMode) = when (mode) {
        CompareMode.PREVIOUS -> previous()
        CompareMode.LAST_YEAR -> lastYear()
    }

    /** "September 2026", "Q3 2026", "2026" */
    val label: String
        get() = when (granularity) {
            Granularity.MONTH -> startDate.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
            Granularity.QUARTER -> "Q${month / 3 + 1} $year"
            Granularity.YEAR -> year.toString()
        }

    /** "Sep", "Q3", "2026" – for axis labels */
    val shortLabel: String
        get() = when (granularity) {
            Granularity.MONTH -> startDate.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))
            Granularity.QUARTER -> "Q${month / 3 + 1}"
            Granularity.YEAR -> year.toString()
        }

    /** "Sep 26", "Q3 26", "2026" */
    val mediumLabel: String
        get() = when (granularity) {
            Granularity.MONTH -> startDate.format(DateTimeFormatter.ofPattern("MMM yy", Locale.getDefault()))
            Granularity.QUARTER -> "Q${month / 3 + 1} ${year % 100}"
            Granularity.YEAR -> year.toString()
        }

    /** Lowercase noun for copy: "month", "quarter", "year" */
    val noun: String get() = granularity.name.lowercase()

    /** Converts this period into a different granularity that contains its start. */
    fun withGranularity(g: Granularity): Period = of(g, start)

    companion object {
        fun of(granularity: Granularity, ts: Long): Period {
            val d = ts.toLocalDate()
            val m = d.monthValue - 1
            return when (granularity) {
                Granularity.MONTH -> Period(granularity, d.year, m)
                Granularity.QUARTER -> Period(granularity, d.year, (m / 3) * 3)
                Granularity.YEAR -> Period(granularity, d.year, 0)
            }
        }

        fun currentMonth() = of(Granularity.MONTH, System.currentTimeMillis())

        fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
        fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        /** Local calendar day number (days since epoch in the device zone) – stable key for grouping. */
        fun Long.toEpochDay(): Long = toLocalDate().toEpochDay()
    }
}
