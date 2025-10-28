package com.heyu.zhudeapp.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlin.OptIn

@OptIn(InternalSerializationApi::class)
@Serializable
data class UserProfile(
    @SerialName("id")
    val id: String,
    @SerialName("username")
    val username: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null
)
