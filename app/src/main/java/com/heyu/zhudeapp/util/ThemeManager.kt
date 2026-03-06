package com.heyu.zhudeapp.util

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.heyu.zhudeapp.R

object ThemeManager {

    const val THEME_DEFAULT = "default"
    const val THEME_TECH = "tech"

    private const val PREFS = "app_settings"
    private const val KEY_THEME = "theme"

    /** 必须在 setContentView() 之前调用 */
    fun applyTheme(activity: AppCompatActivity) {
        when (getCurrent(activity)) {
            THEME_TECH -> activity.setTheme(R.style.Theme_ZhudeApp_Tech)
            else -> Unit // 默认主题已在 AndroidManifest 里声明
        }
    }

    fun getCurrent(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_THEME, THEME_DEFAULT) ?: THEME_DEFAULT

    fun setTheme(context: Context, theme: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_THEME, theme).apply()
    }

    fun isTech(context: Context) = getCurrent(context) == THEME_TECH
}
