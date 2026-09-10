package com.personal.financetracker.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object Formatters {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }
    private val compactFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        maximumFractionDigits = 0
        minimumFractionDigits = 0
    }

    val currencySymbol: String = currencyFormat.currency?.symbol ?: "€"

    fun formatAmount(amount: Double): String = currencyFormat.format(amount)

    /** "€1.234" – for axis labels and dense rows */
    fun formatCompact(amount: Double): String = when {
        abs(amount) >= 1_000_000 -> "%.1fM %s".format(Locale.GERMANY, amount / 1_000_000, currencySymbol)
        abs(amount) >= 10_000 -> "%.1fk %s".format(Locale.GERMANY, amount / 1_000, currencySymbol)
        else -> compactFormat.format(amount)
    }

    /** "+€12,00" / "−€12,00" */
    fun formatSigned(amount: Double): String =
        (if (amount >= 0) "+" else "−") + formatAmount(abs(amount))

    /** "+12%" / "−8%" */
    fun formatPct(fraction: Double): String =
        (if (fraction >= 0) "+" else "−") + "${(abs(fraction) * 100).roundToInt()}%"

    fun formatPctPlain(fraction: Double): String = "${(fraction * 100).roundToInt()}%"

    fun formatDate(timestamp: Long): String =
        SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestamp))

    fun formatDateShort(timestamp: Long): String =
        SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(timestamp))

    fun formatWeekdayDate(timestamp: Long): String =
        SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(timestamp))

    /** "Today", "Yesterday", or "Mon 3 Jun" */
    fun formatRelativeDay(timestamp: Long): String {
        val now = Calendar.getInstance()
        val then = Calendar.getInstance().apply { timeInMillis = timestamp }
        val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
        val dayDiff = if (sameYear) now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR) else Int.MAX_VALUE
        return when (dayDiff) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> if (sameYear) formatWeekdayDate(timestamp) else formatDate(timestamp)
        }
    }

    fun startOfDay(ts: Long): Long = Calendar.getInstance().apply {
        timeInMillis = ts
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun greeting(): String {
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return when {
            h < 5 -> "Good night"
            h < 12 -> "Good morning"
            h < 18 -> "Good afternoon"
            else -> "Good evening"
        }
    }
}
