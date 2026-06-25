package com.personal.financetracker.ui.planned

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.databinding.DialogAddPlannedBinding
import com.personal.financetracker.databinding.FragmentPlannedBinding
import com.personal.financetracker.util.Formatters
import java.util.*

class PlannedFragment : Fragment() {

    private var _binding: FragmentPlannedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannedViewModel by viewModels()
    private lateinit var adapter: PlannedAdapter

    private var allCategories: List<Category> = emptyList()

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlannedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = PlannedAdapter(
            onToggle = { viewModel.toggleDone(it) }
        ) { confirmDelete(it) }
        binding.rvPlanned.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlanned.adapter = adapter

        binding.fabAddPlanned.setOnClickListener { showAddDialog() }

        maybeRequestNotificationPermission()

        viewModel.categories.observe(viewLifecycleOwner) { allCategories = it }

        viewModel.planned.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            val upcoming = list.filter { !it.isDone }
            val total = upcoming.asSequence().filter { it.type == "expense" }.sumOf { it.amount }
            binding.tvPlannedTotal.text = Formatters.formatAmount(total)
            binding.tvPlannedCount.text =
                getString(R.string.planned_status_format, upcoming.size, list.size - upcoming.size)
            val empty = list.isEmpty()
            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.rvPlanned.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun confirmDelete(p: PlannedPayment) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_planned_title)
            .setMessage(R.string.delete_planned_message)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.delete(p) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showAddDialog() {
        val d = DialogAddPlannedBinding.inflate(layoutInflater)
        var type = "expense"
        var plannedMillis = System.currentTimeMillis()
        var spinnerCats: List<Category> = emptyList()

        fun populateSpinner() {
            spinnerCats = allCategories.filter { it.type == type }
            val names = spinnerCats.map { "${it.emoji}  ${it.name}" }
            val ad = ArrayAdapter(requireContext(), R.layout.item_spinner, names)
            ad.setDropDownViewResource(R.layout.item_spinner)
            d.spinnerCategory.adapter = ad
        }

        d.chipExpense.isChecked = true
        populateSpinner()
        d.tvDate.text = Formatters.formatDate(plannedMillis)

        d.chipExpense.setOnClickListener { type = "expense"; populateSpinner() }
        d.chipIncome.setOnClickListener { type = "income"; populateSpinner() }

        d.tvDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = plannedMillis }
            DatePickerDialog(
                requireContext(),
                { _, y, m, day ->
                    cal.set(y, m, day)
                    plannedMillis = cal.timeInMillis
                    d.tvDate.text = Formatters.formatDate(plannedMillis)
                },
                cal[Calendar.YEAR],
                cal[Calendar.MONTH],
                cal[Calendar.DAY_OF_MONTH],
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.plan_payment_title)
            .setView(d.root)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = d.etAmount.text.toString().toDoubleOrNull()
                val pos = d.spinnerCategory.selectedItemPosition
                when {
                    amount == null || amount <= 0 ->
                        Toast.makeText(requireContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show()
                    spinnerCats.isEmpty() || pos < 0 ->
                        Toast.makeText(requireContext(), "Pick a category", Toast.LENGTH_SHORT).show()
                    else -> {
                        val cat = spinnerCats[pos]
                        viewModel.add(
                            PlannedPayment(
                                type = type,
                                amount = amount,
                                categoryId = cat.id,
                                categoryName = cat.name,
                                categoryEmoji = cat.emoji,
                                categoryColor = cat.color,
                                note = d.etNote.text.toString().trim(),
                                plannedDate = plannedMillis
                            )
                        )
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
