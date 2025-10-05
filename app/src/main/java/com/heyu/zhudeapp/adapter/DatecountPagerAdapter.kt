package com.heyu.zhudeapp.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.heyu.zhudeapp.Fragment.countdown.AnniversaryCountdownFragment
import com.heyu.zhudeapp.Fragment.countdown.BirthdayCountdownFragment
import com.heyu.zhudeapp.Fragment.countdown.HolidaysCountdownFragment

class DatecountPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val fragments = listOf(
        HolidaysCountdownFragment(),
        AnniversaryCountdownFragment(),
        BirthdayCountdownFragment()
    )

    override fun getItemCount(): Int = fragments.size

    override fun createFragment(position: Int): Fragment {
        return fragments[position]
    }
}
