package com.heyu.zhudeapp.data

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable

@OptIn(InternalSerializationApi::class)
@Serializable
data class UpdateInfo(
    val latestVersionCode: Int,
    val downloadUrl: String
)
