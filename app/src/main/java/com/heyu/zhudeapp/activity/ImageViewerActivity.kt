package com.heyu.zhudeapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.heyu.zhudeapp.adapter.ImageViewerPagerAdapter
import com.heyu.zhudeapp.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: arrayListOf()
        val currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)

        val adapter = ImageViewerPagerAdapter(this, imageUrls)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)
    }

    companion object {
        const val EXTRA_IMAGE_URLS = "image_urls"
        const val EXTRA_CURRENT_POSITION = "current_position"
    }
}
