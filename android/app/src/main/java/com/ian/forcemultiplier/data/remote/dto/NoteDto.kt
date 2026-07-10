package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("content") val content: String?,
    @SerialName("tags") val tags: String? = null,
    @SerialName("preview") val preview: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("icon") val icon: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("is_bookmarked") val isBookmarked: Boolean = false,
    @SerialName("folder_id") val folderId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("user_id") val userId: String?
)
