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

    // --- ViewHolder for Images ---
    class ImagePagerViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)

    // --- ViewHolder for Videos ---
    class VideoPagerViewHolder(val binding: PagerItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun playVideo() {
            binding.videoView.start()
        }

        fun pauseVideo() {
            if (binding.videoView.isPlaying) {
                binding.videoView.pause()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (mediaUrls[position].contains(".mp4", ignoreCase = true)) {
            VIEW_TYPE_VIDEO
        } else {
            VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_VIDEO) {
            val binding = PagerItemVideoBinding.inflate(inflater, parent, false)
            VideoPagerViewHolder(binding)
        } else {
            val binding = ItemImagePagerBinding.inflate(inflater, parent, false)
            ImagePagerViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val mediaUrl = mediaUrls[position]
        val context = holder.itemView.context

        if (holder.itemViewType == VIEW_TYPE_VIDEO) {
            val videoHolder = holder as VideoPagerViewHolder
            bindVideo(videoHolder, mediaUrl, context)
        } else {
            val imageHolder = holder as ImagePagerViewHolder
            bindImage(imageHolder, mediaUrl, context)
        }
    }

    private fun bindImage(holder: ImagePagerViewHolder, imageUrl: String, context: Context) {
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.image_placeholder)
            .error(R.drawable.image_placeholder)
            .into(holder.binding.photoView)

        holder.binding.photoView.setOnLongClickListener {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }

            AlertDialog.Builder(context)
                .setMessage("要保存这张图片吗？")
                .setPositiveButton("保存") { dialog, _ ->
                    saveImageToGallery(holder)
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true
        }
    }

    private fun bindVideo(holder: VideoPagerViewHolder, videoUrl: String, context: Context) {
        val videoUri = Uri.parse(videoUrl)
        holder.binding.videoProgressBar.visibility = View.VISIBLE
        holder.binding.videoView.setVideoURI(videoUri)
        holder.binding.videoView.setOnPreparedListener { mp ->
            holder.binding.videoProgressBar.visibility = View.GONE
            mp.isLooping = true // Loop the video
            // Auto-play is handled by the Activity now
        }
        holder.binding.videoView.setOnInfoListener { _, what, _ ->
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> holder.binding.videoProgressBar.visibility = View.VISIBLE
                MediaPlayer.MEDIA_INFO_BUFFERING_END -> holder.binding.videoProgressBar.visibility = View.GONE
            }
            true
        }
        holder.binding.videoView.setOnErrorListener { _, _, _ ->
            holder.binding.videoProgressBar.visibility = View.GONE
            Toast.makeText(context, "播放视频失败", Toast.LENGTH_SHORT).show()
            true
        }

        // --- 傻逼我在这里加上了长按下载的功能 ---
        holder.binding.videoView.setOnLongClickListener {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(50)
                }
            }

            AlertDialog.Builder(context)
                .setMessage("要保存这个视频吗？")
                .setPositiveButton("保存") { dialog, _ ->
                    saveVideoToGallery(context, videoUrl)
                    dialog.dismiss()
                }
                .setNegativeButton("取消") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
            true
        }
    }


    override fun getItemCount(): Int = mediaUrls.size

    private fun saveImageToGallery(holder: ImagePagerViewHolder) {
        val context = holder.itemView.context
        val photoView = holder.binding.photoView

        val drawable = photoView.drawable as? BitmapDrawable
        val bitmap = drawable?.bitmap
        if (bitmap == null) {
            Toast.makeText(context, "无法保存图片，资源未加载完成", Toast.LENGTH_SHORT).show()
            return
        }

        val displayName = "ZhudApp_Image_${System.currentTimeMillis()}.jpg"
        val mimeType = "image/jpeg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ZhudApp")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(it)
                outputStream?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
                Toast.makeText(context, "图片已保存至相册", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "保存失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "创建图片文件失败", Toast.LENGTH_SHORT).show()
        }
    }

    // --- 傻逼我在这里加上了保存视频的方法 ---
    private fun saveVideoToGallery(context: Context, videoUrl: String) {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(videoUrl))
            val fileName = "ZhudApp_Video_${System.currentTimeMillis()}.mp4"

            request.setTitle(fileName)
            request.setDescription("正在下载...")
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // Save to /Movies/ZhudApp directory
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_MOVIES, "ZhudApp/$fileName")
            request.setMimeType("video/mp4")

            downloadManager.enqueue(request)
            Toast.makeText(context, "保存成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "无法开始下载: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
