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
    val content: String,
    @SerialName("created_at")
    val createdAt: String? = null,
    // You might want to add user information here in the future
    // @SerialName("user_id")
    // val userId: String? = null
)
