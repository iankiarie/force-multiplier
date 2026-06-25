package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.PredictionDao
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dao: PredictionDao
) : PredictionRepository {

    override suspend fun getPredictions(): Flow<Resource<List<PredictionDto>>> = flow {
        emit(Resource.Loading())
        try {
            val predictions = supabaseClient.postgrest["predictions"]
                .select().decodeList<PredictionDto>()
            emit(Resource.Success(predictions))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
