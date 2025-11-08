package com.heyu.zhudeapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.activity.PostImagePagerActivity
import com.heyu.zhudeapp.databinding.ItemImageBinding

class PostImagesAdapter(
    private val imageUris: List<String>,
    private val onImageSaveListener: OnImageSaveListener
) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUris[position]
        // We need to pass the whole list of URIs to the ViewHolder to pass it to the activity
        holder.bind(imageUrl, imageUris, onImageSaveListener)
    }

    override fun getItemCount(): Int = imageUris.size

    class ImageViewHolder(private val binding: ItemImageBinding) : RecyclerView.ViewHolder(binding.root) {
        // The ViewHolder needs the full list of image URIs
        fun bind(imageUrl: String, imageUris: List<String>, listener: OnImageSaveListener) {
            val context: Context = binding.root.context
            Glide.with(context)
                .load(imageUrl)
                .centerCrop()
                .into(binding.imageItem)

            // Correctly implement the click listener to start PostImagePagerActivity
            binding.imageItem.setOnClickListener {
                val position = adapterPosition // Use the older, but reliable adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val intent = Intent(context, PostImagePagerActivity::class.java).apply {
                        putStringArrayListExtra("image_urls", ArrayList(imageUris))
                        putExtra("current_position", position)
                    }
                    context.startActivity(intent)
                }
            }

            // The setOnLongClickListener has been intentionally removed as per your request.
        }
    }
}
