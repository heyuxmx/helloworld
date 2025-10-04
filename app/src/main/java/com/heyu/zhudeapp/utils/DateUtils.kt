package com.heyu.zhudeapp.utils

import android.annotation.SuppressLint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * 时间转换工具类
 */
object DateUtils {

    // Supabase返回的UTC时间格式
    private const val UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX"
    // 定义各种我们希望输出的时间格式
    private const val TODAY_FORMAT = "今天 HH:mm"
    private const val YESTERDAY_FORMAT = "昨天 HH:mm"
    private const val DEFAULT_FORMAT = "MM-dd HH:mm"

    /**
     * 将从Supabase获取的UTC时间字符串，转换为人性化的、本地化的时间格式。
     * @param utcString 例如 "2024-05-20T10:30:00.123456+00:00"
     * @return 格式化后的字符串，例如 "刚刚", "5分钟前", "今天 18:30"
     */
    @SuppressLint("SimpleDateFormat")
    fun formatTime(utcString: String): String {
        try {
            // 1. 创建一个SimpleDateFormat来解析UTC时间字符串
            val utcParser = SimpleDateFormat(UTC_FORMAT).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = utcParser.parse(utcString) ?: return ""

            val now = Date()
            val diff = now.time - date.time // 毫秒差

            val diffSeconds = diff / 1000
            val diffMinutes = diff / (1000 * 60)
            val diffHours = diff / (1000 * 60 * 60)

            // 2. 根据时间差，返回不同的格式
            return when {
                diffSeconds < 60 -> "刚刚"
                diffMinutes < 60 -> "${diffMinutes}分钟前"
                // 判断是否是今天
                isSameDay(date, now) -> SimpleDateFormat(TODAY_FORMAT).format(date)
                // 判断是否是昨天
                isYesterday(date, now) -> SimpleDateFormat(YESTERDAY_FORMAT).format(date)
                // 其他情况
                else -> SimpleDateFormat(DEFAULT_FORMAT).format(date)
            }
        } catch (e: Exception) {
            // 如果解析出错，返回空字符串或一个默认值
            return ""
        }
    }

    /**
     * 检查两个Date对象是否表示同一天。
     */
    @SuppressLint("SimpleDateFormat")
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val fmt = SimpleDateFormat("yyyy-MM-dd")
        // 注意：这里使用系统默认时区来格式化，是正确的，因为我们要判断的“天”是本地的“天”
        return fmt.format(date1) == fmt.format(date2)
    }

    /**
     * 检查date1是否是date2的昨天。
     */
    private fun isYesterday(date1: Date, date2: Date): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { time = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { time = date2 }
        cal2.add(java.util.Calendar.DAY_OF_YEAR, -1)
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }
}
