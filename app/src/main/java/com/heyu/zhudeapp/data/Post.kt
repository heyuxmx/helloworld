package com.heyu.zhudeapp.data

import android.annotation.SuppressLint
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@SuppressLint("UnsafeOptInUsageError")
@Parcelize
@Serializable
data class Post(
    // Fields sent when creating a new post.
    val content: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),

    // Fields returned by the database.
    // They have default values to handle the creation-time case.
    val id: Long = 0,
    @SerialName("created_at")
    val createdAt: String = "",

    var likes: Int = 0,
    // This will be populated by a separate query, so it should be transient for Post serialization.
    @Transient
    val comments: MutableList<Comment> = mutableListOf(),

    @Transient
    var isLiked: Boolean = false, // To track if the current user has liked the post

    // --- Fields for Optimistic UI ---
    @Transient
    val isUploading: Boolean = false,
    @Transient
    val uploadFailed: Boolean = false,
    @Transient
    val localImageUris: List<String> = emptyList()

) : Parcelable

@Parcelize
@Serializable
data class Comment(
    // id can be null when creating a new comment, so it's nullable.
    val id: Long? = null,

    // Maps to the 'post_id' column in the database. Must be provided.
    @SerialName("post_id")
    val postId: Long,

    // Maps to 'user_name' in DB, used by CommentAdapter.
    @SerialName("user_name")
    val userName: String,

    // Maps to 'content' in DB, used by CommentAdapter as 'text'.
    @SerialName("content")
    val text: String,

    @SerialName("created_at")
    val createdAt: String = ""
) : Parcelable
