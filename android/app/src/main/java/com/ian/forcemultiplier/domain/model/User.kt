package com.ian.forcemultiplier.domain.model

/**
 * User domain model representing a user in the system.
 */
data class User(
    val id: Int,
    val email: String,
    val username: String,
    val fullName: String?,
    val isActive: Boolean,
    val role: String, // In a real app, we might use an enum or a sealed class for role.
    val points: Int, // Current points balance (sum of transactions)
    val createdAt: Long, // Timestamp in milliseconds
    val updatedAt: Long? // Timestamp in milliseconds
)