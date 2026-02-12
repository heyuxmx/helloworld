package com.heyu.zhudeapp.database

import androidx.room.TypeConverter
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class Converters {
    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromCommentList(value: MutableList<Comment>): String {
        return json.encodeToString(value.toList())
    }

    @TypeConverter
    fun toCommentList(value: String): MutableList<Comment> {
        return try {
            json.decodeFromString<List<Comment>>(value).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
    }

    @TypeConverter
    fun fromUserProfile(value: UserProfile?): String? {
        return value?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toUserProfile(value: String?): UserProfile? {
        return value?.let { json.decodeFromString(it) }
    }
}
