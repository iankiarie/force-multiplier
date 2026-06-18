package com.ian.forcemultiplier.domain.model

/**
 * Prediction domain model representing a prediction that users can bet on.
 */
data class Prediction(
    val id: Int,
    val title: String,
    val description: String?,
    val outcome: String?, // team_a, team_b, draw, or null if not resolved
    val deadline: Long, // Timestamp in milliseconds
    val createdAt: Long,
    val resolvedAt: Long?,
    val createdBy: Int // User ID of the creator
)