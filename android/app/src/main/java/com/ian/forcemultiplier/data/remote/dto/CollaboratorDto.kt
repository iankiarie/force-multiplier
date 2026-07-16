package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CollaboratorDto(
    val id: String? = null,
    @SerialName("note_id")     val noteId: String? = null,
    @SerialName("user_id")     val userId: String? = null,
    @SerialName("invited_email") val invitedEmail: String? = null,
    val role: String = "viewer",
    @SerialName("invited_by")  val invitedBy: String? = null,
    val accepted: Boolean = false,
    @SerialName("created_at")  val createdAt: String? = null
)
