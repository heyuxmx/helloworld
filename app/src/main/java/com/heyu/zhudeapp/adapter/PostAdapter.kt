package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.databinding.PostItemBinding
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.launch

interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val onItemLongClickListener: OnItemLongClickListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    // This set still tracks if a post has been liked at least once in this session,
    // to correctly manage the icon state (hollow vs. solid).
    private val likedPostIds = mutableSetOf<Long>()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = PostItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, lifecycleScope, likedPostIds)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post, onItemLongClickListener)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        private val binding: PostItemBinding,
        private val lifecycleScope: LifecycleCoroutineScope,
        private val likedPostIds: MutableSet<Long>
    ) : RecyclerView.ViewHolder(binding.root) {

        private val commentAdapter = CommentAdapter(mutableListOf())

        init {
            binding.commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = commentAdapter
            }
        }

        fun bind(
            post: Post,
            longClickListener: OnItemLongClickListener
        ) {
            binding.postContentText.text = post.content
            binding.likeCountText.text = post.likes.toString()
            binding.commentCountText.text = post.comments.size.toString()
            commentAdapter.updateComments(post.comments)

            // --- New Logic: Show solid heart if likes > 1 OR liked in this session ---

            // 1. Set the initial icon based on persisted likes or session likes.
            if (post.likes > 1 || likedPostIds.contains(post.id)) {
                binding.likeIcon.setImageResource(R.drawable.solidlike)
            } else {
                binding.likeIcon.setImageResource(R.drawable.hollowlike)
            }

            // 2. Set the click listener to increment likes on every click.
            binding.likeIcon.setOnClickListener {
                // Optimistically increment the like count on every click.
                val currentLikes = binding.likeCountText.text.toString().toLong()
                val newLikes = currentLikes + 1
                binding.likeCountText.text = newLikes.toString()

                // If this is the first time liking, or if the icon is currently hollow,
                // change the icon to solid and mark it for the session.
                if (!likedPostIds.contains(post.id) || binding.likeIcon.drawable.constantState == itemView.context.getDrawable(R.drawable.hollowlike)?.constantState) {
                    binding.likeIcon.setImageResource(R.drawable.solidlike)
                    likedPostIds.add(post.id)
                }

                // Call the server in the background to persist the change.
                lifecycleScope.launch {
                    try {
                        // This call should ideally tell the backend to increment the like count.
                        SupabaseModule.likePost(post.id)
                    } catch (e: Exception) {
                        // On failure, roll back the UI change for the like count.
                        // We don't roll back the icon change to avoid visual confusion.
                        binding.likeCountText.text = currentLikes.toString()
                        Toast.makeText(itemView.context, "点赞失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // --- End of the new logic ---

            if (post.imageUrls.isNotEmpty()) {
                val imageAdapter = ImageAdapter(post.imageUrls) { position ->
                    val intent = Intent(itemView.context, ImageViewerActivity::class.java).apply {
                        putStringArrayListExtra("image_urls", ArrayList(post.imageUrls))
                        putExtra("current_position", position)
                    }
                    itemView.context.startActivity(intent)
                }
                binding.imagesRecyclerView.apply {
                    layoutManager = GridLayoutManager(itemView.context, 3)
                    adapter = imageAdapter
                    visibility = View.VISIBLE
                }
            } else {
                binding.imagesRecyclerView.visibility = View.GONE
            }

            binding.commentInputGroup.visibility = View.GONE
            binding.commentInput.text.clear()

            binding.commentIcon.setOnClickListener {
                binding.commentInputGroup.isVisible = !binding.commentInputGroup.isVisible
            }

            binding.sendCommentButton.setOnClickListener {
                val commentText = binding.commentInput.text.toString().trim()
                if (commentText.isEmpty()) {
                    return@setOnClickListener
                }

                binding.sendCommentButton.isEnabled = false
                binding.commentInput.isEnabled = false

                lifecycleScope.launch {
                    try {
                        val savedComment = SupabaseModule.addComment(post.id, commentText)
                        post.comments.add(savedComment)
                        commentAdapter.addComment(savedComment)
                        binding.commentCountText.text = post.comments.size.toString()
                        binding.commentInput.text.clear()
                        binding.commentInputGroup.isVisible = false
                        binding.commentsRecyclerView.smoothScrollToPosition(commentAdapter.itemCount - 1)
                    } catch (e: Exception) {
                        Toast.makeText(itemView.context, "评论失败: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        binding.sendCommentButton.isEnabled = true
                        binding.commentInput.isEnabled = true
                    }
                }
            }

            itemView.setOnLongClickListener {
                longClickListener.onItemLongClick(post)
                true
            }
        }
    }
}
