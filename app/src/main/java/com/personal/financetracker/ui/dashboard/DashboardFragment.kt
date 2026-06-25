package com.personal.financetracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentDashboardBinding
import com.personal.financetracker.ui.transactions.TransactionAdapter
import com.personal.financetracker.util.Formatters
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToInt

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    private data class YearMonth(val year: Int, val month: Int)
    private data class Recur(
        val emoji: String, val name: String, val amount: Double, val nextDue: Long
    )

    private val lifestyleCats = setOf("Food & Drink", "Shopping", "Entertainment")

    private var allTxs: List<Transaction> = emptyList()
    private var availableMonths: List<YearMonth> = emptyList()
    private var selectedMonth: YearMonth? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionAdapter(
            showDelete = false,
            onDelete = {},
            onClick = { tx ->
                val bundle = Bundle().apply { putLong("transactionId", tx.id) }
                findNavController().navigate(R.id.action_dashboard_to_add, bundle)
            }
        )
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = adapter

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_dashboard_to_add)
        }
        binding.tvSeeAll.setOnClickListener {
            findNavController().navigate(R.id.transactions)
        }

        viewModel.allTransactions.observe(viewLifecycleOwner) { txs ->
            allTxs = txs
            availableMonths = monthsFrom(txs)
            ensureSelection()
            buildMonthChips()
            renderHero(txs)
            renderMonth()
            renderRecurring(txs)
            adapter.submitList(txs.take(8))
            binding.emptyState.visibility = if (txs.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    // ---------- Hero (all-time) ----------

    private fun renderHero(txs: List<Transaction>) {
        val income = txs.filter { it.type == "income" }.sumOf { it.amount }
        val expense = txs.filter { it.type == "expense" }.sumOf { it.amount }
        binding.tvBalance.text = Formatters.formatAmount(income - expense)
        binding.tvTotalIncome.text = Formatters.formatAmount(income)
        binding.tvTotalExpense.text = Formatters.formatAmount(expense)
    }

    // ---------- Months / chips ----------

    private fun monthsFrom(txs: List<Transaction>): List<YearMonth> {
        val set = LinkedHashSet<YearMonth>()
        set.add(YearMonth(Formatters.currentYear(), Formatters.currentMonth()))
        val cal = Calendar.getInstance()
        txs.forEach {
            cal.timeInMillis = it.date
            set.add(YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)))
        }
        return set.sortedWith(compareByDescending<YearMonth> { it.year }.thenByDescending { it.month })
    }

    private fun ensureSelection() {
        if (availableMonths.isEmpty()) { selectedMonth = null; return }
        if (selectedMonth == null || selectedMonth !in availableMonths) {
            selectedMonth = availableMonths.first()
        }
    }

    private fun buildMonthChips() {
        val group: ChipGroup = binding.chipGroupMonths
        group.removeAllViews()
        val ctx = requireContext()
        val strokePx = resources.displayMetrics.density * 1f
        availableMonths.forEach { ym ->
            val chip = Chip(ctx).apply {
                text = Formatters.monthLabelShort(ym.year, ym.month)
                isCheckable = true
                isCheckedIconVisible = false
                chipBackgroundColor = ContextCompat.getColorStateList(ctx, R.color.month_chip_bg)
                setTextColor(ContextCompat.getColorStateList(ctx, R.color.month_chip_text))
                chipStrokeColor = ContextCompat.getColorStateList(ctx, R.color.month_chip_stroke)
                chipStrokeWidth = strokePx
                isChecked = (ym == selectedMonth)
                setOnClickListener { selectedMonth = ym; renderMonth() }
            }
            group.addView(chip)
        }
    }

    // ---------- Selected-month sections ----------

    private fun txsForMonth(ym: YearMonth): List<Transaction> {
        val start = Formatters.monthStartFor(ym.year, ym.month)
        val end = Formatters.monthEndFor(ym.year, ym.month)
        return allTxs.filter { it.date in start..end }
    }

    private fun prevMonthOf(ym: YearMonth): YearMonth {
        val cal = Calendar.getInstance()
        cal.set(ym.year, ym.month, 1)
        cal.add(Calendar.MONTH, -1)
        return YearMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
    }

    private fun renderMonth() {
        if (_binding == null) return
        val ym = selectedMonth ?: return
        val txs = txsForMonth(ym)
        val income = txs.filter { it.type == "income" }.sumOf { it.amount }
        val expenses = txs.filter { it.type == "expense" }
        val expense = expenses.sumOf { it.amount }

        binding.tvMonthIncome.text = Formatters.formatAmount(income)
        binding.tvMonthExpense.text = Formatters.formatAmount(expense)
        binding.tvMonthNet.text = Formatters.formatAmount(income - expense)

        // Lifestyle spend
        val food = expenses.filter { it.categoryName == "Food & Drink" }.sumOf { it.amount }
        val shopping = expenses.filter { it.categoryName == "Shopping" }.sumOf { it.amount }
        val fun_ = expenses.filter { it.categoryName == "Entertainment" }.sumOf { it.amount }
        val lifestyle = food + shopping + fun_
        binding.tvLifestyleTotal.text = Formatters.formatAmount(lifestyle)
        binding.tvLifestylePct.text =
            if (expense > 0) getString(R.string.lifestyle_pct_spending, (lifestyle / expense * 100).roundToInt()) else "—"
        binding.tvLifeFood.text = Formatters.formatAmount(food)
        binding.tvLifeShopping.text = Formatters.formatAmount(shopping)
        binding.tvLifeFun.text = Formatters.formatAmount(fun_)

        // Top category
        val top = expenses.groupBy { it.categoryName }
            .map { (name, items) -> name to items.sumOf { it.amount } }
            .maxByOrNull { it.second }
        if (top != null) {
            binding.tvInsightTopcat.text = top.first
            binding.tvInsightTopcatAmt.text = Formatters.formatAmount(top.second)
        } else {
            binding.tvInsightTopcat.text = "—"
            binding.tvInsightTopcatAmt.text = Formatters.formatAmount(0.0)
        }

        // Spend vs last month
        val prev = prevMonthOf(ym)
        val prevExpense = txsForMonth(prev).filter { it.type == "expense" }.sumOf { it.amount }
        if (prevExpense > 0) {
            val pct = (expense - prevExpense) / prevExpense * 100
            val sign = if (pct >= 0) "+" else "−"
            binding.tvInsightVs.text = getString(R.string.percentage_format, sign, abs(pct).roundToInt())
            binding.tvInsightVs.setTextColor(
                ContextCompat.getColor(requireContext(),
                    if (pct > 0) R.color.expense else R.color.income)
            )
        } else {
            binding.tvInsightVs.text = "—"
            binding.tvInsightVs.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
        }
        binding.tvInsightVsSub.text = getString(R.string.vs_last_month, Formatters.monthLabelShort(prev.year, prev.month))

        // Daily average + projected
        val cal = Calendar.getInstance()
        cal.set(ym.year, ym.month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val isCurrent = ym.year == Formatters.currentYear() && ym.month == Formatters.currentMonth()
        val daysElapsed = if (isCurrent) Calendar.getInstance().get(Calendar.DAY_OF_MONTH) else daysInMonth
        val dailyAvg = if (daysElapsed > 0) expense / daysElapsed else 0.0
        binding.tvInsightDaily.text = Formatters.formatAmount(dailyAvg)
        val projected = if (isCurrent) dailyAvg * daysInMonth else expense
        binding.tvInsightProj.text = Formatters.formatAmount(projected)
    }

    // ---------- Recurring / subscriptions ----------

    private fun renderRecurring(txs: List<Transaction>) {
        val recurs = detectRecurring(txs)
        binding.llRecurring.removeAllViews()
        binding.tvRecurringEmpty.visibility = if (recurs.isEmpty()) View.VISIBLE else View.GONE
        recurs.forEach { r ->
            val row = layoutInflater.inflate(R.layout.item_recurring, binding.llRecurring, false)
            row.findViewById<TextView>(R.id.tv_emoji).text = r.emoji
            row.findViewById<TextView>(R.id.tv_name).text = r.name
            row.findViewById<TextView>(R.id.tv_next).text =
                getString(R.string.recurring_next_due, Formatters.formatDateShort(r.nextDue))
            row.findViewById<TextView>(R.id.tv_amount).text = getString(R.string.approx_amount, Formatters.formatAmount(r.amount))
            binding.llRecurring.addView(row)
        }
    }

    private fun detectRecurring(txs: List<Transaction>): List<Recur> {
        val expenses = txs.filter { it.type == "expense" }
        val cal = Calendar.getInstance()
        fun ymKey(date: Long): String {
            cal.timeInMillis = date
            return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
        }
        val result = mutableListOf<Recur>()
        expenses.groupBy { it.categoryName + "|" + it.note.trim().lowercase() }
            .forEach { (_, items) ->
                val months = items.map { ymKey(it.date) }.toSet()
                if (months.size < 3) return@forEach
                val sorted = items.sortedByDescending { it.date }
                val last = sorted.first()
                val amount = median(items.map { it.amount })
                cal.timeInMillis = last.date
                cal.add(Calendar.MONTH, 1)
                val nextDue = cal.timeInMillis
                val name = last.note.ifBlank { last.categoryName }
                result.add(Recur(last.categoryEmoji, name, amount, nextDue))
            }
        return result.sortedByDescending { it.amount }.take(6)
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
