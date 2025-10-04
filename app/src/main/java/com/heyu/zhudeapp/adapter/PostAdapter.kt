package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.PostItemBinding
import com.heyu.zhudeapp.utils.DateUtils

// Define an interface for long click events.
// The Fragment will implement this to receive the event.
fun interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    // Add the listener to the adapter's constructor.
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
        // Set a long click listener on the item view.
        holder.itemView.setOnLongClickListener {
            // When a long click happens, call the listener's method.
            onItemLongClickListener.onItemLongClick(post)
            // Return true to indicate that the event has been consumed.
            true
        }
    }

    override fun getItemCount(): Int = posts.size

    /**
     * Updates the list of posts and notifies the adapter of the data change.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        posts = newPosts
        // This is the simplest way to refresh the list.
        // For better performance, DiffUtil could be used in a more complex app.
        notifyDataSetChanged()
    }

    class PostViewHolder(private val binding: PostItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: Post) {
            binding.postTextContent.text = post.content
            // You might want to format this timestamp nicely in the future
            binding.postTimestamp.text = DateUtils.formatTime(post.createdAt)

            if (post.imageUrls.isNotEmpty()) {
                binding.postImagesRecyclerView.visibility = View.VISIBLE

                val spanCount = when (post.imageUrls.size) {
                    1 -> 1
                    2, 4 -> 2
                    else -> 3
                }

                val layoutManager = GridLayoutManager(itemView.context, spanCount)
                val imagesAdapter = PostImagesAdapter(post.imageUrls)

                binding.postImagesRecyclerView.layoutManager = layoutManager
                binding.postImagesRecyclerView.adapter = imagesAdapter

                // Remove any existing decorations to prevent them from stacking up on recycled views.
                if (binding.postImagesRecyclerView.itemDecorationCount > 0) {
                    binding.postImagesRecyclerView.removeItemDecorationAt(0)
                }

                // Add the new spacing decoration.
                val spacing = itemView.context.resources.getDimensionPixelSize(R.dimen.grid_spacing)
                // 'includeEdge = true' will add spacing to the outside edges of the grid.
                val itemDecoration = GridSpacingItemDecoration(spanCount, spacing, includeEdge = true)
                binding.postImagesRecyclerView.addItemDecoration(itemDecoration)

            } else {
                binding.postImagesRecyclerView.visibility = View.GONE
            }
        }
    }
}
