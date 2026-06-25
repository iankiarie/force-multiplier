package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BetDto(
    @SerialName("id") val id: String,
    @SerialName("stake") val stake: Int,
    @SerialName("chosen_outcome") val chosenOutcome: String,
    @SerialName("settled") val settled: Boolean,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("settled_at") val settledAt: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("prediction_id") val predictionId: String
)
