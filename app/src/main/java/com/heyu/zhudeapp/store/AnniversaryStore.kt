package com.heyu.zhudeapp.store

import android.content.Context
import com.heyu.zhudeapp.model.CustomAnniversary
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AnniversaryStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAnniversaries(): List<CustomAnniversary> {
        val jsonString = prefs.getString(KEY_ANNIVERSARIES, null)
        return if (jsonString != null) {
            try {
                Json.decodeFromString<List<CustomAnniversary>>(jsonString)
            } catch (e: Exception) {
                // Handle potential deserialization errors, e.g., if the stored data is corrupt
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    fun addAnniversary(anniversary: CustomAnniversary) {
        val currentList = getAnniversaries().toMutableList()
        currentList.add(anniversary)
        val jsonString = Json.encodeToString(currentList)
        prefs.edit().putString(KEY_ANNIVERSARIES, jsonString).apply()
    }

    companion object {
        private const val PREFS_NAME = "anniversary_store"
        private const val KEY_ANNIVERSARIES = "custom_anniversaries"
    }
}
