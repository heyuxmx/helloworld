package com.heyu.zhudeapp.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.databinding.PostImageItemBinding

class PostImagesAdapter(private val imageUris: List<String>) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

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
            .load(imageUri) // Can now load both local and remote URIs
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .thumbnail(0.1f)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.postImageItemView)

        holder.binding.postImageItemView.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, ImageViewerActivity::class.java).apply {
                putExtra(ImageViewerActivity.IMAGE_URL, imageUri)
            }
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = imageUris.size
}
