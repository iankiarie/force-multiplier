package com.ian.forcemultiplier.di

import com.ian.forcemultiplier.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = Constants.SUPABASE_URL,
            supabaseKey = Constants.SUPABASE_ANON_KEY
        ) {
            install(Auth) {
                flowType = FlowType.PKCE
                scheme = Constants.AUTH_CALLBACK_SCHEME
                host = Constants.AUTH_CALLBACK_HOST
                defaultRedirectUrl = "${Constants.AUTH_CALLBACK_SCHEME}://${Constants.AUTH_CALLBACK_HOST}"
            }
            install(Postgrest)
            install(Storage)
        }
    }
}
