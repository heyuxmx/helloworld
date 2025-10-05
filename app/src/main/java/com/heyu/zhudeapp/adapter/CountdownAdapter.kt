package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.databinding.ListItemCountdownBinding
import com.heyu.zhudeapp.model.CountdownItem

class CountdownAdapter(private var items: MutableList<CountdownItem>) : RecyclerView.Adapter<CountdownAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ListItemCountdownBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: CountdownItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun updateList(newItems: List<CountdownItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    class ViewHolder(private val binding: ListItemCountdownBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CountdownItem) {
            binding.eventNameText.text = item.name

            // The Fragment is now responsible for providing the correct display date string
            binding.eventDateText.text = item.dateOverride

            // The Fragment is now responsible for pre-calculating the days remaining
            binding.daysRemainingText.text = item.daysRemaining.toString()
        }
    }
}
