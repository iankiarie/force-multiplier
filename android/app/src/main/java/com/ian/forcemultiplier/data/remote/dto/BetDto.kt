package com.ian.forcemultiplier.data.remote.dto

import com.google.gson.annotations.SerializedName

data class BetDto(
    @SerializedName("id") val id: Int,
    @SerializedName("stake") val stake: Int,
    @SerializedName("chosen_outcome") val chosenOutcome: String,
    @SerializedName("settled") val settled: Boolean,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("settled_at") val settledAt: Long?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("prediction_id") val predictionId: Int
)
