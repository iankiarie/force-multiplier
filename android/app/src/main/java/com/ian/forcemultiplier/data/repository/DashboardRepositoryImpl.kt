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
                // 1. Try to get from local DB first
                val localUser = userDao.getUserById(authUser.id)
                if (localUser != null) {
                    emit(Resource.Success(localUser.toUserDto()))
                }

                // 2. Fetch fresh data from Supabase
                val remoteUserDto = supabaseClient.postgrest["users"]
                    .select {
                        filter {
                            eq("id", authUser.id)
                        }
                    }.decodeSingle<UserDto>()
                
                // 3. Update local DB
                userDao.insertUser(remoteUserDto.toUserEntity())
                
                // 4. Emit fresh data
                emit(Resource.Success(remoteUserDto))
            } else {
                emit(Resource.Error("User not logged in"))
            }
        } catch (e: Exception) {
            val authUser = supabaseClient.auth.currentUserOrNull()
            val localUser = authUser?.let { userDao.getUserById(it.id) }
            if (localUser != null) {
                emit(Resource.Success(localUser.toUserDto()))
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
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
