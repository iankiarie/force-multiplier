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
            
            // Seeding/Ensuring specific top users for Demo
            val seededUsers = listOf(
                UserDto(id = "1", email = "jeddy@example.com", username = "jeddy", fullName = "Jeddy Awuor", points = 1250, role = "admin", streak = 15, accuracy = 0.92f),
                UserDto(id = "2", email = "roy@example.com", username = "roy", fullName = "Roy Okite", points = 1100, role = "user", streak = 8, accuracy = 0.85f),
                UserDto(id = "3", email = "daniel@example.com", username = "daniel", fullName = "Daniel Kariuki", points = 950, role = "user", streak = 12, accuracy = 0.88f)
            )
            
            val finalLeaderboard = (seededUsers + leaderboard.filter { it.fullName !in seededUsers.map { u -> u.fullName } })
                .sortedByDescending { it.points }
            
            emit(Resource.Success(finalLeaderboard))
        } catch (e: Exception) {
            // Fallback to seeded data if network fails
            val seededUsers = listOf(
                UserDto(id = "1", email = "jeddy@example.com", username = "jeddy", fullName = "Jeddy Awuor", points = 1250, role = "admin", streak = 15, accuracy = 0.92f),
                UserDto(id = "2", email = "roy@example.com", username = "roy", fullName = "Roy Okite", points = 1100, role = "user", streak = 8, accuracy = 0.85f),
                UserDto(id = "3", email = "daniel@example.com", username = "daniel", fullName = "Daniel Kariuki", points = 950, role = "user", streak = 12, accuracy = 0.88f)
            )
            emit(Resource.Success(seededUsers))
        }
    }
}
