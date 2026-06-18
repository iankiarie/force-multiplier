package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.BetDto
import retrofit2.Response
import retrofit2.http.*

interface BetsApi {
    @GET("bets/")
    suspend fun getBets(): Response<List<BetDto>>

    @POST("bets/")
    suspend fun placeBet(@Body betDto: BetDto): Response<BetDto>

    @GET("bets/user/{userId}")
    suspend fun getBetsByUser(@Path("userId") userId: Int): Response<List<BetDto>>
}
