package com.personal.financetracker.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Repository

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val allTransactions = repo.getAllTransactions().asLiveData()
}
