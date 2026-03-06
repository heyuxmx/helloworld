package com.heyu.zhudeapp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.heyu.zhudeapp.adapter.ImagePagerAdapter
import com.heyu.zhudeapp.databinding.ActivityPostImagePagerBinding
import com.heyu.zhudeapp.util.ThemeManager

class PostImagePagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostImagePagerBinding
    private lateinit var pagerAdapter: ImagePagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPostImagePagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("image_urls") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("current_position", 0)

        binding.videoContainer.visibility = View.GONE
        binding.imagePager.visibility = View.VISIBLE

        pagerAdapter = ImagePagerAdapter(imageUrls)
        binding.imagePager.adapter = pagerAdapter

        binding.imagePager.setCurrentItem(currentPosition, false)

        binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val counterText = "${position + 1} / ${imageUrls.size}"
                binding.pagerCounter.text = counterText

                // Delegate video control to adapter for better lifecycle management
                val recyclerView = binding.imagePager.getChildAt(0) as? RecyclerView
                if (recyclerView != null) {
                    pagerAdapter.onPageSelected(position, recyclerView)
                }
            }
        })

        val initialCounterText = "${currentPosition + 1} / ${imageUrls.size}"
        binding.pagerCounter.text = initialCounterText

        // Handle initial playback
        binding.imagePager.post {
            val recyclerView = binding.imagePager.getChildAt(0) as? RecyclerView
            if (recyclerView != null) {
                pagerAdapter.onPageSelected(currentPosition, recyclerView)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        pagerAdapter.pauseActiveVideo()
    }

    override fun onStop() {
        super.onStop()
        pagerAdapter.pauseActiveVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        pagerAdapter.releaseAll()
    }
}