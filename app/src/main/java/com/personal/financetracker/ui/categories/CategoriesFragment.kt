package com.personal.financetracker.ui.categories

import androidx.appcompat.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.databinding.FragmentCategoriesBinding

class CategoriesFragment : Fragment() {

    private var _binding: FragmentCategoriesBinding? = null
    private val binding get() = _binding!!
    private val viewModel: CategoriesViewModel by viewModels()
    private lateinit var adapter: CategoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCategoriesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CategoryAdapter(
            onEdit = { cat -> showEditDialog(cat) },
            onDelete = { cat -> confirmDelete(cat) }
        )
        binding.rvCategories.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategories.adapter = adapter

        binding.chipExpense.isChecked = true
        var currentType = "expense"

        fun filterAndShow(type: String) {
            currentType = type
            viewModel.allCategories.value?.let { all ->
                adapter.submitList(all.filter { it.type == type })
            }
        }

        binding.chipExpense.setOnClickListener { filterAndShow("expense") }
        binding.chipIncome.setOnClickListener  { filterAndShow("income") }

        binding.btnAddCategory.setOnClickListener { showAddDialog(currentType) }

        viewModel.allCategories.observe(viewLifecycleOwner) {
            filterAndShow(currentType)
        }
    }

    private fun showAddDialog(type: String) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }
        val etEmoji = EditText(requireContext()).apply { hint = "Emoji (e.g. 🎮)" }
        val etName  = EditText(requireContext()).apply { hint = "Name" }
        val etColor = EditText(requireContext()).apply { hint = "Color hex (e.g. #FF5733)" }
        layout.addView(labelView("Emoji"))
        layout.addView(etEmoji)
        layout.addView(labelView("Name"))
        layout.addView(etName)
        layout.addView(labelView("Colour"))
        layout.addView(etColor)

        AlertDialog.Builder(requireContext())
            .setTitle("New ${type.replaceFirstChar { it.uppercase() }} Category")
            .setView(layout)
            .setPositiveButton("Create") { _, _ ->
                val emoji = etEmoji.text.toString().trim().ifEmpty { "📦" }
                val name  = etName.text.toString().trim()
                val color = etColor.text.toString().trim().ifEmpty { "#94A3B8" }
                if (name.isEmpty()) { Toast.makeText(requireContext(), "Enter a name", Toast.LENGTH_SHORT).show(); return@setPositiveButton }
                viewModel.insert(Category(name = name, emoji = emoji, color = color, type = type))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(cat: Category) {
        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(64, 32, 64, 0)
        }
        val etEmoji = EditText(requireContext()).apply { setText(cat.emoji) }
        val etName  = EditText(requireContext()).apply { setText(cat.name) }
        val etColor = EditText(requireContext()).apply { setText(cat.color) }
        layout.addView(labelView("Emoji"))
        layout.addView(etEmoji)
        layout.addView(labelView("Name"))
        layout.addView(etName)
        layout.addView(labelView("Colour"))
        layout.addView(etColor)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Category")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val updated = cat.copy(
                    emoji = etEmoji.text.toString().trim().ifEmpty { cat.emoji },
                    name  = etName.text.toString().trim().ifEmpty { cat.name },
                    color = etColor.text.toString().trim().ifEmpty { cat.color }
                )
                viewModel.update(updated)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun confirmDelete(cat: Category) {
        if (cat.isDefault) {
            Toast.makeText(requireContext(), "Default categories cannot be deleted", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Delete \"${cat.name}\"?")
            .setMessage("Existing transactions will keep this category label.")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(cat) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun labelView(text: String) = TextView(requireContext()).apply {
        this.text = text
        setTextColor(Color.parseColor("#8A8799"))
        textSize = 12f
        setPadding(0, 16, 0, 4)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
