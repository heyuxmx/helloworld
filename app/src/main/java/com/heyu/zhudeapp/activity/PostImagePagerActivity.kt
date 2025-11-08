package com.heyu.zhudeapp.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.heyu.zhudeapp.adapter.ImagePagerAdapter
import com.heyu.zhudeapp.databinding.ActivityPostImagePagerBinding

class PostImagePagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostImagePagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostImagePagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageUrls = intent.getStringArrayListExtra("image_urls") ?: arrayListOf()
        val currentPosition = intent.getIntExtra("current_position", 0)

        val pagerAdapter = ImagePagerAdapter(imageUrls)
        binding.imagePager.adapter = pagerAdapter

        // Set the starting page
        binding.imagePager.setCurrentItem(currentPosition, false)

        // Update counter on page change
        binding.imagePager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val counterText = "${position + 1} / ${imageUrls.size}"
                binding.pagerCounter.text = counterText
            }
        })

        // Set initial counter text
        val initialCounterText = "${currentPosition + 1} / ${imageUrls.size}"
        binding.pagerCounter.text = initialCounterText
    }
}
