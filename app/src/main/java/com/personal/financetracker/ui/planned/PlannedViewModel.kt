package com.personal.financetracker.ui.planned

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.notify.ReminderScheduler
import kotlinx.coroutines.launch

class PlannedViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))
    private val appContext = app.applicationContext

    val planned = repo.getPlannedPayments().asLiveData()
    val categories = repo.getAllCategories().asLiveData()

    fun add(payment: PlannedPayment) {
        viewModelScope.launch {
            val id = repo.insertPlanned(payment)
            ReminderScheduler.schedule(appContext, payment.copy(id = id))
        }
    }

    fun delete(payment: PlannedPayment) {
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, payment.id)
            repo.deletePlanned(payment)
        }
    }

    /** Toggle done. Marking done logs a real transaction dated today; un-marking removes it. */
    fun toggleDone(payment: PlannedPayment) {
        viewModelScope.launch {
            if (!payment.isDone) {
                val tx = Transaction(
                    type = payment.type,
                    amount = payment.amount,
                    categoryId = payment.categoryId,
                    categoryName = payment.categoryName,
                    categoryEmoji = payment.categoryEmoji,
                    categoryColor = payment.categoryColor,
                    note = payment.note,
                    date = System.currentTimeMillis(),
                )
                val txId = repo.insertTransaction(tx)
                ReminderScheduler.cancel(appContext, payment.id)
                repo.updatePlanned(payment.copy(isDone = true, doneTransactionId = txId))
            } else {
                payment.doneTransactionId?.let { tid ->
                    repo.getTransactionById(tid)?.let { repo.deleteTransaction(it) }
                }
                repo.updatePlanned(payment.copy(isDone = false, doneTransactionId = null))
                ReminderScheduler.schedule(appContext, payment.copy(isDone = false, doneTransactionId = null))
            }
        }
    }
}
