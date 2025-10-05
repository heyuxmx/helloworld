package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.PostImageItemBinding

fun interface OnImageClickListener {
    fun onImageClick(position: Int)
}

class PostImagesAdapter(
    private val imageUris: List<String>,
    private val onImageClickListener: OnImageClickListener
) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

    class ImageViewHolder(val binding: PostImageItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = PostImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUri = imageUris[position]
        Glide.with(holder.itemView.context)
            .load(imageUri)
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .thumbnail(0.1f)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.postImageItemView)

        holder.itemView.setOnClickListener {
            onImageClickListener.onImageClick(position)
        }
    }

    override fun getItemCount(): Int = imageUris.size
}