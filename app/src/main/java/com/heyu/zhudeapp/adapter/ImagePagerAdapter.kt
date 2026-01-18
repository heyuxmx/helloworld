package com.heyu.zhudeapp.adapter

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
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
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ItemImagePagerBinding
import com.heyu.zhudeapp.databinding.PagerItemVideoBinding
import java.io.OutputStream

class ImagePagerAdapter(private val mediaUrls: List<String>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_VIDEO = 2
    }

    class ImagePagerViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)

    class VideoPagerViewHolder(val binding: PagerItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        private var isVideoPrepared = false
        private var isDragging = false
        private var isSeeking = false
        private var lastManualSeekTime = 0L

        fun getIsDragging(): Boolean = isDragging

        fun playVideo() {
            try {
                if (isVideoPrepared && !binding.videoView.isPlaying) {
                    binding.videoView.start()
                    binding.playPauseButton.setImageResource(R.drawable.ic_videostop)
                    binding.playPauseButton.tag = "playing"
                } else if (!isVideoPrepared) {
                    binding.playPauseButton.setImageResource(R.drawable.ic_videostop)
                    binding.playPauseButton.tag = "playing"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun pauseVideo() {
            try {
                if (binding.videoView.isPlaying) {
                    binding.videoView.pause()
                    binding.playPauseButton.setImageResource(R.drawable.ic_videoplay)
                    binding.playPauseButton.tag = "paused"
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun setIsVideoPrepared(prepared: Boolean) {
            this.isVideoPrepared = prepared
        }

        fun initializeVideoControls() {
            binding.playPauseButton.setOnClickListener { togglePlayPause() }
            binding.videoView.setOnClickListener { togglePlayPause() }
            
            val touchSlop = ViewConfiguration.get(binding.root.context).scaledTouchSlop
            var initialPosition = 0
            var initialTouchX = 0f
            var targetPosition = 0
            
            binding.videoView.setOnTouchListener { v, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialTouchX = event.x
                        isDragging = false
                        false // 允许系统处理后续的点击和长按
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        val dx = event.x - initialTouchX
                        
                        // 1. 判断是否进入滑动状态
                        if (!isDragging && kotlin.math.abs(dx) > touchSlop) {
                            isDragging = true
                            isSeeking = true
                            v.cancelLongPress() // 确定滑动，取消长按保存计时
                            binding.progressTip.visibility = View.VISIBLE
                            v.parent.requestDisallowInterceptTouchEvent(true)
                            
                            // 核心修复：在真正开始滑动的瞬间捕捉当前位置，防止起始点跳变
                            initialPosition = binding.videoView.currentPosition
                        }
                        
                        if (isDragging && binding.videoView.duration > 0) {
                            val screenWidth = binding.root.width
                            val percentage = dx / screenWidth
                            val duration = binding.videoView.duration
                            
                            targetPosition = (initialPosition + (percentage * duration)).toInt()
                            targetPosition = targetPosition.coerceIn(0, duration)
                            
                            // 滑动时只更新 UI，不调用 seekTo，确保滑动顺滑
                            binding.seekBar.progress = targetPosition
                            binding.currentTimeText.text = formatTime(targetPosition.toLong())
                            
                            binding.progressTipText.text = formatTime(targetPosition.toLong())
                            val progressPercent = if (duration > 0) ((targetPosition.toDouble() / duration) * 100).toInt() else 0
                            binding.progressTipPercent.text = "${progressPercent}%"
                            true
                        } else false
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        if (isDragging) {
                            binding.progressTip.visibility = View.GONE
                            lastManualSeekTime = System.currentTimeMillis()
                            
                            // 2. 只有在松手时才执行跳转
                            binding.videoView.seekTo(targetPosition)
                            
                            // 视觉先行：立即把进度条设为终点位置
                            binding.seekBar.progress = targetPosition
                            binding.currentTimeText.text = formatTime(targetPosition.toLong())

                            // 延长锁定时间到1.5秒，等待底层跳转稳定，彻底解决回退问题
                            v.postDelayed({
                                isDragging = false
                                isSeeking = false
                            }, 1500)
                            true
                        } else {
                            // 注意：这里不要 performClick，否则会导致短按触发两次 toggle
                            false
                        }
                    }
                    else -> false
                }
            }

            binding.seekBar.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.currentTimeText.text = formatTime(progress.toLong())
                        binding.videoView.seekTo(progress)
                    }
                }

                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {
                    isSeeking = true
                }

                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {
                    lastManualSeekTime = System.currentTimeMillis()
                    binding.root.postDelayed({
                        isSeeking = false
                    }, 1500)
                }
            })
        }
        
        private fun togglePlayPause() {
            if (binding.videoView.isPlaying) pauseVideo() else playVideo()
        }

        fun updateVideoInfo() {
            if (binding.videoView.duration > 0) {
                binding.seekBar.max = binding.videoView.duration
                binding.totalTimeText.text = formatTime(binding.videoView.duration.toLong())
            }
        }

        fun startUpdatingSeekBar() {
            val updateRunnable = object : Runnable {
                override fun run() {
                    try {
                        val now = System.currentTimeMillis()
                        // 如果正在操作或操作结束不足1.5秒，不强行同步进度，防止回跳
                        if (binding.videoView.isPlaying && !isSeeking && !isDragging && (now - lastManualSeekTime > 1500)) {
                            val currentPosition = binding.videoView.currentPosition
                            binding.seekBar.progress = currentPosition
                            binding.currentTimeText.text = formatTime(currentPosition.toLong())
                        }
                        if (binding.root.isAttachedToWindow) {
                            binding.videoView.handler.postDelayed(this, 1000)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
            binding.videoView.handler.post(updateRunnable)
        }
        
        private fun formatTime(milliseconds: Long): String {
            val seconds = milliseconds / 1000
            val mins = seconds % 3600 / 60
            val secs = seconds % 60
            return String.format("%02d:%02d", mins, secs)
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
        if (holder is VideoPagerViewHolder) bindVideo(holder, mediaUrl, context)
        else if (holder is ImagePagerViewHolder) bindImage(holder, mediaUrl, context)
    }

    private fun bindImage(holder: ImagePagerViewHolder, imageUrl: String, context: Context) {
        Glide.with(context).load(imageUrl).placeholder(R.drawable.image_placeholder).error(R.drawable.image_placeholder).into(holder.binding.photoView)
        holder.binding.photoView.setOnLongClickListener {
            showSaveDialog(context, "要保存这张图片吗？") { saveImageToGallery(holder) }
            true
        }
    }

    private fun bindVideo(holder: VideoPagerViewHolder, videoUrl: String, context: Context) {
        holder.binding.videoProgressBar.visibility = View.VISIBLE
        holder.binding.videoController.visibility = View.VISIBLE
        holder.initializeVideoControls()
        holder.binding.videoView.setVideoURI(Uri.parse(videoUrl))
        
        holder.binding.videoView.setOnPreparedListener { mp ->
            holder.binding.videoProgressBar.visibility = View.GONE
            holder.setIsVideoPrepared(true)
            mp.isLooping = true
            holder.updateVideoInfo()
            holder.startUpdatingSeekBar()
            if (holder.binding.playPauseButton.tag == "playing") holder.binding.videoView.start()
        }

        holder.binding.videoView.setOnLongClickListener {
            // 滑动时不触发长按
            if (holder.getIsDragging()) return@setOnLongClickListener false
            showSaveDialog(context, "要保存这个视频吗？") { saveVideoToGallery(context, videoUrl) }
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
            context.contentResolver.openOutputStream(it)?.use { s -> bitmap.compress(Bitmap.CompressFormat.JPEG, 100, s) }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(it, values, null, null)
            }
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show()
        }
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
