package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.PostItemBinding
import com.heyu.zhudeapp.utils.DateUtils

fun interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
        holder.itemView.setOnLongClickListener {
            onItemLongClickListener.onItemLongClick(post)
            true
        }
    }

    override fun getItemCount(): Int = posts.size

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        notifyDataSetChanged()
    }

    class PostViewHolder(private val binding: PostItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.postTextContent.text = post.content
            binding.postTimestamp.text = DateUtils.formatTime(post.createdAt)

            // Handle the optimistic UI states
            when {
                post.isUploading -> {
                    binding.uploadingOverlay.visibility = View.VISIBLE
                    binding.uploadingText.visibility = View.VISIBLE
                    binding.retryButton.visibility = View.GONE
                }
                post.uploadFailed -> {
                    binding.uploadingOverlay.visibility = View.VISIBLE
                    binding.uploadingText.text = "发布失败"
                    binding.retryButton.visibility = View.VISIBLE
                }
                else -> {
                    binding.uploadingOverlay.visibility = View.GONE
                }
            }

            val imageUris = if (post.isUploading) {
                post.localImageUris.map { it.toUri().toString() }
            } else {
                post.imageUrls
            }

            if (imageUris.isNotEmpty()) {
                binding.postImagesRecyclerView.visibility = View.VISIBLE

                val spanCount = when (imageUris.size) {
                    1 -> 1
                    2, 4 -> 2
                    else -> 3
                }

                val layoutManager = GridLayoutManager(itemView.context, spanCount)
                
                // Create the click listener for the images adapter
                val onImageClickListener = OnImageClickListener { clickedPosition ->
                    val context = itemView.context
                    // When an image is clicked, start the ImageViewerActivity with all images
                    val intent = ImageViewerActivity.newIntent(
                        context = context,
                        imageUrls = ArrayList(imageUris), // Pass the full list of images
                        currentPosition = clickedPosition // Pass the position of the clicked image
                    )
                    context.startActivity(intent)
                }

                val imagesAdapter = PostImagesAdapter(imageUris, onImageClickListener)

                binding.postImagesRecyclerView.layoutManager = layoutManager
                binding.postImagesRecyclerView.adapter = imagesAdapter

                if (binding.postImagesRecyclerView.itemDecorationCount > 0) {
                    binding.postImagesRecyclerView.removeItemDecorationAt(0)
                }

                val spacing = itemView.context.resources.getDimensionPixelSize(R.dimen.grid_spacing)
                val itemDecoration = GridSpacingItemDecoration(spanCount, spacing, includeEdge = true)
                binding.postImagesRecyclerView.addItemDecoration(itemDecoration)

            } else {
                binding.postImagesRecyclerView.visibility = View.GONE
            }
        }
    }
}
