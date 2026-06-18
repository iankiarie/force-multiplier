package com.ian.forcemultiplier.di

import com.ian.forcemultiplier.data.repository.DashboardRepositoryImpl
import com.ian.forcemultiplier.data.repository.LeaderboardRepositoryImpl
import com.ian.forcemultiplier.data.repository.NoteRepositoryImpl
import com.ian.forcemultiplier.data.repository.PredictionRepositoryImpl
import com.ian.forcemultiplier.domain.repository.DashboardRepository
import com.ian.forcemultiplier.domain.repository.LeaderboardRepository
import com.ian.forcemultiplier.domain.repository.NoteRepository
import com.ian.forcemultiplier.domain.repository.PredictionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(
        dashboardRepositoryImpl: DashboardRepositoryImpl
    ): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindPredictionRepository(
        predictionRepositoryImpl: PredictionRepositoryImpl
    ): PredictionRepository

    @Binds
    @Singleton
    abstract fun bindLeaderboardRepository(
        leaderboardRepositoryImpl: LeaderboardRepositoryImpl
    ): LeaderboardRepository

    @Binds
    @Singleton
    abstract fun bindNoteRepository(
        noteRepositoryImpl: NoteRepositoryImpl
    ): NoteRepository
}
