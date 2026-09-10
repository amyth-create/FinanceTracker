package com.personal.financetracker.ui.categories

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.R
import com.personal.financetracker.data.Category
import com.personal.financetracker.databinding.ItemCategoryBinding
import com.personal.financetracker.ui.common.parseColor
import com.personal.financetracker.ui.common.tintTile
import com.personal.financetracker.ui.common.visible

class CategoryAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemCategoryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(cat: Category) {
            b.tvEmoji.text = cat.emoji
            b.tvEmoji.tintTile(cat.color)
            b.tvName.text = cat.name
            b.colorDot.backgroundTintList = ColorStateList.valueOf(parseColor(cat.color))
            b.tvBadge.visible(cat.isDefault)
            b.btnEdit.setOnClickListener { onEdit(cat) }
            b.btnDelete.setOnClickListener { onDelete(cat) }
            b.btnDelete.alpha = if (cat.isDefault) 0.25f else 1f
            b.root.setOnClickListener { onEdit(cat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(a: Category, b: Category) = a.id == b.id
            override fun areContentsTheSame(a: Category, b: Category) = a == b
        }
    }
}
