package com.personal.financetracker.ui.common

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.personal.financetracker.R
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.CsvTransfer
import com.personal.financetracker.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Shared import/export behaviour. Construct as a fragment field so the launcher registers in time. */
class CsvActions(private val fragment: Fragment) {

    private val repo by lazy { Repository(AppDatabase.getDatabase(fragment.requireContext())) }

    private val importLauncher: ActivityResultLauncher<Array<String>> =
        fragment.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { import(it) } }

    fun pickAndImport() = importLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values", "application/csv", "*/*"))

    fun export() {
        val ctx = fragment.requireContext()
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            val txs = repo.getAllTransactions().first()
            if (txs.isEmpty()) { Toast.makeText(ctx, "No data to export", Toast.LENGTH_SHORT).show(); return@launch }
            try {
                val file = File(ctx.cacheDir, "finance_transactions.csv")
                withContext(Dispatchers.IO) { file.writeText(CsvTransfer.export(txs)) }
                val uri = FileProvider.getUriForFile(ctx, "com.personal.financetracker.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                fragment.startActivity(Intent.createChooser(intent, ctx.getString(R.string.export_csv)))
            } catch (e: Exception) {
                Toast.makeText(ctx, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun import(uri: Uri) {
        val ctx = fragment.requireContext()
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                }
                if (content.isNullOrBlank()) { Toast.makeText(ctx, "File is empty", Toast.LENGTH_SHORT).show(); return@launch }
                val r = withContext(Dispatchers.IO) { CsvTransfer.import(repo, content) }
                val res = ctx.resources
                val msg = buildString {
                    append("Imported ${res.getQuantityString(R.plurals.transaction_count, r.imported, r.imported)}.")
                    if (r.newCategories > 0) append("\nAdded ${res.getQuantityString(R.plurals.category_count, r.newCategories, r.newCategories)}.")
                    if (r.skipped > 0) append("\nSkipped ${res.getQuantityString(R.plurals.row_count, r.skipped, r.skipped)} that couldn't be read.")
                }
                AlertDialog.Builder(ctx).setTitle("Import complete").setMessage(msg).setPositiveButton("OK", null).show()
            } catch (e: Exception) {
                Toast.makeText(ctx, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
