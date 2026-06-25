package com.personal.financetracker.ui.transactions

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentTransactionsBinding
import com.personal.financetracker.util.Formatters
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var adapter: TransactionSectionAdapter
    private val dayKeyFmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let { readAndImport(it) }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionSectionAdapter(
            showDelete = true,
            onDelete = { tx -> confirmDelete(tx) },
            onClick = { tx ->
                val bundle = Bundle().apply { putLong("transactionId", tx.id) }
                findNavController().navigate(R.id.action_transactions_to_add, bundle)
            }
        )
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter

        binding.btnExport.setOnClickListener { exportToCsv() }

        binding.btnImport.setOnClickListener {
            importLauncher.launch(arrayOf("*/*"))
        }

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_transactions_to_add)
        }

        var currentFilter = "all"

        fun applyFilter(filter: String) {
            currentFilter = filter
            binding.chipAll.isChecked = filter == "all"
            binding.chipExpense.isChecked = filter == "expense"
            binding.chipIncome.isChecked = filter == "income"
            viewModel.transactions.value?.let { txs ->
                val filtered = when (filter) {
                    "income" -> txs.filter { it.type == "income" }
                    "expense" -> txs.filter { it.type == "expense" }
                    else -> txs
                }
                adapter.submit(buildRows(filtered))
                binding.tvCount.text = resources.getQuantityString(R.plurals.transaction_count, filtered.size, filtered.size)
                binding.emptyState.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        binding.chipAll.setOnClickListener { applyFilter("all") }
        binding.chipExpense.setOnClickListener { applyFilter("expense") }
        binding.chipIncome.setOnClickListener { applyFilter("income") }

        viewModel.transactions.observe(viewLifecycleOwner) { _ ->
            applyFilter(currentFilter)
        }
    }

    private fun buildRows(txs: List<Transaction>): List<TxRow> {
        val rows = mutableListOf<TxRow>()
        txs.sortedByDescending { it.date }
            .groupBy { dayKeyFmt.format(it.date) }
            .forEach { (_, items) ->
                val net = items.sumOf { if (it.type == "income") it.amount else -it.amount }
                rows.add(TxRow.Header(Formatters.formatDate(items.first().date), net))
                items.forEach { rows.add(TxRow.Item(it)) }
            }
        return rows
    }

    private fun exportToCsv() {
        val transactions = viewModel.transactions.value ?: return
        if (transactions.isEmpty()) {
            Toast.makeText(requireContext(), "No data to export", Toast.LENGTH_SHORT).show()
            return
        }

        val csvHeader = "Date,Type,Amount,Category,Note\n"
        val csvData = transactions.joinToString("\n") { tx ->
            "${Formatters.formatDate(tx.date)},${tx.type},${tx.amount},${tx.categoryName},${tx.note.replace(",", " ")}"
        }
        val csvContent = csvHeader + csvData

        try {
            val fileName = "finance_transactions.csv"
            val file = File(requireContext().cacheDir, fileName)
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(requireContext(), "com.personal.financetracker.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Export CSV"))
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readAndImport(uri: Uri) {
        try {
            val content = requireContext().contentResolver
                .openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            if (content.isNullOrBlank()) {
                Toast.makeText(requireContext(), "File is empty", Toast.LENGTH_SHORT).show()
                return
            }
            viewModel.importCsv(content) { result -> showImportSummary(result) }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showImportSummary(result: TransactionsViewModel.ImportResult) {
        if (_binding == null) return
        val msg = buildString {
            append("Imported ${resources.getQuantityString(R.plurals.transaction_count, result.imported, result.imported)}.")
            if (result.newCategories > 0) {
                append("\nAdded ${resources.getQuantityString(R.plurals.category_count, result.newCategories, result.newCategories)}.")
            }
            if (result.skipped > 0) {
                append("\nSkipped ${resources.getQuantityString(R.plurals.row_count, result.skipped, result.skipped)} that couldn't be read.")
            }
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Import complete")
            .setMessage(msg)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun confirmDelete(tx: Transaction) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete transaction?")
            .setMessage("This cannot be undone.")
            .setPositiveButton("Delete") { _, _ -> viewModel.delete(tx) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
