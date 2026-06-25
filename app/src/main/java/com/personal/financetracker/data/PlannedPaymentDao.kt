package com.personal.financetracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PlannedPaymentDao {

    @Query("SELECT * FROM planned_payments ORDER BY isDone ASC, plannedDate ASC")
    fun getAll(): Flow<List<PlannedPayment>>

    @Query("SELECT * FROM planned_payments WHERE isDone = 0")
    suspend fun getUpcomingOnce(): List<PlannedPayment>

    @Query("SELECT * FROM planned_payments WHERE id = :id")
    suspend fun getById(id: Long): PlannedPayment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(payment: PlannedPayment): Long

    @Update
    suspend fun update(payment: PlannedPayment)

    @Delete
    suspend fun delete(payment: PlannedPayment)
}
