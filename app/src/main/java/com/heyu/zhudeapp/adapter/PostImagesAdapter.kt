package com.heyu.zhudeapp.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
        private val progressBar: ProgressBar = view.findViewById(R.id.video_progress_bar)
        private val playButton: ImageButton = view.findViewById(R.id.play_button)

        fun bind(mediaUrl: String, mediaUris: List<String>, position: Int) {
            val context: Context = view.context

            // Set up the video thumbnail
            val videoUri = Uri.parse(mediaUrl)
            
            // In PostFragment, we only show the thumbnail and play button
            // The actual video will play in PostImagePagerActivity
            playButton.visibility = View.VISIBLE
            
            // Load video thumbnail as static image
            Glide.with(context)
                .asBitmap()
                .load(videoUri)
                .centerCrop()
                .error(R.drawable.image_placeholder) // Fallback image if loading fails
                .into(videoView)
            
            // Open in pager activity on click
            val openPagerActivity = {
                val intent = Intent(context, PostImagePagerActivity::class.java).apply {
                    putStringArrayListExtra("image_urls", ArrayList(mediaUris))
                    putExtra("current_position", position)
                }
                context.startActivity(intent)
            }
            
            view.setOnClickListener { openPagerActivity() }
            
            // Also allow clicking the play button to open the pager activity
            playButton.setOnClickListener { openPagerActivity() }

            // Long press to save video
            view.setOnLongClickListener {
                onImageSaveListener.onImageSave(mediaUrl)
                true
            }
        }
    }
}