package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("username") val username: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("role") val role: String? = "user",
    @SerialName("points") val points: Int = 0,
    @SerialName("streak") val streak: Int = 0,
    @SerialName("accuracy") val accuracy: Float = 0f,
    @SerialName("daily_points") val dailyPoints: Int = 0,
    @SerialName("rank") val rank: Int? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
