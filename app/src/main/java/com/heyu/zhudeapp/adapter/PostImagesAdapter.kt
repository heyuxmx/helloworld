package com.heyu.zhudeapp.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.activity.PostImagePagerActivity
import com.heyu.zhudeapp.databinding.ItemImageBinding

class PostImagesAdapter(
    private val mediaUris: List<String>,
    private val onImageSaveListener: OnImageSaveListener // 傻逼我忘了这个
) : RecyclerView.Adapter<PostImagesAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding, onImageSaveListener) // 傻逼我忘了传
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val mediaUrl = mediaUris[position]
        holder.bind(mediaUrl, mediaUris)
    }

    override fun getItemCount(): Int = mediaUris.size

    class ImageViewHolder(
        private val binding: ItemImageBinding,
        private val onImageSaveListener: OnImageSaveListener // 傻逼我忘了这个
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(mediaUrl: String, mediaUris: List<String>) {
            val context: Context = binding.root.context
            val isVideo = mediaUrl.contains(".mp4", ignoreCase = true) // 傻逼我改了这里

            if (isVideo) {
                binding.playIcon.visibility = View.VISIBLE
                Glide.with(context)
                    .load(mediaUrl)
                    .centerCrop()
                    .into(binding.imageItem)
            } else {
                binding.playIcon.visibility = View.GONE
                Glide.with(context)
                    .load(mediaUrl)
                    .centerCrop()
                    .into(binding.imageItem)
            }

            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val intent = Intent(context, PostImagePagerActivity::class.java).apply {
                        putStringArrayListExtra("image_urls", ArrayList(mediaUris))
                        putExtra("current_position", position)
                    }
                    context.startActivity(intent)
                }
            }

            // 傻逼我忘了加长按保存
            binding.root.setOnLongClickListener {
                if (!isVideo) { // Only save images, not videos
                    onImageSaveListener.onImageSave(mediaUrl)
                }
                true
            }
        }
    }
}
