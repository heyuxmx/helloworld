package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.ItemImageBinding

class ProfileAdapter(
    private val imageUrls: List<String>,
    private val onImageClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ProfileAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageUrls[position], position, onImageClick)
    }

    override fun getItemCount(): Int = imageUrls.size

    class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(imageUrl: String, position: Int, onImageClick: (position: Int) -> Unit) {
            Glide.with(itemView.context)
                .load(imageUrl)
                .into(binding.imageItem)
            
            itemView.setOnClickListener {
                onImageClick(position)
            }
        }
    }
}
