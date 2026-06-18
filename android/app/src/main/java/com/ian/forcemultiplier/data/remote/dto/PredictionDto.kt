package com.ian.forcemultiplier.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PredictionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String?,
    @SerializedName("outcome") val outcome: String?, // team_a, team_b, draw, or null
    @SerializedName("deadline") val deadline: Long, // timestamp in milliseconds
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("resolved_at") val resolvedAt: Long?,
    @SerializedName("created_by") val createdBy: Int,
    @SerializedName("options") val options: List<PredictionOptionDto>
)

data class PredictionOptionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("text") val text: String,
    @SerializedName("prediction_id") val predictionId: Int
)