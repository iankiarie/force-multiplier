package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.UserDao
import com.ian.forcemultiplier.data.mapper.toUserDto
import com.ian.forcemultiplier.data.mapper.toUserEntity
import com.ian.forcemultiplier.data.remote.dto.ActivityDto
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val userDao: UserDao
) : DashboardRepository {

    override suspend fun getCurrentUser(): Flow<Resource<UserDto>> = flow {
        emit(Resource.Loading())

        try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            if (authUser != null) {
                // 1. Try to get from local DB first (fast path)
                val localUser = userDao.getUserById(authUser.id)
                if (localUser != null) {
                    // Always override with the real auth email so the profile
                    // shows the address the user actually signed up with.
                    emit(Resource.Success(localUser.toUserDto().copy(email = authUser.email ?: localUser.email)))
                }

                // 2. Fetch fresh profile data from Supabase
                val remoteUserDto = supabaseClient.postgrest["users"]
                    .select {
                        filter { eq("id", authUser.id) }
                    }.decodeSingle<UserDto>()

                // 3. Always use the auth layer email (source of truth for identity)
                val authEmail = authUser.email ?: remoteUserDto.email

                // 4. Compute rank: count of users with strictly more points + 1
                val usersAhead = try {
                    supabaseClient.postgrest["users"]
                        .select { filter { gt("points", remoteUserDto.points) } }
                        .decodeList<UserDto>().size
                } catch (_: Exception) { null }
                val computedRank = usersAhead?.let { it + 1 }

                val userWithCorrectEmail = remoteUserDto.copy(
                    email = authEmail,
                    rank  = computedRank ?: remoteUserDto.rank
                )

                // 5. Update local DB with corrected data
                userDao.insertUser(userWithCorrectEmail.toUserEntity())

                // 6. Emit fresh data
                emit(Resource.Success(userWithCorrectEmail))
            } else {
                emit(Resource.Error("User not logged in"))
            }
        } catch (e: Exception) {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val localUser = authUser?.let { userDao.getUserById(it.id) }
            if (localUser != null) {
                emit(Resource.Success(localUser.toUserDto().copy(email = authUser?.email ?: localUser.email)))
            } else {
                emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
            }
        }
    }

    override suspend fun getTeamActivity(): Flow<Resource<List<ActivityDto>>> = flow {
        emit(Resource.Loading())
        try {
            val activities = supabaseClient.postgrest["activities"]
                .select {
                    order("created_at", order = Order.DESCENDING)
                    limit(10)
                }.decodeList<ActivityDto>()
            emit(Resource.Success(activities))
        } catch (e: Exception) {
            // Activities table may not exist yet or RLS may block access —
            // fall back to an empty list so the dashboard shows "No recent activity"
            // rather than an error banner.
            emit(Resource.Success(emptyList()))
        }
    }

    // ----- getContributions -------------------------------------------------
    // Fetches all notes for the current user and counts them per calendar day.
    // Returns a map of "yyyy-MM-dd" -> count for the last 140 days.
    override suspend fun getContributions(): Flow<Resource<Map<String, Int>>> = flow {
        emit(Resource.Loading())
        try {
            val authUser = supabaseClient.auth.currentUserOrNull()
            if (authUser == null) {
                emit(Resource.Success(emptyMap()))
                return@flow
            }
            // Re-use the notes table — count records per date on the client side.
            val notes = supabaseClient.postgrest["notes"]
                .select {
                    filter { eq("user_id", authUser.id) }
                }.decodeList<com.ian.forcemultiplier.data.remote.dto.NoteDto>()

            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val counts = mutableMapOf<String, Int>()
            notes.forEach { note ->
                val dateKey = note.createdAt?.let {
                    runCatching {
                        val parser = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        parser.timeZone = java.util.TimeZone.getTimeZone("UTC")
                        fmt.format(parser.parse(it.take(19))!!)
                    }.getOrNull()
                } ?: return@forEach
                counts[dateKey] = (counts[dateKey] ?: 0) + 1
            }
            emit(Resource.Success(counts))
        } catch (e: Exception) {
            emit(Resource.Success(emptyMap())) // non-fatal — heatmap will just show empty
        }
    }
}
