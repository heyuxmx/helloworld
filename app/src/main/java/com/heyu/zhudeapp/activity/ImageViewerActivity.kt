package com.heyu.zhudeapp.activity

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.ActivityImageViewerBinding
import es.dmoral.toasty.Toasty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.IOException

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrl = intent.getStringExtra(IMAGE_URL)

        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.photoView)

            setupLongClickListener(imageUrl)
        }
    }

    private fun setupLongClickListener(imageUrl: String) {
        binding.photoView.setOnLongClickListener {
            AlertDialog.Builder(this)
                .setTitle("下载图片")
                .setMessage("您要将这张图片保存到您的设备吗？")
                .setPositiveButton("下载") { dialog, _ ->
                    downloadImage(imageUrl)
                    dialog.dismiss()
                }
                .setNegativeButton("取消", null)
                .show()
            true
        }
    }

    private fun downloadImage(url: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = Glide.with(applicationContext)
                    .asFile()
                    .load(url)
                    .submit()
                    .get()

                val contentValues = ContentValues().apply {
                    val fileName = "zhude_${System.currentTimeMillis()}.jpg"
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ZhuDeApp")
                    }
                }

                val resolver = contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                uri?.let {
                    resolver.openOutputStream(it).use { outputStream ->
                        FileInputStream(file).use { inputStream ->
                            inputStream.copyTo(outputStream!!)
                        }
                    }
                    withContext(Dispatchers.Main) {
                        Toasty.success(applicationContext, "图片已保存至相册", Toasty.LENGTH_SHORT).show()
                    }
                } ?: throw IOException("无法创建 MediaStore 条目")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toasty.error(applicationContext, "下载失败: ${e.message}", Toasty.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val IMAGE_URL = "IMAGE_URL"
    }
}
