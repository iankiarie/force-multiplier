package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.UserDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthApi {
    @FormUrlEncoded
    @POST("auth/token")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    @POST("auth/users/")
    suspend fun register(@Body userDto: UserDto): Response<UserDto>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<UserDto>

    data class AuthResponse(
        val access_token: String,
        val token_type: String
    )
}