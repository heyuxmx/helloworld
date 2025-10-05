package com.heyu.zhudeapp.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class CountdownItem(
    val name: String,
    val month: Int, // 1-12
    val day: Int,     // 1-31
    val dateOverride: String? = null, // Optional display string for dates like lunar holidays
    @Transient var daysRemaining: Long = 0
)
