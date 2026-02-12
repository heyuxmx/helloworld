package com.heyu.zhudeapp.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
@Parcelize
data class UserProfile(
    @SerialName("id")
    val id: String = "",
    @SerialName("username")
    val username: String = "未知用户",
    @SerialName("avatar_url")
    val avatarUrl: String? = null
) : Parcelable
