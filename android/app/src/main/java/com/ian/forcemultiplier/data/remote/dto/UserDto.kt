package com.ian.forcemultiplier.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UserDto(
    @SerializedName("id") val id: Int,
    @SerializedName("email") val email: String,
    @SerializedName("username") val username: String,
    @SerializedName("full_name") val fullName: String?,
    @SerializedName("is_active") val isActive: Boolean,
    @SerializedName("role") val role: String,
    @SerializedName("points") val points: Int,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long?
)