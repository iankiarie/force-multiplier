package com.ian.forcemultiplier.di

import android.content.Context
import com.ian.forcemultiplier.core.session.SessionManager
import com.ian.forcemultiplier.core.theme.ThemePreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CoreModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideThemePreferenceManager(
        @ApplicationContext context: Context
    ): ThemePreferenceManager = ThemePreferenceManager(context)
}
