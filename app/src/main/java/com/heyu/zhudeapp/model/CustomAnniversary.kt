package com.heyu.zhudeapp.model

import kotlinx.serialization.Serializable

@Serializable
data class CustomAnniversary(
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val isLunar: Boolean = false
)
