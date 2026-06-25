package com.personal.financetracker.ui.categories

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.Category
import com.personal.financetracker.data.Repository
import kotlinx.coroutines.launch

class CategoriesViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(AppDatabase.getDatabase(app))

    val allCategories = repo.getAllCategories().asLiveData()

    fun insert(category: Category) { viewModelScope.launch { repo.insertCategory(category) } }
    fun update(category: Category) { viewModelScope.launch { repo.updateCategory(category) } }
    fun delete(category: Category) { viewModelScope.launch { repo.deleteCategory(category) } }
}
