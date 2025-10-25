
package com.heyu.zhudeapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.PostImageItemBinding

// Listener for single-clicking an image (no change)
fun interface OnImageClickListener {
    fun onImageClick(position: Int)
}

// A new, clean interface to communicate the save action to the Fragment
fun interface OnImageSaveListener {
    fun onSaveImage(imageUrl: String?)
}

class PostImagesAdapter(
    private val imageUris: List<String>,
    private val onImageClickListener: OnImageClickListener,
    private val onImageSaveListener: OnImageSaveListener // The listener is now passed in constructor
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
        val context = holder.itemView.context

        Glide.with(context)
            .load(imageUri)
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .into(holder.binding.postImageItemView)

        // Handle single click to view image
        holder.binding.postImageItemView.setOnClickListener {
            onImageClickListener.onImageClick(position)
        }

        // Handle long click with a standard AlertDialog
        holder.binding.postImageItemView.setOnLongClickListener {
            MaterialAlertDialogBuilder(context)
                .setMessage("要保存这张图片吗？")
                .setNegativeButton("取消", null) // Just dismisses the dialog
                .setPositiveButton("确定") { _, _ ->
                    onImageSaveListener.onSaveImage(imageUri)
                }
                .show()
            true // Consume the long-click event
        }
    }

    override fun getItemCount(): Int = imageUris.size
}
