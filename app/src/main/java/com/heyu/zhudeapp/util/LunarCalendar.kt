package com.heyu.zhudeapp.util

import android.icu.util.ChineseCalendar
import java.time.LocalDate
import java.time.ZoneId

object LunarCalendar {

    /**
     * Finds the next occurrence of a specific lunar holiday, starting from today.
     * This method iterates day by day, using the computer-based month index (0-11)
     * for comparison, which is robust and avoids confusion.
     *
     * @param humanLunarMonth The target lunar month using human-based numbering (1-12).
     * @param lunarDay The target lunar day (1-30).
     * @return A LocalDate object set to the start of the next upcoming specified lunar holiday.
     */
    fun getLunarDate(humanLunarMonth: Int, lunarDay: Int): LocalDate {
        // Immediately convert the human-based month (1-12) to the computer-based month (0-11).
        // All subsequent logic will use this computer-based month for unambiguous comparison.
        val computerLunarMonth = humanLunarMonth - 1

        var currentDate = LocalDate.now()
        val chineseCal = ChineseCalendar()

        // Search for up to 20000 days (over 50 years) to find the next holiday.
        for (i in 0..20000) {
            chineseCal.time = java.util.Date.from(currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

            val currentLunarMonth = chineseCal.get(ChineseCalendar.MONTH) // This is 0-11
            val currentLunarDay = chineseCal.get(ChineseCalendar.DAY_OF_MONTH)
            val isLeapMonth = chineseCal.get(ChineseCalendar.IS_LEAP_MONTH) == 1

            // A holiday must be in a non-leap month.
            // Compare the computer-based month (0-11) directly with the calendar's raw month value.
            if (!isLeapMonth && currentLunarMonth == computerLunarMonth && currentLunarDay == lunarDay) {
                return currentDate
            }

            currentDate = currentDate.plusDays(1)
        }

        // Failsafe, should be unreachable. This indicates a logic error or a very distant holiday.
        return LocalDate.now()
    }
}
