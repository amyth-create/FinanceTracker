package com.personal.financetracker.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object Formatters {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = 2
    }

    fun formatAmount(amount: Double): String =
        currencyFormat.format(amount)

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatDateShort(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun formatMonthYear(timestamp: Long): String {
        val sdf = SimpleDateFormat("MMM yy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun startOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun endOfMonth(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    fun monthStartFor(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun monthEndFor(year: Int, month: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        return cal.timeInMillis
    }

    fun currentMonthLabel(): String {
        return SimpleDateFormat("MMMM", Locale.getDefault()).format(Date())
    }

    /** "Jun 26" style short label for a given year/month (month is 0-based). */
    fun monthLabelShort(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        return SimpleDateFormat("MMM yy", Locale.getDefault()).format(cal.time)
    }

    /** "June 2026" style full label for a given year/month (month is 0-based). */
    fun monthLabelFull(year: Int, month: Int): String {
        val cal = Calendar.getInstance()
        cal.set(year, month, 1, 0, 0, 0)
        return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)
    }

    fun currentYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
    fun currentMonth(): Int = Calendar.getInstance().get(Calendar.MONTH)
}
