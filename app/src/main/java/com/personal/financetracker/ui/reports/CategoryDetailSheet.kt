package com.personal.financetracker.ui.reports

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.personal.financetracker.R
import com.personal.financetracker.databinding.SheetCategoryDetailBinding
import com.personal.financetracker.domain.TYPE_EXPENSE
import com.personal.financetracker.ui.common.*
import com.personal.financetracker.ui.transactions.TransactionAdapter
import com.personal.financetracker.util.Formatters

/** Drill-down for one category within the currently selected report period. */
class CategoryDetailSheet : BottomSheetDialogFragment() {

    private var _b: SheetCategoryDetailBinding? = null
    private val b get() = _b!!
    private val viewModel: ReportsViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _b = SheetCategoryDetailBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val name = requireArguments().getString(ARG_NAME) ?: return
        val type = requireArguments().getString(ARG_TYPE) ?: TYPE_EXPENSE
        val d = viewModel.categoryDetail(name, type)
        val ctx = requireContext()
        val isExpense = type == TYPE_EXPENSE

        b.tvEmoji.text = d.emoji; b.tvEmoji.tintTile(d.color)
        b.tvName.text = d.name
        b.tvPeriod.text = d.period.label
        b.tvTotal.text = Formatters.formatAmount(d.total)

        b.statShare.tvValue.text = Formatters.formatPctPlain(d.share)
        b.statShare.tvLabel.text = "of ${if (isExpense) "spending" else "income"}"
        b.statCount.tvValue.text = d.count.toString()
        b.statCount.tvLabel.text = getString(R.string.transactions_label)
        b.statAvg.tvValue.text = Formatters.formatCompact(if (d.count > 0) d.total / d.count else 0.0)
        b.statAvg.tvLabel.text = "avg each"
        val diff = d.total - d.compareTotal
        if (d.compareTotal > 0) {
            b.statDelta.tvValue.text = Formatters.formatPct(diff / d.compareTotal)
            b.statDelta.tvValue.applyDeltaColor(diff, higherIsGood = !isExpense)
        } else {
            b.statDelta.tvValue.text = if (d.total > 0) "new" else "—"
        }
        b.statDelta.tvLabel.text = "vs ${d.comparePeriod.mediumLabel}"

        val accent = parseColor(d.color)
        ChartStyle.bar(b.chartHistory).apply { axisLeft.isEnabled = false }
        val set = BarDataSet(d.history.mapIndexed { i, (_, v) -> BarEntry(i.toFloat(), v.toFloat()) }, "").apply {
            colors = d.history.indices.map { if (it == d.history.lastIndex) accent else accent.withAlpha(90) }
            valueTextColor = ctx.color(R.color.text_secondary); valueTextSize = 9f
            valueFormatter = ChartStyle.compactCurrency
            isHighlightEnabled = false
        }
        b.chartHistory.data = BarData(set).apply { barWidth = 0.55f }
        b.chartHistory.xAxis.valueFormatter = IndexAxisValueFormatter(d.history.map { it.first.shortLabel })
        b.chartHistory.xAxis.labelCount = d.history.size
        b.chartHistory.invalidate()

        b.tvTxTitle.text = "${d.count} transactions"
        val adapter = TransactionAdapter(showDate = true) { tx ->
            dismiss()
            requireParentFragment().findNavController()
                .navigate(R.id.action_global_add, Bundle().apply { putLong("transactionId", tx.id) })
        }
        b.rvTransactions.layoutManager = LinearLayoutManager(ctx)
        b.rvTransactions.adapter = adapter
        adapter.submitList(d.transactions)
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }

    companion object {
        private const val ARG_NAME = "name"
        private const val ARG_TYPE = "type"
        fun show(fm: FragmentManager, name: String, type: String) {
            CategoryDetailSheet().apply {
                arguments = Bundle().apply { putString(ARG_NAME, name); putString(ARG_TYPE, type) }
            }.show(fm, "category_detail")
        }
    }
}
