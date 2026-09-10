package com.personal.financetracker.ui.categories

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.databinding.DialogEditCategoryBinding
import com.personal.financetracker.databinding.FragmentCategoriesBinding
import com.personal.financetracker.ui.common.dpInt
import com.personal.financetracker.ui.common.parseColor

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var adapter: CategoryAdapter
    private var currentType = "expense"

    private val palette = listOf(
        "#FF8C42", "#F0B429", "#4CAF82", "#2DD4A0", "#22D3EE", "#4A9EFF",
        "#818CF8", "#A78BFA", "#E879F9", "#F472B6", "#FF5C7A", "#94A3B8",
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CategoryAdapter(onEdit = { showDialog(it) }, onDelete = { confirmDelete(it) })
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        binding.btnBack.setOnClickListener { findNavController().popBackStack() }
        binding.segType.onSelected = { currentType = if (it == 0) "expense" else "income"; refresh() }
        binding.btnAddCategory.setOnClickListener { showDialog(null) }
        viewModel.allCategories.observe(viewLifecycleOwner) { refresh() }
    }

    private fun refresh() {
        viewModel.allCategories.value?.let { all -> adapter.submitList(all.filter { it.type == currentType }) }
    }

    private fun showDialog(existing: Category?) {
        val d = DialogEditCategoryBinding.inflate(layoutInflater)
        var selectedColor = existing?.color ?: palette.first()
        d.etEmoji.setText(existing?.emoji ?: "")
        d.etName.setText(existing?.name ?: "")

        val colors = if (existing != null && existing.color !in palette) listOf(existing.color) + palette else palette
        colors.forEach { hex ->
            val c = parseColor(hex)
            val chip = Chip(requireContext()).apply {
                text = "  "
                isCheckable = true
                isCheckedIconVisible = true
                checkedIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_check_circle)
                checkedIconTint = ColorStateList.valueOf(0xFFFFFFFF.toInt())
                chipBackgroundColor = ColorStateList.valueOf(c)
                chipStrokeWidth = 0f
                chipMinHeight = dpInt(36).toFloat()
                chipStartPadding = dpInt(10).toFloat(); chipEndPadding = dpInt(10).toFloat()
                isChecked = hex.equals(selectedColor, ignoreCase = true)
                setOnCheckedChangeListener { _, checked -> if (checked) selectedColor = hex }
            }
            d.chipGroupColor.addView(chip)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(if (existing == null) R.string.new_category else R.string.edit_category)
            .setView(d.root)
            .setPositiveButton(R.string.save) { _, _ ->
                val name = d.etName.text.toString().trim()
                if (name.isEmpty()) { Toast.makeText(requireContext(), R.string.enter_name, Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                val emoji = d.etEmoji.text.toString().trim().ifEmpty { existing?.emoji ?: if (currentType == "income") "💫" else "📦" }
                if (existing == null) {
                    viewModel.insert(Category(name = name, emoji = emoji, color = selectedColor, type = currentType))
                } else {
                    viewModel.update(existing.copy(name = name, emoji = emoji, color = selectedColor))
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDelete(cat: Category) {
        if (cat.isDefault) { Toast.makeText(requireContext(), R.string.default_cannot_delete, Toast.LENGTH_SHORT).show(); return }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete \"${cat.name}\"?")
            .setMessage(R.string.delete_category_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(cat) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
