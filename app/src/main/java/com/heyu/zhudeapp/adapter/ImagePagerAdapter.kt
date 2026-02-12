package com.heyu.zhudeapp.adapter

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AlertDialog
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ItemImagePagerBinding
import com.heyu.zhudeapp.databinding.PagerItemVideoBinding
import com.heyu.zhudeapp.util.VideoCacheManager

@OptIn(UnstableApi::class)
class ImagePagerAdapter(private val mediaUrls: List<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_VIDEO = 2
    }

    class ImagePagerViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)

    class VideoPagerViewHolder(val binding: PagerItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        private var player: ExoPlayer? = null
        private var isDragging = false
        private var isSeeking = false
        private var lastManualSeekTime = 0L
        private var videoUrl: String? = null

        fun getIsDragging(): Boolean = isDragging

        fun playVideo() {
            player?.play()
            binding.playPauseButton.setImageResource(R.drawable.ic_videostop)
            binding.playPauseButton.tag = "playing"
        }

        fun pauseVideo() {
            player?.pause()
            binding.playPauseButton.setImageResource(R.drawable.ic_videoplay)
            binding.playPauseButton.tag = "paused"
        }

        fun releasePlayer() {
            player?.release()
            player = null
        }

        fun bind(url: String) {
            this.videoUrl = url
            if (player == null) {
                val context = itemView.context
                player = ExoPlayer.Builder(context).build().apply {
                    repeatMode = Player.REPEAT_MODE_ONE
                    
                    val dataSourceFactory = VideoCacheManager.getCacheDataSourceFactory(context)
                    val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                        .createMediaSource(MediaItem.fromUri(Uri.parse(url)))
                    
                    setMediaSource(mediaSource)
                    prepare()
                    
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            when (playbackState) {
                                Player.STATE_BUFFERING -> binding.videoProgressBar.visibility = View.VISIBLE
                                Player.STATE_READY -> {
                                    binding.videoProgressBar.visibility = View.GONE
                                    updateVideoInfo()
                                    if (binding.playPauseButton.tag == "playing") play()
                                }
                                else -> {}
                            }
                        }
                    })
                }
                binding.playerView.player = player
            }
            
            initializeVideoControls()
            startUpdatingSeekBar()
        }

        private fun initializeVideoControls() {
            binding.playPauseButton.setOnClickListener { togglePlayPause() }
            
            val touchSlop = ViewConfiguration.get(binding.root.context).scaledTouchSlop
            var initialPosition = 0L
            var initialTouchX = 0f
            var targetPosition = 0L
            
            binding.playerView.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.x
                        isDragging = false
                        false
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - initialTouchX
                        if (!isDragging && kotlin.math.abs(dx) > touchSlop) {
                            isDragging = true
                            isSeeking = true
                            v.cancelLongPress()
                            binding.progressTip.visibility = View.VISIBLE
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            initialPosition = player?.currentPosition ?: 0L
                        }
                        
                        if (isDragging) {
                            val screenWidth = binding.root.width
                            val percentage = dx / screenWidth
                            val duration = player?.duration ?: 0L
                            
                            targetPosition = (initialPosition + (percentage * duration)).toLong()
                            targetPosition = targetPosition.coerceIn(0, duration)
                            
                            binding.seekBar.progress = targetPosition.toInt()
                            binding.currentTimeText.text = formatTime(targetPosition)
                            binding.progressTipText.text = formatTime(targetPosition)
                            val progressPercent = if (duration > 0) ((targetPosition.toDouble() / duration) * 100).toInt() else 0
                            binding.progressTipPercent.text = "${progressPercent}%"
                            true
                        } else false
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            binding.progressTip.visibility = View.GONE
                            lastManualSeekTime = System.currentTimeMillis()
                            player?.seekTo(targetPosition)
                            binding.seekBar.progress = targetPosition.toInt()
                            binding.currentTimeText.text = formatTime(targetPosition)
                            v.postDelayed({
                                isDragging = false
                                isSeeking = false
                            }, 1500)
                            true
                        } else false
                    }
                    else -> false
                }
            }

            binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.currentTimeText.text = formatTime(progress.toLong())
                        player?.seekTo(progress.toLong())
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) { isSeeking = true }
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    lastManualSeekTime = System.currentTimeMillis()
                    binding.root.postDelayed({ isSeeking = false }, 1500)
                }
            })
            
            binding.videoController.visibility = View.VISIBLE
        }
        
        private fun togglePlayPause() {
            if (player?.isPlaying == true) pauseVideo() else playVideo()
        }

        fun updateVideoInfo() {
            val duration = player?.duration ?: 0L
            if (duration > 0) {
                binding.seekBar.max = duration.toInt()
                binding.totalTimeText.text = formatTime(duration)
            }
        }

        fun startUpdatingSeekBar() {
            val updateRunnable = object : Runnable {
                override fun run() {
                    try {
                        val now = System.currentTimeMillis()
                        if (player?.isPlaying == true && !isSeeking && !isDragging && (now - lastManualSeekTime > 1500)) {
                            val currentPos = player?.currentPosition ?: 0L
                            binding.seekBar.progress = currentPos.toInt()
                            binding.currentTimeText.text = formatTime(currentPos)
                        }
                        if (binding.root.isAttachedToWindow) {
                            binding.root.postDelayed(this, 1000)
                        }
                    } catch (e: Exception) {}
                }
            }
            binding.root.post(updateRunnable)
        }
        
        private fun formatTime(milliseconds: Long): String {
            val totalSeconds = milliseconds / 1000
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return String.format("%02d:%02d", mins, secs)
        }
        
        fun setupLongClick(url: String) {
            binding.playerView.setOnLongClickListener {
                if (isDragging) return@setOnLongClickListener false
                val context = itemView.context
                showSaveDialog(context, "要保存这个视频吗？") { saveVideoToGallery(context, url) }
                true
            }
        }
        
        private fun showSaveDialog(context: Context, message: String, onSave: () -> Unit) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else vibrator.vibrate(50)

            AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("保存") { d, _ -> onSave(); d.dismiss() }
                .setNegativeButton("取消") { d, _ -> d.dismiss() }
                .show()
        }

        private fun saveVideoToGallery(context: Context, videoUrl: String) {
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val name = "ZhudApp_VID_${System.currentTimeMillis()}.mp4"
                val req = DownloadManager.Request(Uri.parse(videoUrl))
                    .setTitle(name).setMimeType("video/mp4")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "ZhudApp/$name")
                dm.enqueue(req)
                Toast.makeText(context, "开始下载", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { Toast.makeText(context, "失败", Toast.LENGTH_SHORT).show() }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mediaUrls[position].contains(".mp4", ignoreCase = true)) VIEW_TYPE_VIDEO else VIEW_TYPE_IMAGE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_VIDEO) {
            VideoPagerViewHolder(PagerItemVideoBinding.inflate(inflater, parent, false))
        } else {
            ImagePagerViewHolder(ItemImagePagerBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaUrl = mediaUrls[position]
        val context = holder.itemView.context
        if (holder is VideoPagerViewHolder) {
            holder.bind(mediaUrl)
            holder.setupLongClick(mediaUrl)
        }
        else if (holder is ImagePagerViewHolder) bindImage(holder, mediaUrl, context)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoPagerViewHolder) {
            holder.releasePlayer()
        }
    }

    private fun bindImage(holder: ImagePagerViewHolder, imageUrl: String, context: Context) {
        Glide.with(context).load(imageUrl).placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder).into(holder.binding.photoView)
        holder.binding.photoView.setOnLongClickListener {
            showSaveDialog(context, "要保存这张图片吗？") { saveImageToGallery(holder) }
            true
        }
    }

    private fun showSaveDialog(context: Context, message: String, onSave: () -> Unit) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else vibrator.vibrate(50)

        AlertDialog.Builder(context)
            .setMessage(message)
            .setPositiveButton("保存") { d, _ -> onSave(); d.dismiss() }
            .setNegativeButton("取消") { d, _ -> d.dismiss() }
            .show()
    }

    override fun getItemCount(): Int = mediaUrls.size

    private fun saveImageToGallery(holder: ImagePagerViewHolder) {
        val bitmap = (holder.binding.photoView.drawable as? BitmapDrawable)?.bitmap ?: return
        val context = holder.itemView.context
        val name = "ZhudApp_IMG_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZhudApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { s -> bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, s) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(it, values, null, null)
            }
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        }
    }
}
