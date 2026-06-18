package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface PredictionRepository {
    suspend fun getPredictions(): Flow<Resource<List<PredictionDto>>>
}
