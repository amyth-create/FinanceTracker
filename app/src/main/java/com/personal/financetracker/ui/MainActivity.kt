package com.personal.financetracker.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.ui.setupWithNavController
import com.personal.financetracker.BuildConfig
import com.personal.financetracker.data.AppDatabase
import com.personal.financetracker.data.CsvTransfer
import com.personal.financetracker.data.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import com.personal.financetracker.R
import com.personal.financetracker.databinding.ActivityMainBinding
import com.personal.financetracker.ui.common.visible

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    /** Switch bottom tab from a fragment (keeps bottom-nav back-stack behaviour). */
    fun selectTab(id: Int) { binding.bottomNav.selectedItemId = id }

    private val topLevel = setOf(R.id.dashboard, R.id.transactions, R.id.reports, R.id.planned)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController
        binding.bottomNav.setupWithNavController(navController)
        // Re-selecting the current tab shouldn't rebuild it
        binding.bottomNav.setOnItemReselectedListener { }
        navController.addOnDestinationChangedListener { _, dest, _ ->
            binding.bottomNav.visible(dest.id in topLevel)
        }
        handleDebugImport()
    }

    /**
     * Debug builds only: `adb shell am start ... --es import_csv <path>` seeds the database from a CSV
     * file in the app's private storage. Lets automated runs populate realistic data without the file picker.
     */
    private fun handleDebugImport() {
        if (!BuildConfig.DEBUG) return
        val path = intent?.getStringExtra("import_csv") ?: return
        intent.removeExtra("import_csv")
        lifecycleScope.launch {
            val file = File(path)
            if (!file.exists()) return@launch
            val repo = Repository(AppDatabase.getDatabase(applicationContext))
            withContext(Dispatchers.IO) { CsvTransfer.import(repo, file.readText()) }
        }
    }
}
