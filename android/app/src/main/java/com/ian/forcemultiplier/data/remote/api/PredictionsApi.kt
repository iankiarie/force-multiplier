package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.PredictionDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface PredictionsApi {
    @GET("predictions/")
    suspend fun getPredictions(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 100
    ): Response<List<PredictionDto>>

    @GET("predictions/{predictionId}")
    suspend fun getPredictionById(
        @Path("predictionId") predictionId: Int
    ): Response<PredictionDto>

    @POST("predictions/")
    suspend fun createPrediction(@Body predictionDto: PredictionDto): Response<PredictionDto>

    @PATCH("predictions/{predictionId}")
    suspend fun updatePrediction(
        @Path("predictionId") predictionId: Int,
        @Body predictionDto: PredictionDto
    ): Response<PredictionDto>
}