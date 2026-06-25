package com.personal.financetracker.data

import kotlinx.coroutines.flow.Flow

class Repository(private val db: AppDatabase) {

    // Transactions
    fun getAllTransactions(): Flow<List<Transaction>> =
        db.transactionDao().getAllTransactions()

    fun getTransactionsInRange(start: Long, end: Long): Flow<List<Transaction>> =
        db.transactionDao().getTransactionsInRange(start, end)

    suspend fun getTransactionById(id: Long): Transaction? =
        db.transactionDao().getTransactionById(id)

    suspend fun insertTransaction(transaction: Transaction) =
        db.transactionDao().insert(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        db.transactionDao().delete(transaction)

    // Categories
    fun getAllCategories(): Flow<List<Category>> =
        db.categoryDao().getAllCategories()

    fun getCategoriesByType(type: String): Flow<List<Category>> =
        db.categoryDao().getCategoriesByType(type)

    suspend fun getCategoriesOnce(): List<Category> =
        db.categoryDao().getAllOnce()

    suspend fun insertCategory(category: Category): Long =
        db.categoryDao().insert(category)

    suspend fun updateCategory(category: Category) =
        db.categoryDao().update(category)

    suspend fun deleteCategory(category: Category) =
        db.categoryDao().delete(category)

    // Planned payments
    fun getPlannedPayments(): Flow<List<PlannedPayment>> =
        db.plannedPaymentDao().getAll()

    suspend fun getUpcomingPlannedOnce(): List<PlannedPayment> =
        db.plannedPaymentDao().getUpcomingOnce()

    suspend fun getPlannedById(id: Long): PlannedPayment? =
        db.plannedPaymentDao().getById(id)

    suspend fun insertPlanned(payment: PlannedPayment): Long =
        db.plannedPaymentDao().insert(payment)

    suspend fun updatePlanned(payment: PlannedPayment) =
        db.plannedPaymentDao().update(payment)

    suspend fun deletePlanned(payment: PlannedPayment) =
        db.plannedPaymentDao().delete(payment)
}
