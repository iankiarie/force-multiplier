package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface LeaderboardRepository {
    suspend fun getLeaderboard(period: String): Flow<Resource<List<UserDto>>>
}
