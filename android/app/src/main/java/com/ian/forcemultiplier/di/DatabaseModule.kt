package com.ian.forcemultiplier.di

import android.content.Context
import com.ian.forcemultiplier.data.local.dao.*
import com.ian.forcemultiplier.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun providePredictionDao(database: AppDatabase): PredictionDao = database.predictionDao()

    @Provides
    fun provideBetDao(database: AppDatabase): BetDao = database.betDao()

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideNoteDao(database: AppDatabase): NoteDao = database.noteDao()
}
