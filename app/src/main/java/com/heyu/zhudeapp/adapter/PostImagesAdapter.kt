package com.heyu.zhudeapp.adapter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.PostImagePagerActivity

class PostImagesAdapter(
    private val mediaUris: List<String>,
    private val onImageSaveListener: OnImageSaveListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (mediaUris[position].contains(".mp4", ignoreCase = true)) {
            TYPE_VIDEO
        } else {
            TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_VIDEO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_video, parent, false)
                VideoViewHolder(view, onImageSaveListener)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.list_item_image, parent, false)
                ImageViewHolder(view, onImageSaveListener)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaUrl = mediaUris[position]
        when (holder) {
            is ImageViewHolder -> holder.bind(mediaUrl, mediaUris, position)
            is VideoViewHolder -> holder.bind(mediaUrl, mediaUris, position)
        }
    }

    override fun getItemCount(): Int = mediaUris.size

    class ImageViewHolder(
        private val view: View,
        private val onImageSaveListener: OnImageSaveListener
    ) : RecyclerView.ViewHolder(view) {

        private val imageView: ImageView = view.findViewById(R.id.image_view_item)

        fun bind(mediaUrl: String, mediaUris: List<String>, position: Int) {
            val context: Context = view.context

            Glide.with(context)
                .load(mediaUrl)
                .centerCrop()
                .placeholder(R.color.grey_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .thumbnail(0.1f)
                .into(imageView)

            view.setOnClickListener {
                val intent = Intent(context, PostImagePagerActivity::class.java).apply {
                    putStringArrayListExtra("image_urls", ArrayList(mediaUris))
                    putExtra("current_position", position)
                }
                context.startActivity(intent)
            }

            view.setOnLongClickListener {
                onImageSaveListener.onImageSave(mediaUrl)
                true
            }
        }
    }

    class VideoViewHolder(
        private val view: View,
        private val onImageSaveListener: OnImageSaveListener
    ) : RecyclerView.ViewHolder(view) {

        private val videoView: ImageView = view.findViewById(R.id.video_view_item)
        private val playButton: ImageView = view.findViewById(R.id.play_button)

        fun bind(mediaUrl: String, mediaUris: List<String>, position: Int) {
            val context: Context = view.context

            // 服务器上传视频时会同步生成 _thumb.jpg，用它作封面
            // 若 _thumb.jpg 不存在（如旧 Supabase 视频），回退到 Glide 直接解析视频帧
            val thumbUrl = mediaUrl.replace(Regex("\\.mp4$", RegexOption.IGNORE_CASE), "_thumb.jpg")
            val fallback = Glide.with(context)
                .load(Uri.parse(mediaUrl))
                .centerCrop()
                .placeholder(R.color.grey_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)

            Glide.with(context)
                .load(thumbUrl)
                .centerCrop()
                .placeholder(R.color.grey_placeholder)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error(fallback)
                .into(videoView)
            
            val openPagerActivity = {
                val intent = Intent(context, PostImagePagerActivity::class.java).apply {
                    putStringArrayListExtra("image_urls", ArrayList(mediaUris))
                    putExtra("current_position", position)
                }
                context.startActivity(intent)
            }
            
            view.setOnClickListener { openPagerActivity() }
            playButton.setOnClickListener { openPagerActivity() }
            view.setOnLongClickListener {
                onImageSaveListener.onImageSave(mediaUrl)
                true
            }
        }
    }
}
