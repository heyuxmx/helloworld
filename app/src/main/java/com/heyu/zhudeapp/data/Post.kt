package com.heyu.zhudeapp.data

import android.annotation.SuppressLint
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
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
@Entity(tableName = "posts")
data class Post(
    val content: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("video_url")
    val videoUrl: String? = null,
    @PrimaryKey
    val id: Long = 0,
    @SerialName("created_at")
    val createdAt: String = "",
    var likes: Int = 0,
    // We keep these in the constructor to support copy() and serialization, 
    // but Room needs to know how to handle them or ignore them.
    // For "fastest loading", we WANT to persist author and comments.
    val comments: MutableList<Comment> = mutableListOf(),
    val author: UserProfile? = null
) : Parcelable {

    @Transient
    @IgnoredOnParcel
    @Ignore
    var isLiked: Boolean = false

    @Transient
    @IgnoredOnParcel
    @Ignore
    var isUploading: Boolean = false

    @Transient
    @IgnoredOnParcel
    @Ignore
    var uploadFailed: Boolean = false

    @Transient
    @IgnoredOnParcel
    @Ignore
    var localImageUris: List<String> = emptyList()
}
