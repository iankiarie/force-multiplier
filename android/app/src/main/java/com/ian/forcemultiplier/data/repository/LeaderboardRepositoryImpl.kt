package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.LeaderboardRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LeaderboardRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient
) : LeaderboardRepository {

    override suspend fun getLeaderboard(period: String): Flow<Resource<List<UserDto>>> = flow {
        emit(Resource.Loading())
        try {
            val leaderboard = supabaseClient.postgrest["users"]
                .select {
                    order("points", order = Order.DESCENDING)
                    limit(20)
                }.decodeList<UserDto>()
            emit(Resource.Success(leaderboard))
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Failed to load leaderboard"))
        }
    }
}
