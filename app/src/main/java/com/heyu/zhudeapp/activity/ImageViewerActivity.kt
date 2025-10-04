package com.heyu.zhudeapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.heyu.zhudeapp.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从 Intent 中获取图片 URL
        val imageUrl = intent.getStringExtra("IMAGE_URL")

        // 使用 Glide 将图片加载到 PhotoView 中
        if (imageUrl != null) {
            Glide.with(this)
                .load(imageUrl)
                .into(binding.photoView)
        }
    }

    companion object {
        const val IMAGE_URL = "IMAGE_URL"
    }
}
