package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NoteCommentDto(
    val id: String? = null,
    @SerialName("note_id")    val noteId: String,
    @SerialName("user_id")    val userId: String? = null,
    val content: String,
    val resolved: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)
