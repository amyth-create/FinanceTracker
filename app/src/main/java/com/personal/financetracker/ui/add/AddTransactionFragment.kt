package com.personal.financetracker.ui.add

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.chip.Chip
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentAddTransactionBinding
import kotlinx.coroutines.launch
import java.util.*

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()

    private var selectedType = "expense"
    private var selectedCategory: Category? = null
    private var selectedDate = System.currentTimeMillis()
    private var editingTransactionId: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editingTransactionId = arguments?.getLong("transactionId") ?: 0L

        updateDateDisplay()

        // Type toggle
        updateTypeUI("expense")

        if (editingTransactionId != 0L) {
            lifecycleScope.launch {
                viewModel.getTransaction(editingTransactionId)?.let { tx ->
                    binding.etAmount.setText(tx.amount.toString())
                    binding.etNote.setText(tx.note)
                    selectedDate = tx.date
                    updateDateDisplay()
                    updateTypeUI(tx.type)
                    selectedCategory = Category(tx.categoryId, tx.categoryName, tx.categoryEmoji, tx.categoryColor, tx.type, false)
                    
                    // Rebuild chips if they are already available to show the selected category
                    val cats = if (tx.type == "expense") viewModel.expenseCategories.value else viewModel.incomeCategories.value
                    cats?.let { buildCategoryChips(it) }
                }
            }
        }

        binding.toggleExpense.setOnClickListener { 
            updateTypeUI("expense") 
        }
        binding.toggleIncome.setOnClickListener { 
            updateTypeUI("income") 
        }

        // Date picker
        binding.tvDate.setOnClickListener { showDatePicker() }

        // Save
        binding.btnSave.setOnClickListener { saveTransaction() }

        // Back
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        // Observe categories
        viewModel.expenseCategories.observe(viewLifecycleOwner) {
            if (selectedType == "expense") buildCategoryChips(it)
        }
        viewModel.incomeCategories.observe(viewLifecycleOwner) {
            if (selectedType == "income") buildCategoryChips(it)
        }
    }

    private fun updateTypeUI(type: String) {
        val typeChanged = selectedType != type
        selectedType = type
        if (typeChanged) {
            selectedCategory = null
        }

        val expColor = ContextCompat.getColor(requireContext(), R.color.expense)
        val incColor = ContextCompat.getColor(requireContext(), R.color.income)
        val transparent = Color.TRANSPARENT

        if (type == "expense") {
            binding.toggleExpense.setBackgroundColor(expColor)
            binding.toggleIncome.setBackgroundColor(transparent)
            binding.btnSave.backgroundTintList =
                android.content.res.ColorStateList.valueOf(expColor)
            binding.btnSave.setText(R.string.save_expense)
            viewModel.expenseCategories.value?.let { buildCategoryChips(it) }
        } else {
            binding.toggleIncome.setBackgroundColor(incColor)
            binding.toggleExpense.setBackgroundColor(transparent)
            binding.btnSave.backgroundTintList =
                android.content.res.ColorStateList.valueOf(incColor)
            binding.btnSave.setText(R.string.save_income)
            viewModel.incomeCategories.value?.let { buildCategoryChips(it) }
        }
    }

    private fun buildCategoryChips(categories: List<Category>) {
        binding.chipGroupCategory.removeAllViews()
        categories.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = getString(R.string.category_chip_format, cat.emoji, cat.name)
                isCheckable = true
                chipBackgroundColor = ContextCompat.getColorStateList(requireContext(), R.color.chip_bg_selector)
                setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                
                if (selectedCategory?.id == cat.id) {
                    isChecked = true
                    setTextColor(try { Color.parseColor(cat.color) } catch (_: Exception) { Color.WHITE })
                }

                setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        selectedCategory = cat
                        setTextColor(try { Color.parseColor(cat.color) } catch (_: Exception) { Color.WHITE })
                    } else {
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary))
                    }
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d)
            selectedDate = cal.timeInMillis
            updateDateDisplay()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateDisplay() {
        val sdf = java.text.SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        binding.tvDate.text = sdf.format(Date(selectedDate))
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString().trim()
        val amount = amountStr.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }
        val cat = selectedCategory
        if (cat == null) {
            Toast.makeText(requireContext(), "Select a category", Toast.LENGTH_SHORT).show()
            return
        }

        val tx = Transaction(
            id = editingTransactionId,
            type = selectedType,
            amount = amount,
            categoryId = cat.id,
            categoryName = cat.name,
            categoryEmoji = cat.emoji,
            categoryColor = cat.color,
            note = binding.etNote.text.toString().trim(),
            date = selectedDate
        )
        viewModel.insert(tx)
        findNavController().popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
