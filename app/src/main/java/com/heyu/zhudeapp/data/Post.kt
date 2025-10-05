package com.heyu.zhudeapp.data

import android.annotation.SuppressLint
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class Post(
    // These are the fields you send when creating a new post.
    val content: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),

    // These fields are not sent during creation, but are returned by the database.
    // They need to have default values to handle the creation-time case.
    val id: Long = 0,
    @SerialName("created_at")
    val createdAt: String = "",

    // --- Fields for Optimistic UI ---
    @Transient
    val isUploading: Boolean = false,
    @Transient
    val uploadFailed: Boolean = false,
    @Transient
    val localImageUris: List<String> = emptyList()

) : java.io.Serializable
