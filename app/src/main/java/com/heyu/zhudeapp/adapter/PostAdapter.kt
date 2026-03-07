package com.heyu.zhudeapp.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.activity.PostImagePagerActivity
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.Post
import com.heyu.zhudeapp.di.HeyuModule
import com.heyu.zhudeapp.util.GridSpacingItemDecoration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoUnit

interface OnItemLongClickListener {
    fun onItemLongClick(post: Post)
}

interface OnImageSaveListener {
    fun onImageSave(imageUrl: String)
}

interface OnCommentLongClickListener {
    fun onCommentLongClick(post: Post, comment: Comment)
}

interface OnCommentInteractionListener {
    fun onCommentDraftClicked(post: Post)
}

class PostAdapter(
    private var posts: List<Post>,
    private var commentDrafts: Map<Long, String>,
    private val lifecycleScope: CoroutineScope,
    private val currentUserId: String,
    private val onItemLongClickListener: OnItemLongClickListener,
    private val onImageSaveListener: OnImageSaveListener,
    private val onCommentLongClickListener: OnCommentLongClickListener,
    private val onCommentInteractionListener: OnCommentInteractionListener
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private val likedPostIds = mutableSetOf<Long>()
    
    // Separate pools for different types of nested lists to avoid ViewType collisions
    private val imagesViewPool = RecyclerView.RecycledViewPool()
    private val commentsViewPool = RecyclerView.RecycledViewPool()

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
        // Apply theme-based card stroke
        val card = view as com.google.android.material.card.MaterialCardView
        val context = parent.context
        val ta = context.obtainStyledAttributes(intArrayOf(R.attr.postCardStrokeColor, R.attr.postCardStrokeWidth))
        val strokeColor = ta.getColor(0, 0)
        val strokeWidth = ta.getDimensionPixelSize(1, 0)
        ta.recycle()
        card.strokeColor = strokeColor
        card.strokeWidth = strokeWidth
        if (strokeWidth > 0) card.cardElevation = 0f
        return PostViewHolder(view, lifecycleScope, likedPostIds, currentUserId, onCommentInteractionListener, onImageSaveListener, imagesViewPool, commentsViewPool)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        val draft = commentDrafts[post.id] ?: ""
        holder.bind(post, draft, onItemLongClickListener, onCommentLongClickListener)
    }

    override fun getItemCount(): Int = posts.size

    class PostViewHolder(
        itemView: View,
        private val lifecycleScope: CoroutineScope,
        private val likedPostIds: MutableSet<Long>,
        private val currentUserId: String,
        private val commentInteractionListener: OnCommentInteractionListener,
        private val onImageSaveListener: OnImageSaveListener,
        private val imagesViewPool: RecyclerView.RecycledViewPool,
        private val commentsViewPool: RecyclerView.RecycledViewPool
    ) : RecyclerView.ViewHolder(itemView) {
        private val authorAvatar: ImageView = itemView.findViewById(R.id.author_avatar_image)
        private val authorUsername: TextView = itemView.findViewById(R.id.author_username_text)
        private val postContentText: TextView = itemView.findViewById(R.id.post_content_text)
        private val postTimestampText: TextView = itemView.findViewById(R.id.post_timestamp_text)
        private val mediaContainer: View = itemView.findViewById(R.id.media_container)
        private val imagesRecyclerView: RecyclerView = itemView.findViewById(R.id.images_recycler_view)
        private val postVideoThumbnail: ImageView = itemView.findViewById(R.id.post_video_thumbnail)
        private val postPlayPauseButton: ImageView = itemView.findViewById(R.id.post_play_pause_button)
        private val postVideoContainer: View = itemView.findViewById(R.id.post_video_container)
        private val likeIcon: ImageButton = itemView.findViewById(R.id.like_icon)
        private val likeCountText: TextView = itemView.findViewById(R.id.like_count_text)
        private val commentCountText: TextView = itemView.findViewById(R.id.comment_count_text)
        private val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        private val sendCommentButton: ImageButton = itemView.findViewById(R.id.send_comment_button)
        private val commentsRecyclerView: RecyclerView = itemView.findViewById(R.id.comments_recycler_view)

        init {
            imagesRecyclerView.apply {
                layoutManager = GridLayoutManager(itemView.context, 3)
                setRecycledViewPool(imagesViewPool)
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
                if (itemDecorationCount == 0) {
                    addItemDecoration(GridSpacingItemDecoration(3, 0, true))
                }
            }
            commentsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                setRecycledViewPool(commentsViewPool)
                setHasFixedSize(true)
                isNestedScrollingEnabled = false
            }
        }

        fun bind(
            post: Post,
            draft: String,
            longClickListener: OnItemLongClickListener,
            commentLongClickListener: OnCommentLongClickListener
        ) {
            commentInput.setOnClickListener(null)
            
            authorUsername.text = post.author?.username ?: "匿名用户"
            Glide.with(itemView.context)
                .load(post.author?.avatarUrl)
                .placeholder(R.drawable.hollowlike)
                .error(R.drawable.hollowlike)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .dontAnimate()
                .circleCrop()
                .into(authorAvatar)

            authorAvatar.setOnClickListener {
                post.author?.avatarUrl?.let { avatarUrl ->
                    if (avatarUrl.isNotBlank()) {
                        val intent = Intent(itemView.context, PostImagePagerActivity::class.java).apply {
                            putStringArrayListExtra("image_urls", arrayListOf(avatarUrl))
                            putExtra("current_position", 0)
                        }
                        itemView.context.startActivity(intent)
                    }
                }
            }

            postContentText.text = post.content
            postTimestampText.text = formatTimestamp(post.createdAt)
            likeCountText.text = post.likes.toString()
            commentCountText.text = post.comments.size.toString()

            commentInput.setText(draft.ifEmpty { "添加评论..." })
            commentInput.isFocusable = false
            commentInput.isFocusableInTouchMode = false
            commentInput.isClickable = true
            sendCommentButton.visibility = View.GONE
            commentInput.setOnClickListener {
                commentInteractionListener.onCommentDraftClicked(post)
            }

            val commentsAdapter = CommentAdapter(
                comments = post.comments.toMutableList(),
                onCommentLongClickListener = { comment -> commentLongClickListener.onCommentLongClick(post, comment) }
            )
            commentsRecyclerView.adapter = commentsAdapter

            val isTech = com.heyu.zhudeapp.util.ThemeManager.isTech(itemView.context)
            val likedRes = if (isTech) R.drawable.ic_like else R.drawable.solidlike
            val unlikedRes = if (isTech) R.drawable.ic_like_hollow else R.drawable.hollowlike
            if (post.likes > 1 || likedPostIds.contains(post.id)) {
                likeIcon.setImageResource(likedRes)
            } else {
                likeIcon.setImageResource(unlikedRes)
            }
            likeIcon.setOnClickListener {
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
                    likeIcon.setImageResource(likedRes)
                    likedPostIds.add(post.id)
                }
                lifecycleScope.launch {
                    try {
                        HeyuModule.likePost(post.id)
                    } catch (e: Exception) {
                        likeCountText.text = currentLikes.toString()
                        Toast.makeText(itemView.context, "点赞失败", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (post.imageUrls.isNotEmpty()) {
                mediaContainer.visibility = View.VISIBLE
                imagesRecyclerView.visibility = View.VISIBLE
                postVideoContainer.visibility = View.GONE
                
                val mediaAdapter = PostImagesAdapter(post.imageUrls, onImageSaveListener)
                imagesRecyclerView.adapter = mediaAdapter
            } else {
                mediaContainer.visibility = View.GONE
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

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
    } else {
        return timestamp
    }
}
