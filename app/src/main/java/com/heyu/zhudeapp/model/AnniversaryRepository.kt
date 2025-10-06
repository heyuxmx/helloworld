package com.heyu.zhudeapp.model

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException

class AnniversaryRepository(private val context: Context) {

    private val sharedPreferences by lazy {
        context.getSharedPreferences("anniversary_prefs", Context.MODE_PRIVATE)
    }

    companion object {
        // Using a versioned key is a good practice to prevent deserialization
        // errors if the data class structure changes in the future.
        private const val ANNIVERSARIES_KEY = "anniversaries_list_v2"
    }

    /**
     * Retrieves the list of custom anniversaries from SharedPreferences.
     */
    fun getAnniversaries(): List<CustomAnniversary> {
        val jsonString = sharedPreferences.getString(ANNIVERSARIES_KEY, null) ?: return emptyList()
        return try {
            // Decode into the raw data model class
            Json.decodeFromString<List<CustomAnniversary>>(jsonString)
        } catch (e: SerializationException) {
            // If decoding fails (e.g., data structure changed), return an empty list
            // to prevent a crash. This is a safe fallback.
            emptyList()
        }
    }

    /**
     * Saves the provided list of anniversaries to SharedPreferences.
     * The calling code is responsible for passing the correct list (e.g., only custom anniversaries).
     */
    fun saveAnniversaries(anniversaries: List<CustomAnniversary>) {
        val jsonString = Json.encodeToString(anniversaries)
        sharedPreferences.edit().putString(ANNIVERSARIES_KEY, jsonString).apply()
    }
}
