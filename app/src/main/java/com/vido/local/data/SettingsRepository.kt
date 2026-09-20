package com.vido.local.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("settings")

enum class PlayerMode {
    IN_APP,
    SYSTEM,
}

class SettingsRepository(private val context: Context) {
    private val extensionsKey = stringSetPreferencesKey("private_extensions")
    private val privateThumbnailsKey = booleanPreferencesKey("private_thumbnails")
    private val autoPlayNextKey = booleanPreferencesKey("auto_play_next")
    private val playerModeKey = stringPreferencesKey("player_mode")

    private val defaultPrivateExtensions = emptySet<String>()

    val privateExtensions: Flow<Set<String>> = context.settingsDataStore.data.map { preferences ->
        preferences[extensionsKey] ?: defaultPrivateExtensions
    }
    val showPrivateThumbnails: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[privateThumbnailsKey] ?: false
    }
    val autoPlayNext: Flow<Boolean> = context.settingsDataStore.data.map { preferences ->
        preferences[autoPlayNextKey] ?: false
    }
    val playerMode: Flow<PlayerMode> = context.settingsDataStore.data.map { preferences ->
        runCatching { PlayerMode.valueOf(preferences[playerModeKey] ?: PlayerMode.IN_APP.name) }
            .getOrDefault(PlayerMode.IN_APP)
    }

    suspend fun addExtension(value: String): Boolean {
        val normalized = value.trim().removePrefix(".").lowercase()
        if (normalized.isBlank() || normalized.any { !it.isLetterOrDigit() }) return false
        context.settingsDataStore.edit { preferences ->
            preferences[extensionsKey] = (preferences[extensionsKey] ?: defaultPrivateExtensions) + normalized
        }
        return true
    }

    suspend fun removeExtension(value: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[extensionsKey] = (preferences[extensionsKey] ?: defaultPrivateExtensions) - value
        }
    }

    suspend fun setShowPrivateThumbnails(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[privateThumbnailsKey] = enabled
        }
    }

    suspend fun setAutoPlayNext(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[autoPlayNextKey] = enabled
        }
    }

    suspend fun setPlayerMode(mode: PlayerMode) {
        context.settingsDataStore.edit { preferences ->
            preferences[playerModeKey] = mode.name
        }
    }
}
