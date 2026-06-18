package com.ian.forcemultiplier.data.remote.dto

import com.google.gson.annotations.SerializedName

data class NoteDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("tags") val tags: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("updated_at") val updatedAt: Long?,
    @SerializedName("user_id") val userId: Int
)
