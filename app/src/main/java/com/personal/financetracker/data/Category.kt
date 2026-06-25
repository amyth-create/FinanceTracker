package com.personal.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val emoji: String,
    val color: String,
    val type: String,           // "income" or "expense"
    val isDefault: Boolean = false
)

val defaultExpenseCategories = listOf(
    Category(name = "Food & Drink",    emoji = "🍽️", color = "#FF8C42", type = "expense", isDefault = true),
    Category(name = "Groceries",       emoji = "🛒", color = "#4CAF82", type = "expense", isDefault = true),
    Category(name = "Transport",       emoji = "🚇", color = "#4A9EFF", type = "expense", isDefault = true),
    Category(name = "Housing & Rent",  emoji = "🏠", color = "#A78BFA", type = "expense", isDefault = true),
    Category(name = "Utilities",       emoji = "💡", color = "#F0B429", type = "expense", isDefault = true),
    Category(name = "Health",          emoji = "❤️", color = "#FF5C7A", type = "expense", isDefault = true),
    Category(name = "Shopping",        emoji = "🛍️", color = "#F472B6", type = "expense", isDefault = true),
    Category(name = "Entertainment",   emoji = "🎬", color = "#22D3EE", type = "expense", isDefault = true),
    Category(name = "Travel",          emoji = "✈️", color = "#2DD4A0", type = "expense", isDefault = true),
    Category(name = "Subscriptions",   emoji = "📱", color = "#818CF8", type = "expense", isDefault = true),
    Category(name = "Personal Care",   emoji = "💆", color = "#E879F9", type = "expense", isDefault = true),
    Category(name = "Savings",         emoji = "💰", color = "#F0B429", type = "expense", isDefault = true),
    Category(name = "Other",           emoji = "📦", color = "#94A3B8", type = "expense", isDefault = true),
)

val defaultIncomeCategories = listOf(
    Category(name = "Salary",          emoji = "💼", color = "#2DD4A0", type = "income", isDefault = true),
    Category(name = "Freelance",       emoji = "🖥️", color = "#4A9EFF", type = "income", isDefault = true),
    Category(name = "Investments",     emoji = "📈", color = "#F0B429", type = "income", isDefault = true),
    Category(name = "Rental Income",   emoji = "🏘️", color = "#A78BFA", type = "income", isDefault = true),
    Category(name = "Gift",            emoji = "🎁", color = "#F472B6", type = "income", isDefault = true),
    Category(name = "Refund",          emoji = "↩️", color = "#22D3EE", type = "income", isDefault = true),
    Category(name = "Other Income",    emoji = "💫", color = "#94A3B8", type = "income", isDefault = true),
)
