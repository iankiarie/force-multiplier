package com.ian.forcemultiplier.domain.repository

import com.ian.forcemultiplier.data.remote.dto.ActivityDto
import com.ian.forcemultiplier.data.remote.dto.UserDto
import com.ian.forcemultiplier.util.Resource
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    suspend fun getCurrentUser(): Flow<Resource<UserDto>>
    suspend fun getTeamActivity(): Flow<Resource<List<ActivityDto>>>
    /** Returns a map of ISO date string (yyyy-MM-dd) -> contribution count for the last 140 days */
    suspend fun getContributions(): Flow<Resource<Map<String, Int>>>
}
