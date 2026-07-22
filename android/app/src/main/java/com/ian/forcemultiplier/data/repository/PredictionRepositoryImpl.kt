package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.PredictionDao
import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import com.ian.forcemultiplier.data.remote.dto.PredictionOptionDto
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperation
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class PredictionRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val dao: PredictionDao
) : PredictionRepository {

    override suspend fun getPredictions(): Flow<Resource<List<PredictionDto>>> = flow {
        emit(Resource.Loading())
        try {
            // Fetch predictions then options separately to avoid nested join issues
            val predictions = supabaseClient.postgrest["predictions"]
                .select().decodeList<PredictionDto>()

            val withOptions = predictions.map { pred ->
                val opts = try {
                    supabaseClient.postgrest["prediction_options"]
                        .select {
                            filter {
                                eq("prediction_id", pred.id)
                            }
                        }.decodeList<PredictionOptionDto>()
                } catch (_: Exception) { emptyList() }
                pred.copy(options = opts)
            }
            emit(Resource.Success(withOptions))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun getMyPredictions(userId: String): Flow<Resource<List<PredictionDto>>> = flow {
        emit(Resource.Loading())
        try {
            val predictions = supabaseClient.postgrest["predictions"]
                .select {
                    filter { eq("created_by", userId) }
                }.decodeList<PredictionDto>()

            val withOptions = predictions.map { pred ->
                val opts = try {
                    supabaseClient.postgrest["prediction_options"]
                        .select {
                            filter { eq("prediction_id", pred.id) }
                        }.decodeList<PredictionOptionDto>()
                } catch (_: Exception) { emptyList() }
                pred.copy(options = opts)
            }
            emit(Resource.Success(withOptions.sortedByDescending { it.createdAt }))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }

    override suspend fun createPrediction(
        title: String,
        description: String?,
        category: String?,
        options: List<String>,
        endsAt: String
    ): Resource<PredictionDto> {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return Resource.Error("Not logged in")

            // Insert prediction — { select() } forces PostgREST to return the row
            // (default is 204 No Content; decodeSingle throws without it)
            val pred = supabaseClient.postgrest["predictions"].insert(
                buildJsonObject {
                    put("title",       title)
                    put("description", description)
                    put("category",    category)
                    put("created_by",  user.id)
                    put("ends_at",     endsAt)
                    put("status",      "ACTIVE")
                }
            ) { select() }.decodeSingle<PredictionDto>()

            // Insert each option
            options.forEach { optText ->
                supabaseClient.postgrest["prediction_options"].insert(
                    buildJsonObject {
                        put("prediction_id", pred.id)
                        put("option_text",   optText)
                    }
                )
            }

            // Return prediction with options attached
            val opts = supabaseClient.postgrest["prediction_options"]
                .select { filter { eq("prediction_id", pred.id) } }
                .decodeList<PredictionOptionDto>()

            Resource.Success(pred.copy(options = opts))
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to create prediction")
        }
    }

    override suspend fun resolvePrediction(
        predictionId: String,
        winningOptionId: String
    ): Resource<Unit> {
        return try {
            supabaseClient.postgrest.rpc(
                "resolve_prediction",
                JsonObject(mapOf(
                    "p_prediction_id"      to JsonPrimitive(predictionId),
                    "p_winning_option_id"  to JsonPrimitive(winningOptionId)
                ))
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to resolve prediction")
        }
    }

    override suspend fun getCoinBalance(): Resource<Int> {
        return try {
            val user = supabaseClient.auth.currentUserOrNull()
                ?: return Resource.Error("Not logged in")

            val rows = supabaseClient.postgrest["user_profiles"]
                .select { filter { eq("id", user.id) } }
                .decodeList<kotlinx.serialization.json.JsonObject>()

            val balance = rows.firstOrNull()
                ?.get("coin_balance")
                ?.let { (it as? JsonPrimitive)?.content?.toIntOrNull() } ?: 100
            Resource.Success(balance)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Failed to fetch balance")
        }
    }
}
