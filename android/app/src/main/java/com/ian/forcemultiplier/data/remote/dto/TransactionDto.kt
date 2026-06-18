package com.ian.forcemultiplier.data.remote.dto

import com.google.gson.annotations.SerializedName

data class TransactionDto(
    @SerializedName("id") val id: Int,
    @SerializedName("amount") val amount: Int,
    @SerializedName("type") val type: String,
    @SerializedName("description") val description: String?,
    @SerializedName("created_at") val createdAt: Long,
    @SerializedName("user_id") val userId: Int
)
