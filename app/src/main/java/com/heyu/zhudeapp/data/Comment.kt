package com.heyu.zhudeapp.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@OptIn(InternalSerializationApi::class)
@Serializable
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
    // It's transient because it's not a direct column in the 'comments' table.
    @Transient
    val author: UserProfile? = null
)
