package com.heyu.zhudeapp.adapter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.R
import com.heyu.zhudeapp.databinding.ItemImagePagerBinding
import java.io.OutputStream

class ImagePagerAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImagePagerAdapter.ImagePagerViewHolder>() {

    class ImagePagerViewHolder(val binding: ItemImagePagerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImagePagerViewHolder {
        val binding = ItemImagePagerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImagePagerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImagePagerViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        val context = holder.itemView.context
        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.image_placeholder) // Optional placeholder
            .error(R.drawable.image_placeholder)       // Optional error image
            .into(holder.binding.photoView)

        holder.binding.photoView.setOnLongClickListener {
            // Vibrate for feedback
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    //deprecated in API 26
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

    override fun getItemCount(): Int = imageUrls.size

    private fun saveImageToGallery(holder: ImagePagerViewHolder) {
        val context = holder.itemView.context
        val photoView = holder.binding.photoView

        // Get the Bitmap from the PhotoView
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
}
