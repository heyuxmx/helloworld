package com.heyu.zhudeapp.data

import android.annotation.SuppressLint
import android.os.Parcelable
import com.heyu.zhudeapp.data.Comment
import com.heyu.zhudeapp.data.UserProfile
import kotlinx.parcelize.IgnoredOnParcel
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
    @SerialName("user_id")
    val userId: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    
    // 新增：视频链接字段，用于存储阿里云 OSS 的地址
    @SerialName("video_url")
    val videoUrl: String? = null,

    // Fields returned by the database.
    // They have default values to handle the creation-time case.
    val id: Long = 0,
    @SerialName("created_at")
    val createdAt: String = "",

    var likes: Int = 0,
    // This will be populated by the join query, so it should NOT be transient.
    @IgnoredOnParcel
    val comments: MutableList<Comment> = mutableListOf(),

    // This field will be populated by a join query with the 'users' table.
    // It's transient because it's not a direct column in the 'posts' table.
    @Transient
    @IgnoredOnParcel
    val author: UserProfile? = null, // To hold the author's profile data

    @kotlinx.serialization.Transient
    @IgnoredOnParcel
    var isLiked: Boolean = false, // To track if the current user has liked the post

    // --- Fields for Optimistic UI ---
    @kotlinx.serialization.Transient
    @IgnoredOnParcel
    val isUploading: Boolean = false,
    @kotlinx.serialization.Transient
    @IgnoredOnParcel
    val uploadFailed: Boolean = false,
    @kotlinx.serialization.Transient
    @IgnoredOnParcel
    val localImageUris: List<String> = emptyList()

) : Parcelable
