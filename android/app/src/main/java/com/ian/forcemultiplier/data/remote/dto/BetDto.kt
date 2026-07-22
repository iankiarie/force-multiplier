package com.ian.forcemultiplier.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BetDto(
    @SerialName("id")            val id: String? = null,
    @SerialName("user_id")       val userId: String,
    @SerialName("prediction_id") val predictionId: String,
    @SerialName("option_id")     val optionId: String,
    @SerialName("amount")        val amount: Int,
    @SerialName("created_at")    val createdAt: String? = null
)
