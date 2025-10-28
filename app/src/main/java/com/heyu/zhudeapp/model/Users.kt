package com.heyu.zhudeapp.model

import android.content.Context
import com.heyu.zhudeapp.R

object Users {

    private const val PREFS_NAME = "ZhuDeAppPrefs"
    private const val KEY_ACTIVE_USER_ID = "active_user_id"

    const val USER_ID_GAOBAO = "user_gaobao"
    const val USER_ID_XUBABA = "user_xubaba"

    // Keys for storing user-specific data
    private const val KEY_USERNAME_GAOBAO = "username_gaobao"
    private const val KEY_AVATAR_URL_GAOBAO = "avatar_url_gaobao"
    private const val KEY_USERNAME_XUBABA = "username_xubaba"
    private const val KEY_AVATAR_URL_XUBABA = "avatar_url_xubaba"

    // --- The pre-defined PUBLIC URLs for the default avatars ---
    // These are used when creating a post, to ensure other users can see the avatar.
    private const val DEFAULT_PUBLIC_AVATAR_URL_GAOBAO = "https://yrkgoqusmsmbqmgvnhxte.supabase.co/storage/v1/object/public/avatars/logo_xiaogao.png"
    private const val DEFAULT_PUBLIC_AVATAR_URL_XUBABA = "https://yrkgoqusmsmbqmgvnhxte.supabase.co/storage/v1/object/public/avatars/logo_xiaoxu.png"

    // --- User Management ---
    fun getActiveUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACTIVE_USER_ID, USER_ID_GAOBAO) ?: USER_ID_GAOBAO
    }

    fun setActiveUserId(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ACTIVE_USER_ID, userId).apply()
    }

    // --- Username Management ---
    fun getCurrentUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = getActiveUserId(context)
        return if (userId == USER_ID_GAOBAO) {
            prefs.getString(KEY_USERNAME_GAOBAO, "小高宝宝") ?: "小高宝宝"
        } else {
            prefs.getString(KEY_USERNAME_XUBABA, "小徐爸爸") ?: "小徐爸爸"
        }
    }

    fun saveUsername(context: Context, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (getActiveUserId(context) == USER_ID_GAOBAO) KEY_USERNAME_GAOBAO else KEY_USERNAME_XUBABA
        prefs.edit().putString(key, username).apply()
    }

    // --- Avatar Management ---

    // REFACTORED: This is the final, correct implementation.
    fun getAvatarForDisplay(context: Context): Any {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = getActiveUserId(context)

        val customAvatarUrl = if (userId == USER_ID_GAOBAO) {
            prefs.getString(KEY_AVATAR_URL_GAOBAO, null)
        } else {
            prefs.getString(KEY_AVATAR_URL_XUBABA, null)
        }

        // If a custom URL exists, return it (it's a String).
        // Otherwise, return the LOCAL drawable resource ID (it's an Int).
        return customAvatarUrl ?: if (userId == USER_ID_GAOBAO) {
            R.drawable.logo_xiaogao
        } else {
            R.drawable.logo_xiaoxu
        }
    }

    fun getAvatarUrlForPost(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userId = getActiveUserId(context)

        val customAvatarUrl = if (userId == USER_ID_GAOBAO) {
            prefs.getString(KEY_AVATAR_URL_GAOBAO, null)
        } else {
            prefs.getString(KEY_AVATAR_URL_XUBABA, null)
        }

        // This function MUST always return a public URL.
        return customAvatarUrl ?: if (userId == USER_ID_GAOBAO) {
            DEFAULT_PUBLIC_AVATAR_URL_GAOBAO
        } else {
            DEFAULT_PUBLIC_AVATAR_URL_XUBABA
        }
    }

    fun saveAvatarUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = if (getActiveUserId(context) == USER_ID_GAOBAO) KEY_AVATAR_URL_GAOBAO else KEY_AVATAR_URL_XUBABA
        prefs.edit().putString(key, url).apply()
    }
}
