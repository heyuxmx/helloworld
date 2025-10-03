package com.heyu.zhudeapp.data

import kotlinx.serialization.Serializable

// 建议将数据模型放在一个单独的 'data' 包中

@Serializable
data class Post(
    var id: String = "",
    var userId: String = "",
    var username: String = "",
    var userAvatarUrl: String = "",
    val content: String = "",
    val imageUrls: List<String> = emptyList(),
    var likeCount: Int = 0
)
