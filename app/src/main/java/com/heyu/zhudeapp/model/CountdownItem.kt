package com.heyu.zhudeapp.model

import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.time.LocalDate

@OptIn(InternalSerializationApi::class)
@Serializable
data class CountdownItem(
    val name: String,
    val month: Int, // 1-12
    val day: Int,     // 1-31
    val dateOverride: String? = null, // Optional display string for dates like lunar holidays
    @Transient var daysRemaining: Long = 0,
    // [NEW] Add a transient field to hold the calculated date for display purposes.
    @Transient var nextDate: LocalDate? = null,
    @Transient var isDeletable: Boolean = true
)
