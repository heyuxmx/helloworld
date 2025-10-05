package com.heyu.zhudeapp.repository

import android.content.Context
import com.heyu.zhudeapp.model.CountdownItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnniversaryRepository(context: Context) {

    private val sharedPreferences = context.getSharedPreferences("AnniversaryPrefs", Context.MODE_PRIVATE)

    fun getAnniversaries(): MutableList<CountdownItem> {
        val jsonString = sharedPreferences.getString(PREFS_KEY, null)
        return if (jsonString != null) {
            Json.decodeFromString<MutableList<CountdownItem>>(jsonString)
        } else {
            // Return a default list if nothing is saved yet
            mutableListOf(
                CountdownItem(name = "在一起的纪念日", month = 12, day = 2),
                CountdownItem(name = "第一次见面的纪念日", month = 12, day = 31)
            )
        }
    }

    fun saveAnniversaries(anniversaries: List<CountdownItem>) {
        val jsonString = Json.encodeToString(anniversaries)
        sharedPreferences.edit().putString(PREFS_KEY, jsonString).apply()
    }

    companion object {
        private const val PREFS_KEY = "anniversaries_list"
    }
}
