package com.personal.financetracker.ui.reports

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentReportsBinding
import com.personal.financetracker.util.Formatters
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class ReportsFragment : Fragment() {

    private var _binding: FragmentReportsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ReportsViewModel by viewModels()

    /** A calendar month, 0-based month like Calendar.MONTH. */
    private data class YearMonth(val year: Int, val month: Int)

    private var showingExpenses = true
    private var compareMode = false

    private var allTxs: List<Transaction> = emptyList()
    private var availableMonths: List<YearMonth> = emptyList()

    private var selectedMonth: YearMonth? = null
    private var monthA: YearMonth? = null
    private var monthB: YearMonth? = null

    // For chart tap read-outs
    private var donutTotal: Double = 0.0
    private var lineLabels: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupChart()
        setupPie()
        setupLine()

        binding.toggleExpenses.setOnClickListener {
            showingExpenses = true
            applyToggleStyle()
            renderSingle()
            refreshChart()
        }
        binding.toggleIncome.setOnClickListener {
            showingExpenses = false
            applyToggleStyle()
            renderSingle()
            refreshChart()
        }

        binding.segSingle.setOnClickListener { setMode(false) }
        binding.segCompare.setOnClickListener { setMode(true) }

        applyToggleStyle()
        setMode(false)

        viewModel.allTransactions.observe(viewLifecycleOwner) { txs ->
            allTxs = txs
            availableMonths = monthsFrom(txs)
            ensureSelections()
            buildAllChips()
            renderSingle()
            renderCompare()
            updateLine(txs)
        }

        viewModel.last6Months.observe(viewLifecycleOwner) { updateChart(it) }
    }

    // ---------- Mode handling ----------

    private fun setMode(compare: Boolean) {
        compareMode = compare
        binding.containerSingle.visibility = if (compare) View.GONE else View.VISIBLE
        binding.containerCompare.visibility = if (compare) View.VISIBLE else View.GONE

        val sel = ContextCompat.getColor(requireContext(), R.color.black)
        val unsel = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        if (compare) {
            binding.segCompare.setBackgroundResource(R.drawable.seg_selected)
            binding.segCompare.setTextColor(sel)
            binding.segSingle.setBackgroundColor(Color.TRANSPARENT)
            binding.segSingle.setTextColor(unsel)
        } else {
            binding.segSingle.setBackgroundResource(R.drawable.seg_selected)
            binding.segSingle.setTextColor(sel)
            binding.segCompare.setBackgroundColor(Color.TRANSPARENT)
            binding.segCompare.setTextColor(unsel)
        }
    }

    private fun applyToggleStyle() {
        val active = if (showingExpenses)
            ContextCompat.getColor(requireContext(), R.color.expense)
        else ContextCompat.getColor(requireContext(), R.color.income)
        val muted = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.toggleExpenses.setTextColor(if (showingExpenses) active else muted)
        binding.toggleIncome.setTextColor(if (!showingExpenses) active else muted)
    }

    // ---------- Months & chips ----------

    private fun monthsFrom(txs: List<Transaction>): List<YearMonth> {
        val set = LinkedHashSet<YearMonth>()
        set.add(YearMonth(Formatters.currentYear(), Formatters.currentMonth()))
        val cal = Calendar.getInstance()
        txs.forEach {
            cal.timeInMillis = it.date
            set.add(YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
        }
        return set.sortedWith(
            compareByDescending<YearMonth> { it.year }.thenByDescending { it.month }
        )
    }

    private fun ensureSelections() {
        if (availableMonths.isEmpty()) {
            selectedMonth = null; monthA = null; monthB = null; return
        }
        if (selectedMonth == null || selectedMonth !in availableMonths) {
            selectedMonth = availableMonths.first()
        }
        if (monthA == null || monthA !in availableMonths) {
            monthA = availableMonths.first()
        }
        if (monthB == null || monthB !in availableMonths) {
            monthB = if (availableMonths.size > 1) availableMonths[1] else availableMonths.first()
        }
    }

    private fun buildAllChips() {
        buildMonthChips(binding.chipGroupMonths, selectedMonth) { ym ->
            selectedMonth = ym; renderSingle()
        }
        buildMonthChips(binding.chipGroupA, monthA) { ym ->
            monthA = ym; renderCompare()
        }
        buildMonthChips(binding.chipGroupB, monthB) { ym ->
            monthB = ym; renderCompare()
        }
    }

    private fun buildMonthChips(group: ChipGroup, selected: YearMonth?, onSelect: (YearMonth) -> Unit) {
        group.removeAllViews()
        val ctx = requireContext()
        val strokePx = resources.displayMetrics.density * 1f
        availableMonths.forEach { ym ->
            val chip = Chip(ctx).apply {
                text = Formatters.monthLabelShort(ym.year, ym.month)
                isCheckable = true
                isCheckedIconVisible = false
                isClickable = true
                chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.month_chip_bg)
                setTextColor(ContextCompat.getColorStateList(ctx, R.color.month_chip_text))
                chipStrokeColor = ContextCompat.getColorStateList(ctx, R.color.month_chip_stroke)
                chipStrokeWidth = strokePx
                isChecked = (ym == selected)
                setOnClickListener { onSelect(ym) }
            }
            group.addView(chip)
        }
    }

    // ---------- Single month rendering ----------

    private fun txsForMonth(ym: YearMonth): List<Transaction> {
        val start = Formatters.monthStartFor(ym.year, ym.month)
        val end = Formatters.monthEndFor(ym.year, ym.month)
        return allTxs.filter { it.date in start..end }
    }

    private fun renderSingle() {
        if (_binding == null) return
        val ym = selectedMonth ?: return
        val txs = txsForMonth(ym)
        val income = txs.filter { it.type == "income" }.sumOf { it.amount }
        val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
        val savingsRate = if (income > 0) ((income - expense) / income * 100) else 0.0

        binding.tvSelIncome.text = Formatters.formatAmount(income)
        binding.tvSelExpense.text = Formatters.formatAmount(expense)
        binding.tvSelNet.text = Formatters.formatAmount(income - expense)
        binding.tvSavingsRate.text = if (income > 0) getString(R.string.percentage_decimal_format, savingsRate) else "—"

        val typeLabel = if (showingExpenses) "Spending" else "Income"
        binding.tvBreakdownTitle.text =
            getString(R.string.reports_header_format, typeLabel, Formatters.monthLabelFull(ym.year, ym.month))

        val type = if (showingExpenses) "expense" else "income"
        val typed = txs.filter { it.type == type }
        val colorByName = typed.associate { it.categoryName to it.categoryColor }
        val catTotals = typed
            .groupBy { it.categoryName }
            .map { (name, items) ->
                Triple(name, items.first().categoryEmoji, items.sumOf { it.amount })
            }
            .sortedByDescending { it.third }
        val total = catTotals.sumOf { it.third }
        buildCategoryBreakdown(catTotals, total, colorByName)
        updateDonut(ym, catTotals, total, colorByName)
    }

    private fun buildCategoryBreakdown(
        cats: List<Triple<String, String, Double>>,
        total: Double,
        colorByName: Map<String, String>
    ) {
        binding.llCategories.removeAllViews()
        binding.tvCatEmpty.visibility = if (cats.isEmpty()) View.VISIBLE else View.GONE
        val fallback = if (showingExpenses)
            Color.parseColor("#FF5C7A") else Color.parseColor("#2DD4A0")
        cats.forEach { (name, emoji, amount) ->
            val pct = if (total > 0) (amount / total * 100) else 0.0
            val row = layoutInflater.inflate(R.layout.item_category_stat, binding.llCategories, false)
            row.findViewById<TextView>(R.id.tv_emoji).text = emoji
            row.findViewById<TextView>(R.id.tv_name).text = name
            row.findViewById<TextView>(R.id.tv_amount).text = Formatters.formatAmount(amount)
            row.findViewById<TextView>(R.id.tv_pct).text = getString(R.string.percentage_decimal_format, pct)
            val bar = row.findViewById<View>(R.id.view_bar)
            val barColor = try { Color.parseColor(colorByName[name]) } catch (e: Exception) { fallback }
            bar.setBackgroundColor(barColor)
            bar.post {
                bar.layoutParams.width = ((bar.parent as View).width * pct / 100).toInt()
                bar.requestLayout()
            }
            binding.llCategories.addView(row)
        }
    }

    // ---------- Compare rendering ----------

    private fun renderCompare() {
        if (_binding == null) return
        val a = monthA ?: return
        val b = monthB ?: return

        val aLabel = Formatters.monthLabelShort(a.year, a.month)
        val bLabel = Formatters.monthLabelShort(b.year, b.month)
        binding.tvCmpALabel.text = aLabel
        binding.tvCmpBLabel.text = bLabel
        binding.tvCmpLegend.text = "● $aLabel    ● $bLabel    · change"

        val expA = txsForMonth(a).filter { it.type == "expense" }
        val expB = txsForMonth(b).filter { it.type == "expense" }
        val totalA = expA.sumOf { it.amount }
        val totalB = expB.sumOf { it.amount }
        binding.tvCmpATotal.text = Formatters.formatAmount(totalA)
        binding.tvCmpBTotal.text = Formatters.formatAmount(totalB)
        binding.tvCmpDelta.text = totalsDeltaText(aLabel, bLabel, totalA, totalB)
        binding.tvCmpDelta.setTextColor(deltaColor(totalB - totalA))

        // Per-category union
        val mapA = expA.groupBy { it.categoryName }
        val mapB = expB.groupBy { it.categoryName }
        val names = (mapA.keys + mapB.keys).toSet()

        data class Row(val name: String, val emoji: String, val amtA: Double, val amtB: Double)
        val rows = names.map { name ->
            val itemsA = mapA[name].orEmpty()
            val itemsB = mapB[name].orEmpty()
            val emoji = (itemsA + itemsB).firstOrNull()?.categoryEmoji ?: ""
            Row(name, emoji, itemsA.sumOf { it.amount }, itemsB.sumOf { it.amount })
        }.sortedByDescending { maxOf(it.amtA, it.amtB) }

        val maxVal = rows.maxOfOrNull { maxOf(it.amtA, it.amtB) } ?: 0.0

        binding.llCompare.removeAllViews()
        binding.tvCompareEmpty.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE

        rows.forEach { r ->
            val view = layoutInflater.inflate(R.layout.item_category_compare, binding.llCompare, false)
            view.findViewById<TextView>(R.id.tv_emoji).text = r.emoji
            view.findViewById<TextView>(R.id.tv_name).text = r.name
            view.findViewById<TextView>(R.id.tv_amount_a).text = Formatters.formatAmount(r.amtA)
            view.findViewById<TextView>(R.id.tv_amount_b).text = Formatters.formatAmount(r.amtB)

            val diff = r.amtB - r.amtA
            val delta = view.findViewById<TextView>(R.id.tv_delta)
            delta.text = rowDeltaText(r.amtA, r.amtB)
            delta.setTextColor(deltaColor(diff))

            val barA = view.findViewById<View>(R.id.view_bar_a)
            val barB = view.findViewById<View>(R.id.view_bar_b)
            setBarWidth(barA, r.amtA, maxVal)
            setBarWidth(barB, r.amtB, maxVal)

            binding.llCompare.addView(view)
        }
    }

    private fun setBarWidth(bar: View, amount: Double, maxVal: Double) {
        bar.post {
            val track = (bar.parent as View).width
            val w = if (maxVal > 0) (track * amount / maxVal).toInt() else 0
            bar.layoutParams.width = w
            bar.requestLayout()
        }
    }

    private fun totalsDeltaText(aLabel: String, bLabel: String, a: Double, b: Double): String {
        if (a == 0.0 && b == 0.0) return "No spending in either month"
        val diff = b - a
        val money = Formatters.formatAmount(abs(diff))
        val pct = if (a > 0) " (%+.0f%%)".format(diff / a * 100) else ""
        return when {
            diff > 0 -> "$bLabel spent $money more$pct than $aLabel"
            diff < 0 -> "$bLabel spent $money less$pct than $aLabel"
            else -> "$aLabel and $bLabel spent the same"
        }
    }

    private fun rowDeltaText(a: Double, b: Double): String {
        val diff = b - a
        val sign = if (diff >= 0) "+" else "-"
        val money = "$sign${Formatters.formatAmount(abs(diff))}"
        val pct = when {
            a > 0 -> "%+.0f%%".format(diff / a * 100)
            b > 0 -> "new"
            else -> ""
        }
        return if (pct.isEmpty()) money else "$money\n$pct"
    }

    private fun deltaColor(diff: Double): Int = when {
        diff > 0 -> ContextCompat.getColor(requireContext(), R.color.expense)
        diff < 0 -> ContextCompat.getColor(requireContext(), R.color.income)
        else -> ContextCompat.getColor(requireContext(), R.color.text_secondary)
    }

    // ---------- Bar chart ----------

    private fun setupChart() {
        binding.barChart.apply {
            setDrawGridBackground(false)
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setBackgroundColor(Color.parseColor("#13131A"))
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#8A8799")
                textSize = 10f
                granularity = 1f
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1F1F2E")
                textColor = Color.parseColor("#8A8799")
                axisMinimum = 0f
            }
            axisRight.isEnabled = false
            setDrawValueAboveBar(true)
            animateY(600)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val i = e?.x?.toInt() ?: return
                    if (i in lastMonths.indices) {
                        binding.tvBarDetail.text = getString(
                            R.string.chart_value_format,
                            lastMonths[i].label,
                            Formatters.formatAmount(e.y.toDouble())
                        )
                    }
                }
                override fun onNothingSelected() {
                    binding.tvBarDetail.text = getString(R.string.bar_hint)
                }
            })
        }
    }

    private val euroValueFormatter = object : ValueFormatter() {
        override fun getFormattedValue(value: Float): String = "€" + value.roundToInt()
    }

    // ---------- Donut (category share) ----------

    private fun setupPie() {
        binding.pieChart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 62f
            transparentCircleRadius = 0f
            setHoleColor(Color.parseColor("#13131A"))
            setDrawEntryLabels(false)
            setCenterTextColor(Color.parseColor("#F0EEE9"))
            setCenterTextSize(13f)
            legend.isEnabled = false
            setTouchEnabled(true)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            setBackgroundColor(Color.TRANSPARENT)
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val pe = e as? PieEntry ?: return
                    val v = pe.value.toDouble()
                    val pct = if (donutTotal > 0) (v / donutTotal * 100).roundToInt() else 0
                    binding.pieChart.centerText = "${pe.label}\n${Formatters.formatAmount(v)}"
                    binding.tvDonutDetail.text = getString(
                        R.string.pie_slice_format, pe.label, Formatters.formatAmount(v), pct
                    )
                }
                override fun onNothingSelected() {
                    binding.pieChart.centerText = Formatters.formatAmount(donutTotal)
                    binding.tvDonutDetail.text = getString(R.string.donut_hint)
                }
            })
        }
    }

    private fun updateDonut(
        ym: YearMonth,
        cats: List<Triple<String, String, Double>>,
        total: Double,
        colorByName: Map<String, String>
    ) {
        binding.tvDonutSub.text = Formatters.monthLabelFull(ym.year, ym.month)
        donutTotal = total
        binding.tvDonutDetail.text = getString(R.string.donut_hint)
        if (cats.isEmpty() || total <= 0.0) {
            donutTotal = 0.0
            binding.pieChart.data = null
            binding.pieChart.centerText = "No data"
            binding.pieChart.invalidate()
            return
        }
        val entries = cats.map { PieEntry(it.third.toFloat(), it.first) }
        val colors = cats.map {
            try { Color.parseColor(colorByName[it.first]) } catch (e: Exception) { Color.GRAY }
        }
        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            setDrawValues(false)
            sliceSpace = 2f
        }
        binding.pieChart.data = PieData(dataSet)
        binding.pieChart.centerText = Formatters.formatAmount(total)
        binding.pieChart.highlightValues(null)
        binding.pieChart.invalidate()
    }

    // ---------- Balance over time ----------

    private fun setupLine() {
        binding.lineChart.apply {
            setDrawGridBackground(false)
            description.isEnabled = false
            legend.isEnabled = false
            setScaleEnabled(false)
            setTouchEnabled(true)
            isHighlightPerTapEnabled = true
            isHighlightPerDragEnabled = true
            setBackgroundColor(Color.parseColor("#13131A"))
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#8A8799")
                textSize = 9f
                granularity = 1f
                labelCount = 5
            }
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1F1F2E")
                textColor = Color.parseColor("#8A8799")
            }
            axisRight.isEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    val i = e?.x?.toInt() ?: return
                    if (i in lineLabels.indices) {
                        binding.tvLineDetail.text = getString(
                            R.string.chart_value_format,
                            lineLabels[i],
                            Formatters.formatAmount(e.y.toDouble())
                        )
                    }
                }
                override fun onNothingSelected() {
                    binding.tvLineDetail.text = getString(R.string.line_hint)
                }
            })
        }
    }

    private fun updateLine(txs: List<Transaction>) {
        if (_binding == null) return
        binding.tvLineDetail.text = getString(R.string.line_hint)
        if (txs.isEmpty()) {
            lineLabels = emptyList()
            binding.lineChart.data = null
            binding.lineChart.invalidate()
            return
        }
        val cal = Calendar.getInstance()
        val months = txs.map {
            cal.timeInMillis = it.date
            YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
        }.distinct().sortedWith(compareBy({ it.year }, { it.month }))

        var running = 0.0
        val labels = ArrayList<String>()
        val entries = ArrayList<Entry>()
        months.forEachIndexed { i, ym ->
            val m = txsForMonth(ym)
            val net = m.filter { it.type == "income" }.sumOf { it.amount } -
                    m.filter { it.type == "expense" }.sumOf { it.amount }
            running += net
            entries.add(Entry(i.toFloat(), running.toFloat()))
            labels.add(Formatters.monthLabelShort(ym.year, ym.month))
        }

        lineLabels = labels

        val accent = Color.parseColor("#1FC8A8")
        val dataSet = LineDataSet(entries, "").apply {
            color = accent
            lineWidth = 2f
            setDrawCircles(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = accent
            fillAlpha = 40
            mode = LineDataSet.Mode.CUBIC_BEZIER
            highLightColor = accent
            highlightLineWidth = 1.2f
            setDrawHorizontalHighlightIndicator(false)
        }
        binding.lineChart.apply {
            data = LineData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            invalidate()
        }
    }

    private var lastMonths: List<MonthData> = emptyList()

    private fun refreshChart() {
        if (lastMonths.isNotEmpty()) updateChart(lastMonths)
    }

    private fun updateChart(months: List<MonthData>) {
        lastMonths = months
        binding.tvChartTitle.text = if (showingExpenses) "Monthly Spending" else "Monthly Income"
        binding.tvBarDetail.text = getString(R.string.bar_hint)
        val color = if (showingExpenses) Color.parseColor("#FF5C7A") else Color.parseColor("#2DD4A0")
        val entries = months.mapIndexed { i, m ->
            BarEntry(i.toFloat(), (if (showingExpenses) m.expense else m.income).toFloat())
        }
        val dataSet = BarDataSet(entries, "").apply {
            this.color = color
            valueTextColor = Color.parseColor("#8A8799")
            valueTextSize = 9f
            valueFormatter = euroValueFormatter
        }
        binding.barChart.apply {
            data = BarData(dataSet).apply { barWidth = 0.6f }
            xAxis.valueFormatter = IndexAxisValueFormatter(months.map { it.label })
            highlightValues(null)
            invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
