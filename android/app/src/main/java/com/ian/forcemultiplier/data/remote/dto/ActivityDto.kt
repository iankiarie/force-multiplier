package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActivityDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("type") val type: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_id") val userId: String? = null
)
