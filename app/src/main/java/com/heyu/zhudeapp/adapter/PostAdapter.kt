package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Listener for long-clicking the whole post item (e.g., for deletion)
interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    private val lifecycleScope: CoroutineScope,
    private val onItemLongClickListener: OnItemLongClickListener, // Existing listener for post deletion
    private val onImageSaveListener: OnImageSaveListener // New listener for image saving
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val likedPostIds = mutableSetOf<Long>()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view, lifecycleScope, likedPostIds)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        // Pass both listeners down to the ViewHolder
        holder.bind(post, onItemLongClickListener, onImageSaveListener)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val lifecycleScope: CoroutineScope,
        private val likedPostIds: MutableSet<Long>
    ) : RecyclerView.ViewHolder(itemView) {

        private val postContentText: TextView = itemView.findViewById(R.id.post_content_text)
        private val imagesRecyclerView: RecyclerView = itemView.findViewById(R.id.images_recycler_view)
        private val likeIcon: ImageButton = itemView.findViewById(R.id.like_icon)
        private val likeCountText: TextView = itemView.findViewById(R.id.like_count_text)
        private val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
        private val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        private val sendCommentButton: ImageButton = itemView.findViewById(R.id.send_comment_button)
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)

        private lateinit var commentsAdapter: CommentsAdapter

        fun bind(
            post: Post,
            longClickListener: OnItemLongClickListener, // Existing listener
            imageSaveListener: OnImageSaveListener // New listener
        ) {
            postContentText.text = post.content
            likeCountText.text = post.likes.toString()
            commentCountText.text = post.comments.size.toString()

            // Setup Comments Adapter - NO CHANGE
            commentsAdapter = CommentsAdapter(post.comments.toMutableList())
            commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = commentsAdapter
            }

            // Like Icon Logic - NO CHANGE
            if (post.likes > 1 || likedPostIds.contains(post.id)) {
                likeIcon.setImageResource(R.drawable.solidlike)
            } else {
                likeIcon.setImageResource(R.drawable.hollowlike)
            }
            likeIcon.setOnClickListener {
                val currentLikes = likeCountText.text.toString().toIntOrNull() ?: 0
                val newLikes = currentLikes + 1
                likeCountText.text = newLikes.toString()

                if (!likedPostIds.contains(post.id)) {
                    likeIcon.setImageResource(R.drawable.solidlike)
                    likedPostIds.add(post.id)
                }
                lifecycleScope.launch {
                    try {
                        SupabaseModule.likePost(post.id)
                    } catch (e: Exception) {
                        likeCountText.text = currentLikes.toString()
                        Toast.makeText(itemView.context, "点赞失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Image Adapter Logic - MODIFIED to pass the new listener
            if (post.imageUrls.isNotEmpty()) {
                val imageAdapter = PostImagesAdapter(
                    imageUris = post.imageUrls,
                    onImageClickListener = { position ->
                         val intent = Intent(itemView.context, ImageViewerActivity::class.java).apply {
                            putStringArrayListExtra("image_urls", ArrayList(post.imageUrls))
                            putExtra("current_position", position)
                        }
                        itemView.context.startActivity(intent)
                    },
                    onImageSaveListener = imageSaveListener // Pass the listener down
                )
                imagesRecyclerView.apply {
                    layoutManager = GridLayoutManager(itemView.context, 3)
                    adapter = imageAdapter
                    visibility = View.VISIBLE
                }
            } else {
                imagesRecyclerView.visibility = View.GONE
            }

            // Send Comment Logic - NO CHANGE
            sendCommentButton.setOnClickListener {
                val commentText = commentInput.text.toString().trim()
                if (commentText.isEmpty()) {
                    return@setOnClickListener
                }
                sendCommentButton.isEnabled = false
                commentInput.isEnabled = false

                lifecycleScope.launch {
                    try {
                        val newComment = SupabaseModule.addComment(post.id, commentText)
                        withContext(Dispatchers.Main) {
                            post.comments.add(newComment)
                            commentsAdapter.addComment(newComment)
                            commentCountText.text = post.comments.size.toString()
                            commentInput.text.clear()
                            commentsRecyclerView.smoothScrollToPosition(commentsAdapter.itemCount - 1)
                        }
                    } catch (e: Exception) {
                         withContext(Dispatchers.Main) {
                            Toast.makeText(itemView.context, "评论失败: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                           sendCommentButton.isEnabled = true
                           commentInput.isEnabled = true
                        }
                    }
                }
            }

            // Restore the long-click listener for the entire post item
            itemView.setOnLongClickListener {
                longClickListener.onItemLongClick(post)
                false // IMPORTANT: Return false to allow the event to propagate to children (like the images)
            }
        }
    }
}
