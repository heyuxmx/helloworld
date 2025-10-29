package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.ImageViewerActivity
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.SupabaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

// Listener for long-clicking the whole post item (e.g., for deletion)
interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

// Listener for long-clicking a comment (for deletion)
interface OnCommentLongClickListener {
    fun onCommentLongClick(post: Post, comment: Comment)
}

class PostAdapter(
    private var posts: List<Post>,
    private val lifecycleScope: CoroutineScope,
    private val currentUserId: String, // The ID of the currently logged-in user
    private val onItemLongClickListener: OnItemLongClickListener, // Existing listener for post deletion
    private val onImageSaveListener: OnImageSaveListener, // Listener for image saving
    private val onCommentLongClickListener: OnCommentLongClickListener // Listener for comment deletion
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val likedPostIds = mutableSetOf<Long>()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePosts(newPosts: List<Post>) {
        this.posts = newPosts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_post, parent, false)
        // Pass the currentUserId to the ViewHolder
        return PostViewHolder(view, lifecycleScope, likedPostIds, currentUserId)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        // Pass all listeners down to the ViewHolder
        holder.bind(post, onItemLongClickListener, onImageSaveListener, onCommentLongClickListener)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val lifecycleScope: CoroutineScope,
        private val likedPostIds: MutableSet<Long>,
        private val currentUserId: String // Receive the current user's ID
    ) : RecyclerView.ViewHolder(itemView) {
        private val authorAvatar: ImageView = itemView.findViewById(R.id.author_avatar_image)
        private val authorUsername: TextView = itemView.findViewById(R.id.author_username_text)
        private val postContentText: TextView = itemView.findViewById(R.id.post_content_text)
        private val postTimestampText: TextView = itemView.findViewById(R.id.post_timestamp_text)
        private val imagesRecyclerView: RecyclerView = itemView.findViewById(R.id.images_recycler_view)
        private val likeIcon: ImageButton = itemView.findViewById(R.id.like_icon)
        private val likeCountText: TextView = itemView.findViewById(R.id.like_count_text)
        private val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
        private val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        private val sendCommentButton: ImageButton = itemView.findViewById(R.id.send_comment_button)
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)

        private lateinit var commentsAdapter: CommentAdapter
        private fun sendCommentAction(post: Post) {
            val commentText = commentInput.text.toString().trim()
            if (commentText.isEmpty()) {
                return
            }
            sendCommentButton.isEnabled = false
            commentInput.isEnabled = false

            lifecycleScope.launch {
                try {
                    // Use the currentUserId passed into the ViewHolder
                    val newComment = SupabaseModule.addComment(post.id, commentText, currentUserId)
                    withContext(Dispatchers.Main) {
                        (post.comments as? MutableList)?.add(newComment)
                        commentsAdapter.addComment(newComment)
                        commentCountText.text = commentsAdapter.itemCount.toString()
                        commentInput.text.clear()
                        commentsRecyclerView.smoothScrollToPosition(commentsAdapter.itemCount - 1)
                        val imm = itemView.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(commentInput.windowToken, 0)
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
        fun bind(
            post: Post,
            longClickListener: OnItemLongClickListener, // Existing listener
            imageSaveListener: OnImageSaveListener, // New listener
            commentLongClickListener: OnCommentLongClickListener // Listener for comments
        ) {
            // Bind author information
            authorUsername.text = post.author?.username ?: "匿名用户"
            Glide.with(itemView.context)
                .load(post.author?.avatarUrl)
                .placeholder(R.drawable.hollowlike) // Using an existing drawable as a temporary placeholder
                .error(R.drawable.hollowlike) // Using an existing drawable as a temporary error fallback
                .circleCrop()
                .into(authorAvatar)

            postContentText.text = post.content
            postTimestampText.text = formatTimestamp(post.createdAt)
            likeCountText.text = post.likes.toString()
            commentCountText.text = post.comments.size.toString()

            // Setup Comments Adapter
            commentsAdapter = CommentAdapter(
                comments = post.comments.toMutableList()
            )
            commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = commentsAdapter
            }

            // Like Icon Logic
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

            // Image Adapter Logic
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

            // Send Comment Logic
            sendCommentButton.setOnClickListener {
                sendCommentAction(post)
            }

            commentInput.setOnEditorActionListener { _, actionId, event ->
                val isActionSend = actionId == EditorInfo.IME_ACTION_SEND
                val isActionDone = actionId == EditorInfo.IME_ACTION_DONE
                val isEnterPress = event?.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER

                if (isActionSend || isActionDone || isEnterPress) {
                    sendCommentAction(post)
                    true // Consume the event
                } else {
                    false // Do not consume the event
                }
            }

            // Restore the long-click listener for the entire post item
            itemView.setOnLongClickListener {
                longClickListener.onItemLongClick(post)
                true 
            }
        }
    }
}

private fun formatTimestamp(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""

    data class ParseResult(val zonedDateTime: ZonedDateTime, val hasTimeInformation: Boolean)

    fun parseTimestamp(ts: String): ParseResult? {
        val zone = ZoneId.of("Asia/Shanghai")
        val normalizedTs = ts.replace(" ", "T")

        // 1. Try common ISO formats first
        try {
            val zdt = ZonedDateTime.parse(normalizedTs, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            return ParseResult(zdt.withZoneSameInstant(zone), true)
        } catch (e: DateTimeParseException) { /* Continue */ }

        try {
            val ldt = LocalDateTime.parse(normalizedTs)
            return ParseResult(ldt.atZone(zone), true)
        } catch (e: DateTimeParseException) { /* Continue */ }
        
        // 2. Try custom format from Supabase/Postgres
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[.SSSSSS]")
            val ldt = LocalDateTime.parse(normalizedTs, formatter)
            return ParseResult(ldt.atZone(zone), true)
        } catch (e: DateTimeParseException) { /* Continue */ }


        // 3. Finally, try as just a date
        try {
            val ld = LocalDate.parse(normalizedTs)
            return ParseResult(ld.atStartOfDay(zone), false)
        } catch (e: DateTimeParseException) { /* Continue */ }

        return null // All parsing failed
    }

    val parseResult = parseTimestamp(timestamp) ?: return timestamp

    val zonedDateTime = parseResult.zonedDateTime
    val shouldShowTime = parseResult.hasTimeInformation
    val now = ZonedDateTime.now(zonedDateTime.zone)
    val postDate = zonedDateTime.toLocalDate()
    val nowDate = now.toLocalDate()

    val daysAgo = ChronoUnit.DAYS.between(postDate, nowDate)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    return when (daysAgo) {
        0L -> if (shouldShowTime) timeFormatter.format(zonedDateTime) else "今天"
        1L -> if (shouldShowTime) "昨天 ${timeFormatter.format(zonedDateTime)}" else "昨天"
        in 2..3 -> if (shouldShowTime) "${daysAgo}天前 ${timeFormatter.format(zonedDateTime)}" else "${daysAgo}天前"
        else -> {
            val pattern = if (postDate.year == nowDate.year) {
                if (shouldShowTime) "MM-dd HH:mm" else "MM-dd"
            } else {
                if (shouldShowTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
            }
            DateTimeFormatter.ofPattern(pattern).format(zonedDateTime)
        }
    }
}
