package com.ian.forcemultiplier.domain.model

/**
 * Bet domain model representing a bet placed by a user on a prediction.
 */
data class Bet(
    val id: Int,
    val stake: Int, // Amount staked
    val chosenOutcome: String, // team_a, team_b, draw
    val settled: Boolean,
    val createdAt: Long, // Timestamp in milliseconds
    val settledAt: Long?, // Timestamp in milliseconds
    val userId: Int, // User who placed the bet
    val predictionId: Int // Prediction the bet is placed on
)