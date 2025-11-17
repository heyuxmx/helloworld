package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
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

// Listener for saving an image on long-click
interface OnImageSaveListener {
    fun onImageSave(imageUrl: String)
}

// Listener for long-clicking a comment (for deletion)
interface OnCommentLongClickListener {
    fun onCommentLongClick(post: Post, comment: Comment)
}

// REFACTORED: Simplified listener. Its only job is to signal the fragment to open the focus view.
interface OnCommentInteractionListener {
    fun onCommentDraftClicked(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    private var commentDrafts: Map<Long, String>,
    private val lifecycleScope: CoroutineScope,
    private val currentUserId: String, // The ID of the currently logged-in user
    private val onItemLongClickListener: OnItemLongClickListener,
    private val onImageSaveListener: OnImageSaveListener,
    private val onCommentLongClickListener: OnCommentLongClickListener,
    private val onCommentInteractionListener: OnCommentInteractionListener // REFACTORED
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val likedPostIds = mutableSetOf<Long>()

    @SuppressLint("NotifyDataSetChanged")
    fun updatePostsAndDrafts(newPosts: List<Post>, newDrafts: Map<Long, String>) {
        this.posts = newPosts
        this.commentDrafts = newDrafts
        notifyDataSetChanged()
    }

    fun getPostIndex(postId: String): Int {
        return posts.indexOfFirst { it.id.toString() == postId }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_post, parent, false)
        return PostViewHolder(view, lifecycleScope, likedPostIds, currentUserId, onCommentInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val draft = commentDrafts[post.id] ?: ""
        holder.bind(post, draft, onItemLongClickListener, onImageSaveListener, onCommentLongClickListener)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val lifecycleScope: CoroutineScope,
        private val likedPostIds: MutableSet<Long>,
        private val currentUserId: String,
        private val commentInteractionListener: OnCommentInteractionListener // REFACTORED
    ) : RecyclerView.ViewHolder(itemView) {
        private val authorAvatar: ImageView = itemView.findViewById(R.id.author_avatar_image)
        private val authorUsername: TextView = itemView.findViewById(R.id.author_username_text)
        private val postContentText: TextView = itemView.findViewById(R.id.post_content_text)
        private val postTimestampText: TextView = itemView.findViewById(R.id.post_timestamp_text)
        private val imagesRecyclerView: RecyclerView = itemView.findViewById(R.id.images_recycler_view)
        private val likeIcon: ImageButton = itemView.findViewById(R.id.like_icon)
        private val likeCountText: TextView = itemView.findViewById(R.id.like_count_text)
        private val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
        private val commentInput: EditText = itemView.findViewById(R.id.comment_input) // Will be treated as a button
        private val sendCommentButton: ImageButton = itemView.findViewById(R.id.send_comment_button) // Will be hidden
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)

        // TextWatcher is no longer needed here.
        private var textWatcher: TextWatcher? = null

        private lateinit var commentsAdapter: CommentAdapter

        fun bind(
            post: Post,
            draft: String,
            longClickListener: OnItemLongClickListener,
            imageSaveListener: OnImageSaveListener,
            commentLongClickListener: OnCommentLongClickListener
        ) {
            // Unbind previous listeners to prevent conflicts
            commentInput.removeTextChangedListener(textWatcher)
            textWatcher = null
            commentInput.setOnClickListener(null)
            sendCommentButton.setOnClickListener(null)

            // Bind author information
            authorUsername.text = post.author?.username ?: "匿名用户"
            Glide.with(itemView.context)
                .load(post.author?.avatarUrl)
                .placeholder(R.drawable.hollowlike)
                .error(R.drawable.hollowlike)
                .circleCrop()
                .into(authorAvatar)

            postContentText.text = post.content
            postTimestampText.text = formatTimestamp(post.createdAt)
            likeCountText.text = post.likes.toString()
            commentCountText.text = post.comments.size.toString()

            // --- REFACTORED COMMENT INTERACTION LOGIC ---

            // 1. Visually treat the comment box as a read-only button/display area.
            commentInput.setText(draft.ifEmpty { "添加评论..." })
            commentInput.isFocusable = false
            commentInput.isFocusableInTouchMode = false
            commentInput.isClickable = true

            // 2. Hide the separate send button, as all sending happens in the focus view.
            sendCommentButton.visibility = View.GONE

            // 3. The only interaction is to click the entire box to open the focus view.
            commentInput.setOnClickListener {
                commentInteractionListener.onCommentDraftClicked(post)
            }
            
            // --- END REFACTORED LOGIC ---

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
                        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
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
        try {
            val ldt = LocalDateTime.parse(ts.replace(" ", "T"))
            return ParseResult(ldt.atZone(zone), true)
        } catch (e: DateTimeParseException) {
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
