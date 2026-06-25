package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.domain.model.Prediction
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

/**
 * Read-side repository for predictions.
 */
interface PredictionQueryRepository {
    fun getActivePredictions(): Flow<Resource<List<Prediction>>>
    fun getPredictionById(id: String): Flow<Resource<Prediction>>
    fun getPredictionHistory(userId: String): Flow<Resource<List<Prediction>>>
}

/**
 * Write-side repository for predictions.
 */
interface PredictionCommandRepository {
    suspend fun createPrediction(prediction: Prediction): Resource<Unit>
    suspend fun placeBet(predictionId: String, optionId: String, amount: Int): Resource<Unit>
    suspend fun resolvePrediction(predictionId: String, winningOptionId: String): Resource<Unit>
}
