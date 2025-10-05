package com.heyu.zhudeapp.Fragment.countdown

import android.icu.util.ChineseCalendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.heyu.zhudeapp.adapter.CountdownAdapter
import com.heyu.zhudeapp.databinding.FragmentHolidaysCountdownBinding
import com.heyu.zhudeapp.model.CountdownItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HolidaysCountdownFragment : Fragment() {

    private var _binding: FragmentHolidaysCountdownBinding? = null
    private val binding get() = _binding!!

    // 新方法：通过固定的农历月、日来定义农历节日
    // 注意: ChineseCalendar 的月份是从0开始的 (0=正月), 日期是从1开始的
    private val lunarHolidays = mapOf(
        "春节" to (0 to 1),   // 正月初一
        "端午节" to (4 to 5), // 五月初五
        "中秋节" to (7 to 15)  // 八月十五
    )

    // 公历节日定义 (保持不变)
    private val gregorianHolidays = mapOf(
        "元旦" to (1 to 1),
        "清明节" to (4 to 4), // 为简化，清明节固定为4月4日
        "劳动节" to (5 to 1),
        "国庆节" to (10 to 1)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHolidaysCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 结合公历和农历数据，计算出所有节日下一个最近的日期和倒计时
        val processedHolidays = calculateNextHolidayDates()

        // 2. 将计算结果按剩余天数升序排序
        val sortedHolidays = processedHolidays.sortedBy { it.daysRemaining }

        // 3. 将最终结果交给适配器显示
        val adapter = CountdownAdapter(sortedHolidays.toMutableList())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    /**
     * 智能计算所有节日的下一个倒数日
     * 使用 Android 内置的 ChineseCalendar, 真正做到“一劳永逸”
     */
    private fun calculateNextHolidayDates(): List<CountdownItem> {
        val today = LocalDate.now()
        val holidayItems = mutableListOf<CountdownItem>()
        val allHolidayNames = gregorianHolidays.keys + lunarHolidays.keys

        for (name in allHolidayNames) {
            val nextEventDate: LocalDate

            if (lunarHolidays.containsKey(name)) {
                // --- 全新的、动态的农历计算逻辑 ---
                val (lunarMonth, lunarDay) = lunarHolidays[name]!!

                // 创建一个以今天为上下文的农历日历实例
                val calendar = ChineseCalendar()

                // 将日历设置为当前农历年份的指定农历月、日
                calendar.set(ChineseCalendar.MONTH, lunarMonth)
                calendar.set(ChineseCalendar.IS_LEAP_MONTH, 0) // 假设节日不在闰月
                calendar.set(ChineseCalendar.DAY_OF_MONTH, lunarDay)

                // 【修正】使用官方推荐的、最可靠的方式进行日历转换
                var gregorianDate = Instant.ofEpochMilli(calendar.timeInMillis)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()

                // 【终极修正】如果计算出的公历日期在今天之前，说明今年的这个节日已经过了。
                // 我们需要循环查找，直到找到第一个在今天或今天之后的节日日期。
                // 使用 "while" 循环可以健壮地处理所有边界情况。
                while (gregorianDate.isBefore(today)) {
                    // 让农历年份+1, 自动计算下一年的节日日期
                    calendar.add(ChineseCalendar.YEAR, 1)
                    // 再次使用可靠的方式进行转换
                    gregorianDate = Instant.ofEpochMilli(calendar.timeInMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                }
                nextEventDate = gregorianDate

            } else {
                // --- 公历节日计算逻辑 (保持不变) ---
                val (month, day) = gregorianHolidays[name]!!
                val dateInCurrentYear = LocalDate.of(today.year, month, day)
                nextEventDate = if (dateInCurrentYear.isBefore(today)) {
                    dateInCurrentYear.plusYears(1)
                } else {
                    dateInCurrentYear
                }
            }

            // 使用计算出的精确日期创建倒计时项目
            val daysRemaining = ChronoUnit.DAYS.between(today, nextEventDate)
            val dateOverride = String.format("%d年%d月%d日", nextEventDate.year, nextEventDate.monthValue, nextEventDate.dayOfMonth)
            holidayItems.add(
                CountdownItem(
                    name = name,
                    month = nextEventDate.monthValue,
                    day = nextEventDate.dayOfMonth,
                    dateOverride = dateOverride,
                    daysRemaining = daysRemaining
                )
            )
        }
        return holidayItems
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
