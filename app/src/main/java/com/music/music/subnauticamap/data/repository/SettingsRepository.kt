/**
 * @file SettingsRepository.kt
 * @description DataStore repository for persisting app settings
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Connection settings data class
 */
data class ConnectionSettings(
    val ipAddress: String = "",
    val port: Int = 63030,
    val keepScreenOn: Boolean = true
)

/**
 * Repository for managing app settings persistence
 */
class SettingsRepository(private val context: Context) {

    companion object {
        private val IP_ADDRESS_KEY = stringPreferencesKey("ip_address")
        private val PORT_KEY = intPreferencesKey("port")
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        const val DEFAULT_PORT = 63030
    }

    /**
     * Flow of current connection settings
     */
    val connectionSettings: Flow<ConnectionSettings> = context.dataStore.data
        .map { preferences ->
            ConnectionSettings(
                ipAddress = preferences[IP_ADDRESS_KEY] ?: "",
                port = preferences[PORT_KEY] ?: DEFAULT_PORT,
                keepScreenOn = preferences[KEEP_SCREEN_ON_KEY] ?: true
            )
        }

    /**
     * Save connection settings
     *
     * @param ipAddress IP address of the PC
     * @param port Port number
     */
    suspend fun saveConnectionSettings(ipAddress: String, port: Int) {
        context.dataStore.edit { preferences ->
            preferences[IP_ADDRESS_KEY] = ipAddress
            preferences[PORT_KEY] = port
        }
    }

    /**
     * Save keep screen on setting
     *
     * @param keepScreenOn Whether to keep screen on
     */
    suspend fun saveKeepScreenOn(keepScreenOn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] = keepScreenOn
        }
    }

    /**
     * Clear saved connection settings
     */
    suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
