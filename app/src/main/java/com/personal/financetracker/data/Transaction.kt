package com.personal.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,           // "income" or "expense"
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,   // stored for display, avoids joins
    val categoryEmoji: String,
    val categoryColor: String,
    val note: String = "",
    val date: Long = System.currentTimeMillis()
)
