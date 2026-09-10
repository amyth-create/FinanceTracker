package com.personal.financetracker.ui.add

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.personal.financetracker.ui.common.color
import com.personal.financetracker.util.Formatters
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddTransactionViewModel by viewModels()

    private var selectedType = "expense"
    private var selectedCategoryId: Long? = null
    private var selectedDate = System.currentTimeMillis()
    private var editingId: Long = 0
    private var editing: Transaction? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        editingId = arguments?.getLong("transactionId") ?: 0L
        binding.tvCurrency.text = Formatters.currencySymbol

        binding.segType.onSelected = { setType(if (it == 0) "expense" else "income", resetCategory = true) }
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { confirmDelete() }

        binding.chipToday.setOnClickListener { selectedDate = System.currentTimeMillis(); syncDateChips() }
        binding.chipYesterday.setOnClickListener { selectedDate = System.currentTimeMillis() - 86_400_000L; syncDateChips() }
        binding.chipPick.setOnClickListener { showDatePicker() }
        syncDateChips()

        viewModel.expenseCategories.observe(viewLifecycleOwner) { if (selectedType == "expense") buildChips(it) }
        viewModel.incomeCategories.observe(viewLifecycleOwner) { if (selectedType == "income") buildChips(it) }

        if (editingId != 0L) {
            binding.tvTitle.text = getString(R.string.edit_transaction)
            binding.btnDelete.visibility = View.VISIBLE
            viewLifecycleOwner.lifecycleScope.launch {
                val tx = viewModel.getTransaction(editingId) ?: return@launch
                if (_binding == null) return@launch
                editing = tx
                binding.etAmount.setText(String.format(Locale.US, "%.2f", tx.amount))
                binding.etNote.setText(tx.note)
                selectedDate = tx.date
                selectedCategoryId = tx.categoryId
                binding.segType.select(if (tx.type == "expense") 0 else 1)
                setType(tx.type, resetCategory = false)
                syncDateChips()
            }
        } else {
            setType("expense", resetCategory = false)
            binding.etAmount.requestFocus()
        }
    }

    private fun setType(type: String, resetCategory: Boolean) {
        if (resetCategory && type != selectedType) selectedCategoryId = null
        selectedType = type
        val isExpense = type == "expense"
        binding.btnSave.setText(if (isExpense) R.string.save_expense else R.string.save_income)
        binding.btnSave.backgroundTintList = ContextCompat.getColorStateList(requireContext(), if (isExpense) R.color.expense else R.color.income)
        binding.btnSave.setTextColor(requireContext().color(if (isExpense) R.color.white else R.color.on_accent))
        (if (isExpense) viewModel.expenseCategories.value else viewModel.incomeCategories.value)?.let { buildChips(it) }
    }

    private fun buildChips(categories: List<Category>) {
        val group = binding.chipGroupCategory
        group.removeAllViews()
        categories.forEach { cat ->
            val chip = Chip(requireContext()).apply {
                text = "${cat.emoji} ${cat.name}"
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_bg_selector)
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke_selector)
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector))
                isChecked = selectedCategoryId == cat.id
                setOnCheckedChangeListener { _, checked -> if (checked) selectedCategoryId = cat.id }
            }
            group.addView(chip)
        }
    }

    private fun syncDateChips() {
        val today = Formatters.startOfDay(System.currentTimeMillis())
        val day = Formatters.startOfDay(selectedDate)
        when (day) {
            today -> { binding.chipToday.isChecked = true; binding.chipPick.text = getString(R.string.pick_date) }
            today - 86_400_000L -> { binding.chipYesterday.isChecked = true; binding.chipPick.text = getString(R.string.pick_date) }
            else -> { binding.chipPick.isChecked = true; binding.chipPick.text = Formatters.formatDate(selectedDate) }
        }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        DatePickerDialog(requireContext(), { _, y, m, d ->
            cal.set(y, m, d); selectedDate = cal.timeInMillis; syncDateChips()
        }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).apply {
            setOnCancelListener { syncDateChips() }
        }.show()
    }

    private fun save() {
        val amount = binding.etAmount.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (amount == null || amount <= 0) { toast(R.string.enter_valid_amount); return }
        val cats = if (selectedType == "expense") viewModel.expenseCategories.value else viewModel.incomeCategories.value
        val cat = cats?.firstOrNull { it.id == selectedCategoryId }
            ?: editing?.takeIf { it.categoryId == selectedCategoryId && it.type == selectedType }
                ?.let { Category(it.categoryId, it.categoryName, it.categoryEmoji, it.categoryColor, it.type) }
        if (cat == null) { toast(R.string.select_category); return }

        viewModel.insert(
            Transaction(
                id = editingId, type = selectedType, amount = amount,
                categoryId = cat.id, categoryName = cat.name, categoryEmoji = cat.emoji, categoryColor = cat.color,
                note = binding.etNote.text.toString().trim(), date = selectedDate,
            )
        )
        findNavController().popBackStack()
    }

    private fun confirmDelete() {
        val tx = editing ?: return
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_transaction_title)
            .setMessage(R.string.cannot_undo)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(tx); findNavController().popBackStack() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun toast(res: Int) = Toast.makeText(requireContext(), res, Toast.LENGTH_SHORT).show()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
