package com.heyu.zhudeapp.activity

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.heyu.zhudeapp.adapter.ImagePagerAdapter
import com.heyu.zhudeapp.databinding.ActivityPostImagePagerBinding

class PostImagePagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostImagePagerBinding
    private var lastPlayedViewHolder: ImagePagerAdapter.VideoPagerViewHolder? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostImagePagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("image_urls") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("current_position", 0)

        // --- Universal Pager Logic ---
        binding.videoContainer.visibility = View.GONE // We don't need this separate container anymore
        binding.imagePager.visibility = View.VISIBLE

        val pagerAdapter = ImagePagerAdapter(imageUrls)
        binding.imagePager.adapter = pagerAdapter

        binding.imagePager.setCurrentItem(currentPosition, false)

        binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // Update counter
                val counterText = "${position + 1} / ${imageUrls.size}"
                binding.pagerCounter.text = counterText

                // --- Video Playback Control ---
                // 1. Pause the last played video
                lastPlayedViewHolder?.pauseVideo()
                lastPlayedViewHolder = null // Reset

                // 2. Start the new video if the current page is a video
                val recyclerView = binding.imagePager.getChildAt(0) as? RecyclerView
                val currentViewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                if (currentViewHolder is ImagePagerAdapter.VideoPagerViewHolder) {
                    // Delay slightly to allow video to prepare
                    binding.root.postDelayed({
                        currentViewHolder.playVideo()
                        lastPlayedViewHolder = currentViewHolder
                    }, 300) // Small delay to allow preparation
                }
            }
        })

        // Set initial counter text
        val initialCounterText = "${currentPosition + 1} / ${imageUrls.size}"
        binding.pagerCounter.text = initialCounterText

        // Auto-play the first video if the initial item is a video
        // But only after the video is prepared to avoid delays
        binding.imagePager.post {
            val recyclerView = binding.imagePager.getChildAt(0) as? RecyclerView
            val initialViewHolder = recyclerView?.findViewHolderForAdapterPosition(currentPosition)
            if (initialViewHolder is ImagePagerAdapter.VideoPagerViewHolder) {
                // Delay slightly to allow video to prepare
                binding.root.postDelayed({
                    initialViewHolder.playVideo()
                    lastPlayedViewHolder = initialViewHolder
                }, 300) // Small delay to allow initial preparation
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause any playing video when the activity is not in the foreground
        lastPlayedViewHolder?.pauseVideo()
    }

    override fun onStop() {
        super.onStop()
        // Pause (or stop) video playback when the activity is no longer visible
        lastPlayedViewHolder?.pauseVideo()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Release resources
        lastPlayedViewHolder?.pauseVideo() // Ensure it's paused
    }
}