package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.local.dao.UserDao
import com.ian.forcemultiplier.data.remote.api.AuthApi
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class DashboardRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val userDao: UserDao
) : DashboardRepository {

    override suspend fun getCurrentUser(): Flow<Resource<UserDto>> = flow {
        emit(Resource.Loading())
        
        try {
            val response = authApi.getCurrentUser()
            if (response.isSuccessful) {
                response.body()?.let { user ->
                    emit(Resource.Success(user))
                } ?: emit(Resource.Error("Empty user data"))
            } else {
                emit(Resource.Error(response.message()))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Unknown error"))
        }
    }
}
