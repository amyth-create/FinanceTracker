package com.personal.financetracker.ui.add

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository
import com.personal.financetracker.data.Transaction
import kotlinx.coroutines.launch

class AddTransactionViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val expenseCategories = repo.getCategoriesByType("expense").asLiveData()
    val incomeCategories  = repo.getCategoriesByType("income").asLiveData()

    suspend fun getTransaction(id: Long) = repo.getTransactionById(id)

    fun insert(transaction: Transaction) {
        viewModelScope.launch { repo.insertTransaction(transaction) }
    }
}
