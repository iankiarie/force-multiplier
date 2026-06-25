package com.ian.forcemultiplier.domain.model

/**
 * User domain model representing a user in the system.
 */
data class User(
    val id: String, // UUID from Auth
    val email: String,
    val username: String?,
    val fullName: String?,
    val avatarUrl: String?,
    val organizationId: String?,
    val isActive: Boolean,
    val role: UserRole,
    val points: Int,
    val createdAt: Long,
    val updatedAt: Long?
)

enum class UserRole {
    EMPLOYEE, MANAGER, ADMIN, SUPER_ADMIN
}
