package com.heyu.zhudeapp.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    // These fields are not sent during creation, but are returned by the database.
    // They need to have default values to handle the creation-time case.
    val id: Long = 0,
    @SerialName("created_at")
    val createdAt: String = "",

    // These are the fields you send when creating a new post.
    val content: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList()
)
