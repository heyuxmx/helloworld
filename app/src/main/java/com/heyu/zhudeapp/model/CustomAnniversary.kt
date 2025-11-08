package com.heyu.zhudeapp.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlin.OptIn

@OptIn(InternalSerializationApi::class)
@Serializable
data class CustomAnniversary(
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val isLunar: Boolean = false
)
