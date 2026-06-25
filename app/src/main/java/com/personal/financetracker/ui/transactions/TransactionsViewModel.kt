package com.personal.financetracker.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Category
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class TransactionsViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val transactions = repo.getAllTransactions().asLiveData()

    fun delete(transaction: Transaction) {
        viewModelScope.launch { repo.deleteTransaction(transaction) }
    }

    /** Result of a CSV import. */
    data class ImportResult(val imported: Int, val skipped: Int, val newCategories: Int)

    /** Date formats accepted on import (export uses "d MMM yyyy"). */
    private val dateFormats = listOf("d MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy")

    fun importCsv(content: String, onResult: (ImportResult) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { parseAndInsert(content) }
            onResult(result)
        }
    }

    private suspend fun parseAndInsert(content: String): ImportResult {
        // Build a lookup of existing categories keyed by type + lowercased name.
        val byKey = HashMap<String, Category>()
        repo.getCategoriesOnce().forEach { byKey[catKey(it.type, it.name)] = it }

        var imported = 0
        var skipped = 0
        var newCategories = 0

        val lines = content.split(Regex("\\r\\n|\\n|\\r"))
        lines.forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed

            // Skip a header row like "Date,Type,Amount,Category,Note"
            val lower = line.lowercase()
            if (index == 0 && lower.startsWith("date") && lower.contains("amount")) {
                return@forEachIndexed
            }

            val parts = line.split(",")
            if (parts.size < 4) { skipped++; return@forEachIndexed }

            val date = parseDate(parts[0].trim())
            val type = parseType(parts[1].trim())
            val amount = parseAmount(parts[2].trim())
            val catName = parts[3].trim()
            val note = if (parts.size >= 5) parts.subList(4, parts.size).joinToString(",").trim() else ""

            if (date == null || type == null || amount == null || catName.isEmpty()) {
                skipped++; return@forEachIndexed
            }

            val key = catKey(type, catName)
            var cat = byKey[key]
            if (cat == null) {
                val newCat = Category(
                    name = catName,
                    emoji = if (type == "income") "💫" else "📦",
                    color = "#94A3B8",
                    type = type,
                    isDefault = false
                )
                val id = repo.insertCategory(newCat)
                cat = newCat.copy(id = id)
                byKey[key] = cat
                newCategories++
            }

            repo.insertTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    categoryId = cat.id,
                    categoryName = cat.name,
                    categoryEmoji = cat.emoji,
                    categoryColor = cat.color,
                    note = note,
                    date = date
                )
            )
            imported++
        }

        return ImportResult(imported, skipped, newCategories)
    }

    private fun catKey(type: String, name: String) = type + "|" + name.lowercase().trim()

    private fun parseType(raw: String): String? {
        val t = raw.lowercase()
        return when {
            t.startsWith("inc") -> "income"
            t.startsWith("exp") -> "expense"
            else -> null
        }
    }

    private fun parseDate(s: String): Long? {
        for (f in dateFormats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(s)
                if (parsed != null) return parsed.time
            } catch (_: Exception) { /* try next format */ }
        }
        return null
    }

    private fun parseAmount(s: String): Double? {
        var c = s.replace(Regex("[^0-9.,-]"), "")
        if (c.isEmpty()) return null
        // Normalise separators: if both present, treat comma as thousands; else comma as decimal.
        c = if (c.contains(",") && c.contains(".")) c.replace(",", "")
        else if (c.contains(",")) c.replace(",", ".")
        else c
        return c.toDoubleOrNull()?.let { abs(it) }
    }
}
