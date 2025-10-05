package com.heyu.zhudeapp.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.heyu.zhudeapp.adapters.ImageViewerAdapter
import com.heyu.zhudeapp.databinding.ActivityImageViewerBinding

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra(EXTRA_IMAGE_URLS) ?: emptyList<String>()
        val currentPosition = intent.getIntExtra(EXTRA_CURRENT_POSITION, 0)

        val adapter = ImageViewerAdapter(imageUrls)
        binding.viewPager.adapter = adapter
        binding.viewPager.setCurrentItem(currentPosition, false)
    }

    companion object {
        private const val EXTRA_IMAGE_URLS = "image_urls"
        private const val EXTRA_CURRENT_POSITION = "current_position"

        fun newIntent(context: Context, imageUrls: ArrayList<String>, currentPosition: Int): Intent {
            return Intent(context, ImageViewerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_IMAGE_URLS, imageUrls)
                putExtra(EXTRA_CURRENT_POSITION, currentPosition)
            }
        }
    }
}