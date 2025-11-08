package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
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

    fun getPostIndex(postId: String): Int {
        return posts.indexOfFirst { it.id.toString() == postId }
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
                    // First, fetch the current user's profile using the new helper function
                    val currentUserProfile = SupabaseModule.getUserProfile(currentUserId)

                    if (currentUserProfile == null) {
                        // If the profile can't be fetched, show an error and don't proceed
                        withContext(Dispatchers.Main) {
                            Toast.makeText(itemView.context, "无法获取用户信息，请稍后重试", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // Proceed to add the comment to the database
                    val newCommentIncomplete = SupabaseModule.addComment(post.id, commentText, currentUserId)

                    // The returned comment object might not have the author's profile.
                    // We create a complete object by attaching the profile we just fetched.
                    val newCommentComplete = newCommentIncomplete.copy(author = currentUserProfile)

                    // Update the UI on the main thread with the complete comment object
                    withContext(Dispatchers.Main) {
                        commentsAdapter.addComment(newCommentComplete)
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
                comments = post.comments.toMutableList(),
                onCommentLongClickListener = {
                    comment -> commentLongClickListener.onCommentLongClick(post,comment)
                }
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
                // Add vibration feedback
                val vibrator = itemView.context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (vibrator.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // For API 26+
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        // Deprecated in API 26
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(50)
                    }
                }

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
                        val context = itemView.context
                        val intent = Intent(context, ImageViewerActivity::class.java).apply {
                            putStringArrayListExtra("image_urls", ArrayList(post.imageUrls))
                            putExtra("current_position", position)
                        }
                        context.startActivity(intent)
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
        // Try parsing with optional milliseconds and timezone
        val formatters = listOf(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME to true,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX") to true,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS") to true,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX") to true,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss") to true,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME to true,
            DateTimeFormatter.ISO_LOCAL_DATE to false
        )

        for ((formatter, hasTime) in formatters) {
            try {
                return if (hasTime) {
                    val zdt = ZonedDateTime.parse(ts, formatter.withZone(zone))
                    ParseResult(zdt, true)
                } else {
                    val ld = LocalDate.parse(ts, formatter)
                    ParseResult(ld.atStartOfDay(zone), false)
                }
            } catch (e: DateTimeParseException) {
                // Continue to next formatter
            }
        }
        // Fallback for timestamps that might not have a 'T'
        try {
            val ldt = LocalDateTime.parse(ts.replace(" ", "T"))
            return ParseResult(ldt.atZone(zone), true)
        } catch (e: DateTimeParseException) {
            // Final fallback failed
        }
        return null
    }


    val now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"))
    val parsedResult = parseTimestamp(timestamp) ?: return timestamp

    val postTime = parsedResult.zonedDateTime
    if (!parsedResult.hasTimeInformation) {
         return postTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }


    val minutesDiff = ChronoUnit.MINUTES.between(postTime, now)
    val hoursDiff = ChronoUnit.HOURS.between(postTime, now)
    val daysDiff = ChronoUnit.DAYS.between(postTime.toLocalDate(), now.toLocalDate())

    return when {
        minutesDiff < 1 -> "刚刚"
        minutesDiff < 60 -> "${minutesDiff}分钟前"
        hoursDiff < 24 && now.dayOfMonth == postTime.dayOfMonth -> "${hoursDiff}小时前"
        daysDiff == 1L -> "昨天 " + postTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        now.year == postTime.year -> postTime.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
        else -> postTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    }
}
