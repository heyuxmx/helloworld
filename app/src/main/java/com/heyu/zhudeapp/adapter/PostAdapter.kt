package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.PostItemBinding

// Change 'val' to 'var' to make the list updatable
class PostAdapter(private var posts: List<Post>) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
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
            binding.postTimestamp.text = post.createdAt

            if (post.imageUrls.isNotEmpty()) {
                binding.postImagesRecyclerView.visibility = View.VISIBLE

                // Determine the number of columns for the grid dynamically.
                // - 1 image: 1 column (large image)
                // - 2 or 4 images: 2 columns (2x1 or 2x2 grid)
                // - 3 or 5+ images: 3 columns (standard grid)
                val spanCount = when (post.imageUrls.size) {
                    1 -> 1
                    2, 4 -> 2
                    else -> 3
                }

                // Set up the GridLayoutManager and the adapter for the nested RecyclerView.
                val layoutManager = GridLayoutManager(itemView.context, spanCount)
                val imagesAdapter = PostImagesAdapter(post.imageUrls)

                binding.postImagesRecyclerView.layoutManager = layoutManager
                binding.postImagesRecyclerView.adapter = imagesAdapter

            } else {
                // If there are no images, hide the RecyclerView.
                binding.postImagesRecyclerView.visibility = View.GONE
            }
        }
    }
}
