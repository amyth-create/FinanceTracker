package com.personal.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "planned_payments")
data class PlannedPayment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,            // "income" or "expense"
    val amount: Double,
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val categoryColor: String,
    val note: String = "",
    val plannedDate: Long,
    val isDone: Boolean = false,
    val doneTransactionId: Long? = null
)
