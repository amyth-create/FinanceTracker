package com.personal.financetracker.ui.categories

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.personal.financetracker.data.Category
import com.personal.financetracker.databinding.ItemCategoryBinding

class CategoryAdapter(
    private val onEdit: (Category) -> Unit,
    private val onDelete: (Category) -> Unit
) : ListAdapter<Category, CategoryAdapter.ViewHolder>(DIFF) {

    inner class ViewHolder(private val b: ItemCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(cat: Category) {
            b.tvEmoji.text = cat.emoji
            b.tvName.text = cat.name
            b.tvColor.text = cat.color

            val color = try { Color.parseColor(cat.color) } catch (e: Exception) { Color.GRAY }
            val bg = Color.argb(40, Color.red(color), Color.green(color), Color.blue(color))
            b.tvEmoji.backgroundTintList = android.content.res.ColorStateList.valueOf(bg)
            b.tvColor.setTextColor(color)

            b.btnEdit.setOnClickListener { onEdit(cat) }
            b.btnDelete.setOnClickListener { onDelete(cat) }
            b.btnDelete.alpha = if (cat.isDefault) 0.3f else 1.0f
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val b = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(b)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    companion object {
        val DIFF = object : DiffUtil.ItemCallback<Category>() {
            override fun areItemsTheSame(a: Category, b: Category) = a.id == b.id
            override fun areContentsTheSame(a: Category, b: Category) = a == b
        }
    }
}
