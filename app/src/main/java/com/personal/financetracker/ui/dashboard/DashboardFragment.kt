package com.personal.financetracker.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialFadeThrough
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.personal.financetracker.R
import com.personal.financetracker.databinding.FragmentDashboardBinding
import com.personal.financetracker.databinding.ItemCategoryMiniBinding
import com.personal.financetracker.databinding.ItemSimpleRowBinding
import com.personal.financetracker.domain.Analytics
import com.personal.financetracker.ui.MainActivity
import com.personal.financetracker.ui.common.*
import com.personal.financetracker.ui.transactions.TransactionAdapter
import com.personal.financetracker.util.Formatters
import java.util.concurrent.TimeUnit

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DashboardViewModel by viewModels()
    private lateinit var adapter: TransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvGreeting.text = Formatters.greeting()

        adapter = TransactionAdapter(showDate = true) { tx ->
            findNavController().navigate(R.id.action_global_add, Bundle().apply { putLong("transactionId", tx.id) })
        }
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = adapter

        binding.fabAdd.setOnClickListener { findNavController().navigate(R.id.action_global_add) }
        binding.btnSettings.setOnClickListener { findNavController().navigate(R.id.action_dashboard_to_settings) }
        binding.tvSeeAll.setOnClickListener { (activity as? MainActivity)?.selectTab(R.id.transactions) }
        binding.tvSeeReports.setOnClickListener { (activity as? MainActivity)?.selectTab(R.id.reports) }
        binding.tvSeePlanned.setOnClickListener { (activity as? MainActivity)?.selectTab(R.id.planned) }
        binding.barHero.trackColor = 0x33FFFFFF
        binding.barHero.barColor = 0xFFFFFFFF.toInt()
        binding.periodPicker.onPrevious = { viewModel.previous() }
        binding.periodPicker.onNext = { viewModel.next() }

        viewModel.data.observe(viewLifecycleOwner) { render(it) }
    }

    private fun render(d: DashboardData) {
        val ctx = requireContext()
        val s = d.summary
        binding.periodPicker.bind(d.period)

        // Hero
        binding.tvNet.countTo(s.net) { Formatters.formatSigned(it) }
        binding.tvHeroIncome.countTo(s.income) { Formatters.formatAmount(it) }
        binding.tvHeroExpense.countTo(s.expense) { Formatters.formatAmount(it) }
        binding.tvBalance.countTo(d.balance) { Formatters.formatAmount(it) }
        binding.barHero.animateTo(if (s.income > 0) (s.expense / s.income).toFloat() else if (s.expense > 0) 1f else 0f)
        binding.tvHeroSub.text = when {
            s.income <= 0 && s.expense <= 0 -> "Nothing recorded yet this ${d.period.noun}"
            s.income <= 0 -> "No income recorded this ${d.period.noun}"
            s.expense <= s.income -> "Spent ${Formatters.formatPctPlain(s.expense / s.income)} of income"
            else -> "Spent ${Formatters.formatPctPlain(s.expense / s.income)} of income · over budget"
        }

        // Insight tiles
        binding.tvDaily.countTo(s.avgExpensePerDay) { Formatters.formatAmount(it) }
        binding.tvProjected.countTo(s.projectedExpense) { Formatters.formatAmount(it) }
        binding.tvProjectedSub.text = if (d.period.isCurrent()) "month-end spend at this pace" else "total spent"
        val pct = Analytics.pctChange(s.expense, d.previous.expense)
        if (pct != null) {
            binding.tvVs.text = Formatters.formatPct(pct)
            binding.tvVs.applyDeltaColor(pct, higherIsGood = false)
        } else {
            binding.tvVs.text = "—"; binding.tvVs.setTextColor(ctx.color(R.color.text_primary))
        }
        binding.tvVsSub.text = "vs ${d.period.previous().mediumLabel}"
        val top = d.topCategories.firstOrNull()
        binding.tvTopCat.text = top?.let { "${it.emoji} ${it.name}" } ?: "—"
        binding.tvTopCatSub.text = top?.let { "${Formatters.formatAmount(it.amount)} · ${Formatters.formatPctPlain(it.share)}" } ?: "no spending"

        // Top categories
        binding.llCategories.removeAllViews()
        binding.cardCategories.visible(d.topCategories.isNotEmpty())
        val max = d.topCategories.maxOfOrNull { it.amount } ?: 0.0
        d.topCategories.forEach { c ->
            val row = ItemCategoryMiniBinding.inflate(layoutInflater, binding.llCategories, false)
            row.tvEmoji.text = c.emoji; row.tvEmoji.tintTile(c.color)
            row.tvName.text = c.name
            row.tvAmount.text = Formatters.formatAmount(c.amount)
            row.tvPct.text = Formatters.formatPctPlain(c.share)
            row.bar.barColor = parseColor(c.color)
            binding.llCategories.addView(row.root)
            row.bar.animateTo(if (max > 0) (c.amount / max).toFloat() else 0f, delay = 60L * binding.llCategories.childCount)
        }
        binding.llCategories.staggerChildren()

        // Upcoming
        binding.llUpcoming.removeAllViews()
        binding.tvUpcomingEmpty.visible(d.upcoming.isEmpty())
        val today = Formatters.startOfDay(System.currentTimeMillis())
        d.upcoming.forEach { p ->
            val row = ItemSimpleRowBinding.inflate(layoutInflater, binding.llUpcoming, false)
            row.tvEmoji.text = p.categoryEmoji; row.tvEmoji.tintTile(p.categoryColor)
            row.tvTitle.text = p.note.ifBlank { p.categoryName }
            val days = TimeUnit.MILLISECONDS.toDays(Formatters.startOfDay(p.plannedDate) - today).toInt()
            row.tvSub.text = when {
                days < 0 -> getString(R.string.overdue)
                days == 0 -> getString(R.string.due_today)
                days == 1 -> getString(R.string.tomorrow)
                else -> getString(R.string.in_days, days)
            } + " · ${Formatters.formatDateShort(p.plannedDate)}"
            row.tvSub.setTextColor(ctx.color(if (days < 0) R.color.expense else R.color.text_muted))
            row.tvAmount.text = (if (p.type == "income") "+" else "−") + Formatters.formatAmount(p.amount)
            binding.llUpcoming.addView(row.root)
        }

        // Recurring
        binding.llRecurring.removeAllViews()
        binding.cardRecurring.visible(d.recurring.isNotEmpty())
        d.recurring.forEach { r ->
            val row = ItemSimpleRowBinding.inflate(layoutInflater, binding.llRecurring, false)
            row.tvEmoji.text = r.emoji; row.tvEmoji.tintTile(r.color)
            row.tvTitle.text = r.name
            row.tvSub.text = "≈ monthly · next ${Formatters.formatDateShort(r.nextDue)}"
            row.tvAmount.text = "≈ ${Formatters.formatAmount(r.amount)}"
            binding.llRecurring.addView(row.root)
        }

        // Recent
        adapter.submitList(d.recent)
        binding.emptyState.visible(d.totalCount == 0)
        binding.cardRecent.visible(d.totalCount > 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
