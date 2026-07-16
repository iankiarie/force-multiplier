package com.ian.forcemultiplier.di

import com.ian.forcemultiplier.data.repository.CollaborationRepositoryImpl
import com.ian.forcemultiplier.domain.repository.CollaborationRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CollaborationModule {

    @Binds
    @Singleton
    abstract fun bindCollaborationRepository(
        impl: CollaborationRepositoryImpl
    ): CollaborationRepository
}
