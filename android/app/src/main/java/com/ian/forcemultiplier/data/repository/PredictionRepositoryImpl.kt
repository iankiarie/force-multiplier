package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.PredictionDao
import com.ian.forcemultiplier.data.remote.api.PredictionsApi
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor(
    private val api: PredictionsApi,
    private val dao: PredictionDao
) : PredictionRepository {

    override suspend fun getPredictions(): Flow<Resource<List<PredictionDto>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getPredictions()
            if (response.isSuccessful) {
                emit(Resource.Success(response.body() ?: emptyList()))
            } else {
                emit(Resource.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
