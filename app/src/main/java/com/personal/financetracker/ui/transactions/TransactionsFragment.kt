package com.personal.financetracker.ui.transactions

import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialFadeThrough
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.personal.financetracker.R
import com.personal.financetracker.data.Transaction
import com.personal.financetracker.databinding.FragmentTransactionsBinding
import com.personal.financetracker.ui.common.CsvActions
import com.personal.financetracker.ui.common.dp
import com.personal.financetracker.ui.common.visible

class TransactionsFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TransactionsViewModel by viewModels()
    private lateinit var adapter: TransactionSectionAdapter
    private val csv = CsvActions(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough()
        exitTransition = MaterialFadeThrough()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = TransactionSectionAdapter { tx -> openTransaction(tx) }
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactions.adapter = adapter
        ItemTouchHelper(SwipeToDelete()).attachToRecyclerView(binding.rvTransactions)

        binding.fabAdd.setOnClickListener { findNavController().navigate(R.id.action_global_add) }
        binding.btnMore.setOnClickListener { showMenu() }
        binding.btnSearch.setOnClickListener { toggleSearch(true) }
        binding.btnCloseSearch.setOnClickListener { toggleSearch(false) }
        binding.etSearch.doAfterTextChanged { viewModel.search.value = it?.toString().orEmpty() }

        binding.chipAll.setOnClickListener { viewModel.filter.value = "all" }
        binding.chipExpense.setOnClickListener { viewModel.filter.value = "expense" }
        binding.chipIncome.setOnClickListener { viewModel.filter.value = "income" }
        binding.chipAll.isChecked = true

        viewModel.listing.observe(viewLifecycleOwner) { l ->
            val lm = binding.rvTransactions.layoutManager as LinearLayoutManager
            val wasAtTop = lm.findFirstCompletelyVisibleItemPosition() <= 1
            val firstLoad = adapter.itemCount == 0 && l.rows.isNotEmpty()
            adapter.submitList(l.rows) {
                // Keep a freshly added (newest) transaction visible instead of anchoring to the old first row
                if (wasAtTop && _binding != null) binding.rvTransactions.scrollToPosition(0)
            }
            if (firstLoad) binding.rvTransactions.scheduleLayoutAnimation()
            binding.tvCount.text = resources.getQuantityString(R.plurals.transaction_count, l.count, l.count)
            val empty = l.count == 0
            binding.emptyState.visible(empty)
            binding.tvEmptyTitle.text = getString(if (l.total == 0) R.string.no_transactions_yet else R.string.no_results)
            binding.tvEmptySub.visible(l.total == 0)
        }
    }

    private fun toggleSearch(show: Boolean) {
        binding.searchBar.visible(show)
        binding.headerRow.visible(!show)
        if (show) {
            binding.etSearch.requestFocus()
            (requireContext().getSystemService(InputMethodManager::class.java))
                ?.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        } else {
            binding.etSearch.setText("")
            (requireContext().getSystemService(InputMethodManager::class.java))
                ?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
        }
    }

    private fun showMenu() {
        val pm = PopupMenu(requireContext(), binding.btnMore)
        pm.menu.add(0, 0, 0, getString(R.string.export_csv))
        pm.menu.add(0, 1, 1, getString(R.string.import_csv))
        pm.setOnMenuItemClickListener {
            if (it.itemId == 0) csv.export() else csv.pickAndImport(); true
        }
        pm.show()
    }

    private fun openTransaction(tx: Transaction) {
        findNavController().navigate(R.id.action_global_add, Bundle().apply { putLong("transactionId", tx.id) })
    }

    private fun deleteWithUndo(tx: Transaction) {
        viewModel.delete(tx)
        Snackbar.make(binding.root, R.string.transaction_deleted, Snackbar.LENGTH_LONG)
            .setAnchorView(binding.fabAdd)
            .setAction(R.string.undo) { viewModel.restore(tx) }
            .show()
    }

    /** Swipe a row left to delete it (with undo). Headers can't be swiped. */
    private inner class SwipeToDelete : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
        private val bg = ContextCompat.getDrawable(requireContext(), R.drawable.swipe_delete_bg)!!
        private val icon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_delete)!!

        override fun getSwipeDirs(rv: RecyclerView, vh: RecyclerView.ViewHolder): Int =
            if (vh.itemViewType == TransactionSectionAdapter.TYPE_ITEM) super.getSwipeDirs(rv, vh) else 0

        override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

        override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
            adapter.transactionAt(vh.bindingAdapterPosition)?.let { deleteWithUndo(it) }
        }

        override fun onChildDraw(c: Canvas, rv: RecyclerView, vh: RecyclerView.ViewHolder, dX: Float, dY: Float, state: Int, active: Boolean) {
            val v = vh.itemView
            if (dX < 0) {
                bg.setBounds(v.right + dX.toInt(), v.top, v.right, v.bottom)
                bg.draw(c)
                val size = v.dp(22f).toInt()
                val m = (v.height - size) / 2
                icon.setBounds(v.right - m - size, v.top + m, v.right - m, v.bottom - m)
                icon.setTint(0xFFFFFFFF.toInt())
                icon.draw(c)
            }
            super.onChildDraw(c, rv, vh, dX, dY, state, active)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
