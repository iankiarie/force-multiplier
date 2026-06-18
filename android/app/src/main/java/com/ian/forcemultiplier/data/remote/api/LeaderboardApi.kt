package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface LeaderboardApi {
    @GET("leaderboard/")
    suspend fun getLeaderboard(
        @Query("period") period: String = "monthly" // weekly, monthly, quarterly, yearly, all_time
    ): Response<List<UserDto>>
}
