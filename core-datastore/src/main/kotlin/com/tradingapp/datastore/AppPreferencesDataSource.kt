package com.tradingapp.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesDataSource @Inject constructor(private val dataStore: DataStore<Preferences>) {
    fun themeMode(): Flow<ThemeMode> = dataStore.data
        .map { prefs ->
            val stored = prefs[KEY_THEME_MODE] ?: ThemeMode.SYSTEM.name
            ThemeMode.valueOf(stored)
        }
        .catch { emit(ThemeMode.SYSTEM) }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs -> prefs[KEY_THEME_MODE] = mode.name }
    }

    fun verboseLogging(): Flow<Boolean> = dataStore.data
        .map { prefs -> prefs[KEY_VERBOSE_LOGGING] ?: true }
        .catch { emit(true) }

    suspend fun setVerboseLogging(enabled: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_VERBOSE_LOGGING] = enabled }
    }

    companion object {
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_VERBOSE_LOGGING = booleanPreferencesKey("verbose_logging")
    }
}
