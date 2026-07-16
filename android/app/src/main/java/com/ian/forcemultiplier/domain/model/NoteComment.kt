package com.ian.forcemultiplier.domain.model

data class NoteComment(
    val id: String,
    val noteId: String,
    val userId: String,
    val content: String,
    val resolved: Boolean,
    val createdAt: String?
)
