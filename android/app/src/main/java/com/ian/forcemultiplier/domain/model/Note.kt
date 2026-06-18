package com.ian.forcemultiplier.domain.model

/**
 * Note domain model representing a note in the mind vault.
 */
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val tags: List<String>, // We'll use a list of strings for tags
    val createdAt: Long, // Timestamp in milliseconds
    val updatedAt: Long? // Timestamp in milliseconds
)