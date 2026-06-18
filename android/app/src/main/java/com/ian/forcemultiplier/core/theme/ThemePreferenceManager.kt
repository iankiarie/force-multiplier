package com.ian.forcemultiplier.core.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class AppTheme(val label: String) {
    SYSTEM("System default"),
    DARK("Dark"),
    LIGHT("Light")
}

private val Context.themeDataStore: DataStore<Preferences>
    by preferencesDataStore(name = "fm_theme_prefs")

@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val THEME_KEY = stringPreferencesKey("app_theme")

    val themeFlow: Flow<AppTheme> = context.themeDataStore.data.map { prefs ->
        val value = prefs[THEME_KEY] ?: AppTheme.SYSTEM.name
        AppTheme.valueOf(value)
    }

    suspend fun setTheme(theme: AppTheme) {
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = theme.name
        }
    }
}
