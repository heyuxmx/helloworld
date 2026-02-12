package com.heyu.zhudeapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
data class Comment(
    val id: Long = 0,
    @SerialName("post_id")
    val postId: Long,
    @SerialName("user_id")
    val userId: String,
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    
    // This field will be populated by a join query with the 'users' table.
    val author: UserProfile? = null
) : Parcelable
