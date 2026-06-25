package com.ian.forcemultiplier.domain.model

data class Prediction(
    val id: String,
    val title: String,
    val description: String?,
    val category: String?,
    val createdBy: String,
    val endsAt: Long,
    val resolvedAt: Long?,
    val winningOptionId: String?,
    val status: PredictionStatus,
    val options: List<PredictionOption>,
    val createdAt: Long
)

data class PredictionOption(
    val id: String,
    val predictionId: String,
    val optionText: String,
    val totalStake: Int
)

enum class PredictionStatus {
    ACTIVE, RESOLVED, CANCELLED
}
