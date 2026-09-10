package com.personal.financetracker.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.personal.financetracker.R
import com.personal.financetracker.databinding.FragmentSettingsBinding
import com.personal.financetracker.databinding.ItemSettingsRowBinding
import com.personal.financetracker.ui.common.CsvActions
import com.personal.financetracker.ui.common.visible

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val csv = CsvActions(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnBack.setOnClickListener { findNavController().popBackStack() }

        row(binding.rowCategories, R.drawable.ic_tag, getString(R.string.categories), getString(R.string.settings_categories_sub)) {
            findNavController().navigate(R.id.action_settings_to_categories)
        }
        row(binding.rowExport, R.drawable.ic_export, getString(R.string.export_csv), getString(R.string.settings_export_sub)) { csv.export() }
        row(binding.rowImport, R.drawable.ic_import, getString(R.string.import_csv), getString(R.string.settings_import_sub), last = true) { csv.pickAndImport() }

        val version = try {
            requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        } catch (_: Exception) { "?" }
        binding.tvVersion.text = getString(R.string.settings_version, version)
    }

    private fun row(b: ItemSettingsRowBinding, icon: Int, title: String, sub: String, last: Boolean = false, onClick: () -> Unit) {
        b.icon.setImageResource(icon)
        b.tvTitle.text = title
        b.tvSub.text = sub
        b.divider.visible(!last)
        b.root.setOnClickListener { onClick() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
