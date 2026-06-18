package com.ian.forcemultiplier.data.repository

import com.ian.forcemultiplier.data.remote.api.LeaderboardApi
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.domain.repository.LeaderboardRepository
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class LeaderboardRepositoryImpl @Inject constructor(
    private val api: LeaderboardApi
) : LeaderboardRepository {

    override suspend fun getLeaderboard(period: String): Flow<Resource<List<UserDto>>> = flow {
        emit(Resource.Loading())
        try {
            val response = api.getLeaderboard(period)
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
