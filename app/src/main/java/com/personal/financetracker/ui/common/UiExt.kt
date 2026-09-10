package com.personal.financetracker.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.personal.financetracker.R

fun parseColor(hex: String?, fallback: Int = Color.GRAY): Int =
    try { Color.parseColor(hex) } catch (_: Exception) { fallback }

fun Int.withAlpha(alpha: Int): Int = Color.argb(alpha, Color.red(this), Color.green(this), Color.blue(this))

fun View.dp(value: Float): Float = value * resources.displayMetrics.density
fun View.dpInt(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
fun Context.dp(value: Float): Float = value * resources.displayMetrics.density

fun Context.color(res: Int): Int = ContextCompat.getColor(this, res)

/** Tints an emoji tile background with a soft version of the category colour. */
fun TextView.tintTile(hex: String?) {
    val c = parseColor(hex)
    backgroundTintList = ColorStateList.valueOf(c.withAlpha(46))
}

fun View.visible(show: Boolean) { visibility = if (show) View.VISIBLE else View.GONE }

/** Colours a delta label: spending up = bad (red), spending down = good (green). */
fun TextView.applyDeltaColor(delta: Double, higherIsGood: Boolean) {
    val ctx = context
    setTextColor(
        when {
            delta > 0 -> ctx.color(if (higherIsGood) R.color.income else R.color.expense)
            delta < 0 -> ctx.color(if (higherIsGood) R.color.expense else R.color.income)
            else -> ctx.color(R.color.text_secondary)
        }
    )
}
