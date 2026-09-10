package com.personal.financetracker.ui.reports

import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentReportsBinding
import com.personal.financetracker.databinding.ItemCategoryRowBinding
import com.personal.financetracker.databinding.ItemSimpleRowBinding
import com.personal.financetracker.databinding.ItemStatTileBinding
import com.personal.financetracker.databinding.ItemTransactionBinding
import com.personal.financetracker.databinding.SectionBreakdownBinding
import com.personal.financetracker.databinding.SectionCashflowBinding
import com.personal.financetracker.domain.*
import com.personal.financetracker.ui.common.*
import com.personal.financetracker.ui.transactions.TransactionRow
import com.personal.financetracker.util.Formatters
import kotlin.math.abs

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    private val bd: SectionBreakdownBinding get() = binding.breakdown
    private val cf: SectionCashflowBinding get() = binding.cashflow

    private var data: ReportData? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()

        binding.periodPicker.onPrevious = { viewModel.previous() }
        binding.periodPicker.onNext = { viewModel.next() }
        binding.periodPicker.onLabelClick = { showGranularityMenu() }
        binding.btnCompare.setOnClickListener { showCompareMenu() }

        binding.tabs.select(viewModel.tab)
        binding.tabs.onSelected = { i -> viewModel.tab = i; data?.let { render(it) } }

        viewModel.queryLive.observe(viewLifecycleOwner) { q ->
            binding.periodPicker.bind(q.period)
            binding.tvCompare.text = when (q.compareMode) {
                CompareMode.PREVIOUS -> "vs previous ${q.period.noun}"
                CompareMode.LAST_YEAR -> "vs last year"
            }
        }
        viewModel.report.observe(viewLifecycleOwner) { data = it; render(it) }
    }

    // ---------- Menus ----------

    private fun showGranularityMenu() {
        val pm = PopupMenu(requireContext(), binding.periodPicker, android.view.Gravity.CENTER)
        pm.menu.add(0, 0, 0, getString(R.string.period_month))
        pm.menu.add(0, 1, 1, getString(R.string.period_quarter))
        pm.menu.add(0, 2, 2, getString(R.string.period_year))
        pm.setOnMenuItemClickListener {
            viewModel.setGranularity(Granularity.values()[it.itemId]); true
        }
        pm.show()
    }

    private fun showCompareMenu() {
        val q = viewModel.currentQuery()
        val pm = PopupMenu(requireContext(), binding.btnCompare)
        pm.menu.add(0, 0, 0, "Previous ${q.period.noun}")
        pm.menu.add(0, 1, 1, getString(R.string.compare_last_year))
        pm.setOnMenuItemClickListener {
            viewModel.setCompareMode(if (it.itemId == 0) CompareMode.PREVIOUS else CompareMode.LAST_YEAR); true
        }
        pm.show()
    }

    // ---------- Render ----------

    private fun render(d: ReportData) {
        if (_binding == null) return
        val tab = viewModel.tab
        val showEmpty = !d.hasData && tab != 2
        binding.emptyState.visible(showEmpty)
        binding.breakdown.root.visible(tab != 2 && !showEmpty)
        binding.cashflow.root.visible(tab == 2)
        when (tab) {
            0 -> if (!showEmpty) renderBreakdown(d, TYPE_EXPENSE)
            1 -> if (!showEmpty) renderBreakdown(d, TYPE_INCOME)
            else -> renderCashFlow(d)
        }
    }

    private fun renderBreakdown(d: ReportData, type: String) {
        val isExpense = type == TYPE_EXPENSE
        val s = d.summary; val c = d.compareSummary
        val total = if (isExpense) s.expense else s.income
        val cmpTotal = if (isExpense) c.expense else c.income
        val accent = requireContext().color(if (isExpense) R.color.expense else R.color.income)

        bd.tvTotalLabel.text = getString(if (isExpense) R.string.total_spent else R.string.total_income)
        bd.tvTotal.text = Formatters.formatAmount(total)

        val diff = total - cmpTotal
        val pct = Analytics.pctChange(total, cmpTotal)
        if (cmpTotal == 0.0 && total == 0.0) {
            bd.tvDelta.text = "—"; bd.tvDeltaSub.text = "no data to compare"
            bd.tvDelta.setTextColor(requireContext().color(R.color.text_secondary))
        } else if (diff == 0.0) {
            bd.tvDelta.text = "No change"; bd.tvDeltaSub.text = "vs ${d.comparePeriod.label}"
            bd.tvDelta.setTextColor(requireContext().color(R.color.text_secondary))
        } else {
            val arrow = if (diff >= 0) "▲" else "▼"
            bd.tvDelta.text = "$arrow ${Formatters.formatSigned(diff)}" + (pct?.let { " (${Formatters.formatPct(it)})" } ?: "")
            bd.tvDelta.applyDeltaColor(diff, higherIsGood = !isExpense)
            bd.tvDeltaSub.text = "vs ${d.comparePeriod.label} · ${Formatters.formatAmount(cmpTotal)}"
        }

        bindStat(bd.statPerDay, Formatters.formatAmount(if (isExpense) s.avgExpensePerDay else s.income / s.period.daysElapsed().coerceAtLeast(1)), getString(R.string.per_day))
        bindStat(bd.statPerTx, Formatters.formatAmount(if (isExpense) s.avgExpensePerTx else s.avgIncomePerTx), getString(R.string.per_transaction))
        bindStat(bd.statCount, (if (isExpense) s.expenseCount else s.incomeCount).toString(), getString(R.string.transactions_label))

        renderTrend(d, type, accent)
        renderCategories(if (isExpense) d.spendingCats else d.incomeCats, type, isExpense)

        bd.cardPace.visible(isExpense && d.paceCurrent.isNotEmpty())
        if (isExpense) renderPace(d)
        bd.cardWeekday.visible(isExpense && d.weekdays.any { it.total > 0 })
        if (isExpense) renderWeekday(d)

        val notes = if (isExpense) d.topExpenseNotes else d.topIncomeNotes
        bd.cardNotes.visible(notes.isNotEmpty())
        renderNotes(notes)

        val largest = if (isExpense) d.largestExpenses else d.largestIncome
        bd.cardLargest.visible(largest.isNotEmpty())
        renderLargest(largest)
    }

    private fun bindStat(tile: ItemStatTileBinding, value: String, label: String) {
        tile.tvValue.text = value; tile.tvLabel.text = label
    }

    // ---------- Trend ----------

    private fun renderTrend(d: ReportData, type: String, accent: Int) {
        val isExpense = type == TYPE_EXPENSE
        val pts = d.trend
        val avg = if (isExpense) d.avgExpenseBefore else d.avgIncomeBefore
        bd.tvTrendSub.text = "Last ${pts.size} ${d.period.noun}s"
        bd.tvTrendAvg.text = avg?.let { "avg ${Formatters.formatCompact(it)}" } ?: ""
        bd.tvTrendAvg.visible(avg != null)

        val entries = pts.mapIndexed { i, p -> BarEntry(i.toFloat(), (if (isExpense) p.expense else p.income).toFloat()) }
        val colors = pts.mapIndexed { i, _ -> if (i == pts.lastIndex) accent else accent.withAlpha(90) }
        val set = BarDataSet(entries, "").apply {
            this.colors = colors
            valueTextColor = requireContext().color(R.color.text_secondary)
            valueTextSize = 9f
            valueFormatter = ChartStyle.compactCurrency
            highLightAlpha = 40
        }
        bd.chartTrend.apply {
            data = BarData(set).apply { barWidth = 0.55f }
            xAxis.valueFormatter = IndexAxisValueFormatter(pts.map { it.period.shortLabel })
            xAxis.labelCount = pts.size
            axisLeft.removeAllLimitLines()
            if (avg != null && avg > 0) {
                axisLeft.addLimitLine(LimitLine(avg.toFloat(), "").apply {
                    lineColor = requireContext().color(R.color.text_muted)
                    lineWidth = 1f
                    enableDashedLine(8f, 6f, 0f)
                })
            }
            axisLeft.axisMaximum = maxOf(entries.maxOfOrNull { it.y } ?: 0f, avg?.toFloat() ?: 0f) * 1.2f + 1f
            highlightValues(null)
            invalidate()
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val i = e?.x?.toInt() ?: return
                    if (i in pts.indices && i != pts.lastIndex) viewModel.setPeriod(pts[i].period)
                }
                override fun onNothingSelected() {}
            })
        }
    }

    // ---------- Categories ----------

    private fun renderCategories(cats: List<CategoryStat>, type: String, isExpense: Boolean) {
        bd.tvCatTitle.text = getString(if (isExpense) R.string.by_category else R.string.by_source)
        bd.stackedBar.segments = cats.map { StackedBarView.Segment(it.share.toFloat(), parseColor(it.color)) }
        bd.llCategories.removeAllViews()
        val max = cats.maxOfOrNull { it.amount } ?: 0.0
        cats.forEach { c ->
            val row = ItemCategoryRowBinding.inflate(layoutInflater, bd.llCategories, false)
            row.tvEmoji.text = c.emoji; row.tvEmoji.tintTile(c.color)
            row.tvName.text = c.name
            row.tvAmount.text = Formatters.formatAmount(c.amount)
            row.bar.barColor = parseColor(c.color)
            row.bar.fraction = if (max > 0) (c.amount / max).toFloat() else 0f
            row.tvSub.text = "${resources.getQuantityString(R.plurals.transaction_count, c.count, c.count)} · ${Formatters.formatPctPlain(c.share)}"
            when {
                c.isNew -> { row.tvDelta.text = getString(R.string.category_new); row.tvDelta.setTextColor(requireContext().color(R.color.text_muted)) }
                c.compareAmount == 0.0 -> row.tvDelta.text = ""
                c.delta == 0.0 -> { row.tvDelta.text = "no change"; row.tvDelta.setTextColor(requireContext().color(R.color.text_muted)) }
                else -> {
                    row.tvDelta.text = Formatters.formatSigned(c.delta) + (c.deltaPct?.let { " (${Formatters.formatPct(it)})" } ?: "")
                    row.tvDelta.applyDeltaColor(c.delta, higherIsGood = !isExpense)
                }
            }
            row.root.setOnClickListener { CategoryDetailSheet.show(childFragmentManager, c.name, type) }
            bd.llCategories.addView(row.root)
        }
    }

    // ---------- Pace ----------

    private fun renderPace(d: ReportData) {
        val ctx = requireContext()
        val cur = d.paceCurrent.mapIndexed { i, v -> Entry((i + 1).toFloat(), v.toFloat()) }
        val cmp = d.paceCompare.mapIndexed { i, v -> Entry((i + 1).toFloat(), v.toFloat()) }
        val setCur = LineDataSet(cur, "cur").apply {
            color = ctx.color(R.color.accent); lineWidth = 2.5f
            setDrawCircles(false); setDrawValues(false)
            setDrawFilled(true); fillColor = ctx.color(R.color.accent); fillAlpha = 35
            mode = LineDataSet.Mode.LINEAR
            highLightColor = ctx.color(R.color.accent); setDrawHorizontalHighlightIndicator(false)
        }
        val setCmp = LineDataSet(cmp, "cmp").apply {
            color = ctx.color(R.color.series_prev); lineWidth = 1.5f
            enableDashedLine(10f, 6f, 0f)
            setDrawCircles(false); setDrawValues(false)
            mode = LineDataSet.Mode.LINEAR
            isHighlightEnabled = false
        }
        bd.tvPaceLegendA.text = d.period.mediumLabel
        bd.tvPaceLegendB.text = d.comparePeriod.mediumLabel
        bd.tvPaceSub.text = if (d.period.isCurrent()) {
            val today = d.paceCurrent.lastOrNull() ?: 0.0
            val idx = d.paceCurrent.size - 1
            val cmpSameDay = d.paceCompare.getOrNull(idx)
            if (cmpSameDay != null && cmpSameDay > 0) {
                val diff = today - cmpSameDay
                "Day ${idx + 1}: ${Formatters.formatSigned(diff)} vs the same point in ${d.comparePeriod.mediumLabel}"
            } else getString(R.string.spending_pace_sub)
        } else getString(R.string.spending_pace_sub)

        bd.chartPace.apply {
            data = LineData(setCmp, setCur)
            xAxis.axisMinimum = 1f
            xAxis.axisMaximum = maxOf(d.period.dayCount, d.comparePeriod.dayCount).toFloat()
            xAxis.labelCount = 5
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) = "d${value.toInt()}"
            }
            highlightValues(null)
            invalidate()
        }
    }

    // ---------- Weekday ----------

    private fun renderWeekday(d: ReportData) {
        val ctx = requireContext()
        val entries = d.weekdays.mapIndexed { i, w -> BarEntry(i.toFloat(), w.average.toFloat()) }
        val maxI = d.weekdays.indices.maxByOrNull { d.weekdays[it].average } ?: -1
        val set = BarDataSet(entries, "").apply {
            colors = d.weekdays.indices.map { if (it == maxI) ctx.color(R.color.warning) else ctx.color(R.color.warning).withAlpha(90) }
            valueTextColor = ctx.color(R.color.text_secondary); valueTextSize = 9f
            valueFormatter = ChartStyle.compactCurrency
            isHighlightEnabled = false
        }
        bd.chartWeekday.apply {
            data = BarData(set).apply { barWidth = 0.6f }
            xAxis.valueFormatter = IndexAxisValueFormatter(d.weekdays.map { it.label })
            xAxis.labelCount = 7
            invalidate()
        }
    }

    // ---------- Notes / largest ----------

    private fun renderNotes(notes: List<NoteStat>) {
        bd.llNotes.removeAllViews()
        notes.forEach { n ->
            val row = ItemSimpleRowBinding.inflate(layoutInflater, bd.llNotes, false)
            row.tvEmoji.text = n.emoji
            row.tvTitle.text = n.label
            row.tvSub.text = resources.getQuantityString(R.plurals.transaction_count, n.count, n.count)
            row.tvAmount.text = Formatters.formatAmount(n.amount)
            bd.llNotes.addView(row.root)
        }
    }

    private fun renderLargest(list: List<Transaction>) {
        bd.llLargest.removeAllViews()
        list.forEachIndexed { i, tx ->
            val row = ItemTransactionBinding.inflate(layoutInflater, bd.llLargest, false)
            TransactionRow.bind(row, tx, showDate = true, showDivider = i < list.lastIndex)
            row.root.setOnClickListener { openTransaction(tx) }
            bd.llLargest.addView(row.root)
        }
    }

    private fun openTransaction(tx: Transaction) {
        findNavController().navigate(R.id.action_global_add, Bundle().apply { putLong("transactionId", tx.id) })
    }

    // ---------- Cash flow ----------

    private fun renderCashFlow(d: ReportData) {
        val ctx = requireContext()
        val s = d.summary; val c = d.compareSummary
        cf.tvNet.text = Formatters.formatSigned(s.net)
        cf.tvNet.setTextColor(ctx.color(if (s.net >= 0) R.color.text_primary else R.color.expense))
        val rate = s.savingsRate
        cf.tvSavings.text = when {
            rate == null -> "No income recorded"
            rate >= 0 -> "Saved ${Formatters.formatPctPlain(rate)} of income"
            else -> "Overspent by ${Formatters.formatPctPlain(-rate)}"
        }
        val good = rate != null && rate >= 0
        cf.tvSavings.setTextColor(ctx.color(if (good) R.color.accent else R.color.expense))
        cf.tvSavings.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ctx.color(if (good) R.color.accent_dim else R.color.expense_dim)
        )
        val netDiff = s.net - c.net
        cf.tvNetDelta.text = if (c.txCount == 0) "" else "${Formatters.formatSigned(netDiff)} vs ${d.comparePeriod.mediumLabel}"
        cf.tvNetDelta.applyDeltaColor(netDiff, higherIsGood = true)
        cf.barFlow.barColor = ctx.color(if (s.expense <= s.income) R.color.expense else R.color.expense)
        cf.barFlow.trackColor = ctx.color(R.color.income_dim)
        cf.barFlow.fraction = if (s.income > 0) (s.expense / s.income).toFloat() else if (s.expense > 0) 1f else 0f
        cf.tvFlowIncome.text = Formatters.formatAmount(s.income)
        cf.tvFlowExpense.text = Formatters.formatAmount(s.expense)

        cf.sankey.data = d.sankey
        val sankeyEmpty = d.sankey.sources.isEmpty() && d.sankey.targets.isEmpty()
        cf.sankey.visible(!sankeyEmpty)
        cf.tvSankeySub.text = when {
            sankeyEmpty -> getString(R.string.no_data_period)
            d.sankey.deficit > 0 -> "Spending exceeded income by ${Formatters.formatAmount(d.sankey.deficit)}"
            else -> getString(R.string.where_money_went_sub)
        }

        renderCashChart(d)
        renderBalance(d)
    }

    private fun renderCashChart(d: ReportData) {
        val ctx = requireContext()
        val pts = d.cashFlow
        cf.tvCashSub.text = "Last ${pts.size} ${d.period.noun}s"
        val inc = BarDataSet(pts.mapIndexed { i, p -> BarEntry(i.toFloat(), p.income.toFloat()) }, "in").apply {
            color = ctx.color(R.color.income); setDrawValues(false); highLightAlpha = 60
        }
        val exp = BarDataSet(pts.mapIndexed { i, p -> BarEntry(i.toFloat(), p.expense.toFloat()) }, "out").apply {
            color = ctx.color(R.color.expense); setDrawValues(false); highLightAlpha = 60
        }
        val groupSpace = 0.24f; val barSpace = 0.02f; val barWidth = 0.36f
        cf.chartCashflow.apply {
            data = BarData(inc, exp).apply { this.barWidth = barWidth }
            xAxis.valueFormatter = IndexAxisValueFormatter(pts.map { it.period.shortLabel })
            xAxis.setCenterAxisLabels(true)
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = pts.size.toFloat()
            xAxis.labelCount = pts.size
            groupBars(0f, groupSpace, barSpace)
            highlightValues(null)
            invalidate()
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val i = h?.let { ((it.x - 0f) / 1f).toInt() } ?: return
                    val p = pts.getOrNull(i) ?: return
                    cf.tvCashDetail.text = "${p.period.mediumLabel} · in ${Formatters.formatCompact(p.income)} · out ${Formatters.formatCompact(p.expense)} · net ${Formatters.formatSigned(p.net)}"
                }
                override fun onNothingSelected() { cf.tvCashDetail.text = getString(R.string.trend_hint) }
            })
        }
        cf.tvCashDetail.text = "Tap a bar to see that ${d.period.noun}"
    }

    private fun renderBalance(d: ReportData) {
        val ctx = requireContext()
        val pts = d.balance
        if (pts.isEmpty()) { cf.chartBalance.data = null; cf.chartBalance.invalidate(); return }
        val entries = pts.mapIndexed { i, (_, v) -> Entry(i.toFloat(), v.toFloat()) }
        val set = LineDataSet(entries, "").apply {
            color = ctx.color(R.color.accent); lineWidth = 2f
            setDrawCircles(false); setDrawValues(false)
            setDrawFilled(true); fillColor = ctx.color(R.color.accent); fillAlpha = 35
            mode = LineDataSet.Mode.CUBIC_BEZIER
            highLightColor = ctx.color(R.color.accent); setDrawHorizontalHighlightIndicator(false)
        }
        val min = entries.minOf { it.y }
        cf.chartBalance.apply {
            data = LineData(set)
            axisLeft.axisMinimum = if (min < 0) min * 1.15f else 0f
            xAxis.valueFormatter = IndexAxisValueFormatter(pts.map { it.first.mediumLabel })
            xAxis.labelCount = minOf(5, pts.size)
            highlightValues(null)
            invalidate()
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val i = e?.x?.toInt() ?: return
                    pts.getOrNull(i)?.let { cf.tvBalanceDetail.text = "${it.first.label} · ${Formatters.formatAmount(it.second)}" }
                }
                override fun onNothingSelected() { cf.tvBalanceDetail.text = "" }
            })
        }
        cf.tvBalanceDetail.text = "Now: ${Formatters.formatAmount(pts.last().second)}"
    }

    // ---------- Chart setup ----------

    private fun setupCharts() {
        ChartStyle.bar(bd.chartTrend)
        ChartStyle.line(bd.chartPace).apply { axisLeft.setLabelCount(3, false) }
        ChartStyle.bar(bd.chartWeekday).apply { axisLeft.isEnabled = false }
        ChartStyle.bar(cf.chartCashflow)
        ChartStyle.line(cf.chartBalance)
        listOf(bd.chartTrend, bd.chartWeekday, cf.chartCashflow).forEach { it.animateY(500) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
