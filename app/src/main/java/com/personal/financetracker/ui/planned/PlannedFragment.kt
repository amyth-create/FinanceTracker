package com.personal.financetracker.ui.planned

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialFadeThrough
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.data.PlannedPayment
import com.personal.financetracker.databinding.DialogAddPlannedBinding
import com.personal.financetracker.databinding.FragmentPlannedBinding
import com.personal.financetracker.ui.common.dp
import com.personal.financetracker.ui.common.visible
import com.personal.financetracker.util.Formatters
import java.util.Calendar

class PlannedFragment : Fragment() {

    private var _binding: FragmentPlannedBinding? = null
    private val binding get() = _binding!!
    private val viewModel: PlannedViewModel by viewModels()
    private lateinit var adapter: PlannedAdapter
    private var allCategories: List<Category> = emptyList()

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentPlannedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = PlannedAdapter(onToggle = { viewModel.toggleDone(it) })
        binding.rvPlanned.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPlanned.adapter = adapter
        ItemTouchHelper(SwipeToDelete()).attachToRecyclerView(binding.rvPlanned)

        binding.fabAddPlanned.setOnClickListener { showAddDialog() }
        maybeRequestNotificationPermission()

        viewModel.categories.observe(viewLifecycleOwner) { allCategories = it }
        viewModel.planned.observe(viewLifecycleOwner) { list ->
            val upcoming = list.filter { !it.isDone }.sortedBy { it.plannedDate }
            val done = list.filter { it.isDone }.sortedByDescending { it.plannedDate }
            val rows = ArrayList<PlannedRow>()
            if (upcoming.isNotEmpty()) {
                rows.add(PlannedRow.Header(getString(R.string.planned_upcoming), upcoming.size))
                upcoming.forEachIndexed { i, p -> rows.add(PlannedRow.Item(p, i == upcoming.lastIndex)) }
            }
            if (done.isNotEmpty()) {
                rows.add(PlannedRow.Header(getString(R.string.planned_done), done.size))
                done.forEachIndexed { i, p -> rows.add(PlannedRow.Item(p, i == done.lastIndex)) }
            }
            val firstLoad = adapter.itemCount == 0 && rows.isNotEmpty()
            adapter.submitList(rows)
            if (firstLoad) binding.rvPlanned.scheduleLayoutAnimation()
            val out = upcoming.filter { it.type == "expense" }.sumOf { it.amount }
            val inc = upcoming.filter { it.type == "income" }.sumOf { it.amount }
            binding.tvPlannedTotal.text = Formatters.formatAmount(out)
            binding.tvPlannedIncome.text = "+${Formatters.formatAmount(inc)}"
            binding.tvPlannedIncome.visible(inc > 0)
            binding.tvPlannedCount.text = getString(R.string.planned_status_format, upcoming.size, done.size)
            binding.emptyState.visible(list.isEmpty())
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun deleteWithUndo(p: PlannedPayment) {
        viewModel.delete(p)
        Snackbar.make(binding.root, R.string.planned_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fabAddPlanned)
            .setAction(R.string.undo) { viewModel.add(p) }
            .show()
    }

    private fun showAddDialog() {
        val d = DialogAddPlannedBinding.inflate(layoutInflater)
        var type = "expense"
        var plannedMillis = System.currentTimeMillis() + 7L * 86_400_000L
        var selectedCat: Category? = null

        fun buildChips() {
            d.chipGroupCategory.removeAllViews()
            selectedCat = null
            allCategories.filter { it.type == type }.forEach { cat ->
                val chip = Chip(requireContext()).apply {
                    text = "${cat.emoji} ${cat.name}"
                    isCheckable = true; isCheckedIconVisible = false
                    chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_bg_selector)
                    chipStrokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke_selector)
                    chipStrokeWidth = resources.displayMetrics.density
                    setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_selector))
                    setOnCheckedChangeListener { _, checked -> if (checked) selectedCat = cat }
                }
                d.chipGroupCategory.addView(chip)
            }
        }
        d.segType.onSelected = { type = if (it == 0) "expense" else "income"; buildChips() }
        buildChips()
        d.tvDate.text = Formatters.formatDate(plannedMillis)
        d.tvDate.setOnClickListener {
            val cal = Calendar.getInstance().apply { timeInMillis = plannedMillis }
            DatePickerDialog(requireContext(), { _, y, m, day ->
                cal.set(y, m, day); plannedMillis = cal.timeInMillis; d.tvDate.text = Formatters.formatDate(plannedMillis)
            }, cal[Calendar.YEAR], cal[Calendar.MONTH], cal[Calendar.DAY_OF_MONTH]).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.plan_payment_title)
            .setView(d.root)
            .setPositiveButton(R.string.add, null)
            .setNegativeButton(R.string.cancel, null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = d.etAmount.text.toString().replace(',', '.').toDoubleOrNull()
                val cat = selectedCat
                when {
                    amount == null || amount <= 0 -> Toast.makeText(requireContext(), R.string.enter_valid_amount, Toast.LENGTH_SHORT).show()
                    cat == null -> Toast.makeText(requireContext(), R.string.select_category, Toast.LENGTH_SHORT).show()
                    else -> {
                        viewModel.add(
                            PlannedPayment(
                                type = type, amount = amount, categoryId = cat.id, categoryName = cat.name,
                                categoryEmoji = cat.emoji, categoryColor = cat.color,
                                note = d.etNote.text.toString().trim(), plannedDate = plannedMillis,
                            )
                        )
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private inner class SwipeToDelete : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val bg = ContextCompat.getDrawable(requireContext(), R.drawable.swipe_delete_bg)!!
        private val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!
        override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder) =
            if (vh.itemViewType == PlannedAdapter.TYPE_ITEM) super.getSwipeDirs(rv, vh) else 0
        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
        override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
            adapter.paymentAt(vh.bindingAdapterPosition)?.let { deleteWithUndo(it) }
        }
        override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
            val v = vh.itemView
            if (dX < 0) {
                bg.setBounds(v.right + dX.toInt(), v.top, v.right, v.bottom); bg.draw(c)
                val size = v.dp(22f).toInt(); val m = (v.height - size) / 2
                icon.setBounds(v.right - m - size, v.top + m, v.right - m, v.bottom - m)
                icon.setTint(0xFFFFFFFF.toInt()); icon.draw(c)
            }
            super.onChildDraw(c, rv, vh, dX, dY, state, active)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
