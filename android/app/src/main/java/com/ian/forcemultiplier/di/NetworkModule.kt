package com.ian.forcemultiplier.di

import com.ian.forcemultiplier.core.session.SessionManager
import com.ian.forcemultiplier.data.remote.api.*
import com.ian.forcemultiplier.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(sessionManager: SessionManager): Interceptor {
        return Interceptor { chain ->
            val original = chain.request()
            val token = sessionManager.getAuthToken()
            
            if (token != null) {
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            } else {
                chain.proceed(original)
            }
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun providePredictionsApi(retrofit: Retrofit): PredictionsApi = retrofit.create(PredictionsApi::class.java)

    @Provides
    @Singleton
    fun provideNotesApi(retrofit: Retrofit): NotesApi = retrofit.create(NotesApi::class.java)

    @Provides
    @Singleton
    fun provideBetsApi(retrofit: Retrofit): BetsApi = retrofit.create(BetsApi::class.java)

    @Provides
    @Singleton
    fun provideTransactionsApi(retrofit: Retrofit): TransactionsApi = retrofit.create(TransactionsApi::class.java)

    @Provides
    @Singleton
    fun provideLeaderboardApi(retrofit: Retrofit): LeaderboardApi = retrofit.create(LeaderboardApi::class.java)
}
