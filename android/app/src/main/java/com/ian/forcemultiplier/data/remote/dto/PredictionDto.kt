package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PredictionDto(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String?,
    @SerialName("category") val category: String? = null,
    @SerialName("status") val status: String? = "ACTIVE",
    @SerialName("ends_at") val endsAt: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("resolved_at") val resolvedAt: String? = null,
    @SerialName("winning_option_id") val winningOptionId: String? = null,
    @SerialName("created_by") val createdBy: String?,
    @SerialName("options") val options: List<PredictionOptionDto> = emptyList()
)

@Serializable
data class PredictionOptionDto(
    @SerialName("id") val id: String,
    @SerialName("option_text") val optionText: String,
    @SerialName("prediction_id") val predictionId: String?,
    @SerialName("total_stake") val totalStake: Int = 0
)