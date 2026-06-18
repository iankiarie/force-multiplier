package com.ian.forcemultiplier.data.remote.api

import com.ian.forcemultiplier.data.remote.dto.TransactionDto
import retrofit2.Response
import retrofit2.http.*

interface TransactionsApi {
    @GET("transactions/")
    suspend fun getTransactions(): Response<List<TransactionDto>>

    @GET("transactions/user/{userId}")
    suspend fun getTransactionsByUser(@Path("userId") userId: Int): Response<List<TransactionDto>>
}
