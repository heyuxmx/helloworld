package com.heyu.zhudeapp.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.heyu.zhudeapp.activity.ImageViewerPageFragment

class ImageViewerPagerAdapter(
    activity: FragmentActivity,
    private val imageUrls: List<String>
) : FragmentStateAdapter(activity) {

    override fun getItemCount(): Int = imageUrls.size

    override fun createFragment(position: Int): Fragment {
        return ImageViewerPageFragment.newInstance(imageUrls[position])
    }
}
