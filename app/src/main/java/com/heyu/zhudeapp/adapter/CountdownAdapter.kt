package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.databinding.ListItemCountdownBinding
import com.heyu.zhudeapp.model.CountdownItem
import java.time.format.DateTimeFormatter

/**
 * A DiffUtil.Callback to calculate the difference between two lists of CountdownItems.
 * This allows for efficient updates and nice animations.
 */
class CountdownDiffCallback(
    private val oldList: List<CountdownItem>,
    private val newList: List<CountdownItem>
) : DiffUtil.Callback() {

    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Items are considered the same if they have the same unique name.
        return oldList[oldItemPosition].name == newList[newItemPosition].name
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        // Contents are the same if the data class's equals method returns true.
        // This checks all properties like daysRemaining, nextDate, etc.
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

class CountdownAdapter(private var items: MutableList<CountdownItem>) : RecyclerView.Adapter<CountdownAdapter.ViewHolder>() {

    var onItemLongClickListener: ((CountdownItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = ViewHolder(binding)

        holder.itemView.setOnLongClickListener {
            // Use adapterPosition for a reliable position.
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = items[position]
                if (item.isDeletable) {
                    onItemLongClickListener?.invoke(item)
                }
            }
            true // Consume the long click event.
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    /**
     * Updates the list with new items using DiffUtil to calculate the differences.
     * This provides better performance and enables animations.
     */
    fun updateList(newItems: List<CountdownItem>) {
        val diffCallback = CountdownDiffCallback(this.items, newItems)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.items.clear()
        this.items.addAll(newItems)
        diffResult.dispatchUpdatesTo(this) // Dispatches updates to the adapter and triggers animations.
    }

    class ViewHolder(private val binding: ListItemCountdownBinding) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        fun bind(item: CountdownItem) {
            binding.eventNameText.text = item.name
            binding.daysRemainingText.text = item.daysRemaining.toString()

            if (item.nextDate != null) {
                binding.eventDateText.text = item.nextDate!!.format(dateFormatter)
            } else {
                binding.eventDateText.text = item.dateOverride ?: ""
            }
        }
    }
}
