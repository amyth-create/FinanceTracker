package com.personal.financetracker.data

import com.personal.financetracker.util.Formatters
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

/** CSV export/import in the app's own format: Date,Type,Amount,Category,Note */
object CsvTransfer {

    data class ImportResult(val imported: Int, val skipped: Int, val newCategories: Int)

    private val dateFormats = listOf("d MMM yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy", "dd.MM.yyyy")

    fun export(transactions: List<Transaction>): String = buildString {
        append("Date,Type,Amount,Category,Note\n")
        transactions.forEach { tx ->
            append(Formatters.formatDate(tx.date)).append(',')
            append(tx.type).append(',')
            append(tx.amount).append(',')
            append(tx.categoryName.replace(",", " ")).append(',')
            append(tx.note.replace(",", " ").replace("\n", " ")).append('\n')
        }
    }

    suspend fun import(repo: Repository, content: String): ImportResult {
        val byKey = HashMap<String, Category>()
        repo.getCategoriesOnce().forEach { byKey[key(it.type, it.name)] = it }
        var imported = 0; var skipped = 0; var newCats = 0

        content.split(Regex("\\r\\n|\\n|\\r")).forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            val lower = line.lowercase()
            if (index == 0 && lower.startsWith("date") && lower.contains("amount")) return@forEachIndexed

            val parts = line.split(",")
            if (parts.size < 4) { skipped++; return@forEachIndexed }
            val date = parseDate(parts[0].trim())
            val type = parseType(parts[1].trim())
            val amount = parseAmount(parts[2].trim())
            val catName = parts[3].trim()
            val note = if (parts.size >= 5) parts.subList(4, parts.size).joinToString(",").trim() else ""
            if (date == null || type == null || amount == null || catName.isEmpty()) { skipped++; return@forEachIndexed }

            val k = key(type, catName)
            var cat = byKey[k]
            if (cat == null) {
                val fresh = Category(
                    name = catName,
                    emoji = if (type == "income") "💫" else "📦",
                    color = "#94A3B8",
                    type = type,
                )
                cat = fresh.copy(id = repo.insertCategory(fresh))
                byKey[k] = cat
                newCats++
            }
            repo.insertTransaction(
                Transaction(
                    type = type, amount = amount, categoryId = cat.id, categoryName = cat.name,
                    categoryEmoji = cat.emoji, categoryColor = cat.color, note = note, date = date,
                )
            )
            imported++
        }
        return ImportResult(imported, skipped, newCats)
    }

    private fun key(type: String, name: String) = type + "|" + name.lowercase().trim()

    private fun parseType(raw: String): String? = when {
        raw.lowercase().startsWith("inc") -> "income"
        raw.lowercase().startsWith("exp") -> "expense"
        else -> null
    }

    private fun parseDate(s: String): Long? {
        for (f in dateFormats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault()).apply { isLenient = false }
                sdf.parse(s)?.let { return it.time }
            } catch (_: Exception) { }
        }
        return null
    }

    private fun parseAmount(s: String): Double? {
        var c = s.replace(Regex("[^0-9.,-]"), "")
        if (c.isEmpty()) return null
        c = when {
            c.contains(",") && c.contains(".") -> c.replace(",", "")
            c.contains(",") -> c.replace(",", ".")
            else -> c
        }
        return c.toDoubleOrNull()?.let { abs(it) }
    }
}
