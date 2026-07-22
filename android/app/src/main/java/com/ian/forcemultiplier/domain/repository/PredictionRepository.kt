package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface PredictionRepository {
    suspend fun getPredictions(): Flow<Resource<List<PredictionDto>>>
    suspend fun getMyPredictions(userId: String): Flow<Resource<List<PredictionDto>>>
    suspend fun createPrediction(
        title: String,
        description: String?,
        category: String?,
        options: List<String>,
        endsAt: String
    ): Resource<PredictionDto>
    suspend fun resolvePrediction(predictionId: String, winningOptionId: String): Resource<Unit>
    suspend fun getCoinBalance(): Resource<Int>
}
