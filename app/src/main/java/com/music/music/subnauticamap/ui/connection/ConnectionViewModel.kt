/**
 * @file ConnectionViewModel.kt
 * @description ViewModel for connection screen state management
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.music.music.subnauticamap.data.repository.ApiResult
import com.music.music.subnauticamap.data.repository.ConnectionSettings
import com.music.music.subnauticamap.data.repository.GameStateRepository
import com.music.music.subnauticamap.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Connection state
 */
sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Connecting : ConnectionState()
    data class Connected(val version: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/**
 * UI state for connection screen
 */
data class ConnectionUiState(
    val ipAddress: String = "",
    val port: String = "63030",
    val keepScreenOn: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.Idle,
    val isInputValid: Boolean = false
)

/**
 * ViewModel for managing connection screen
 */
class ConnectionViewModel(application: Application) : AndroidViewModel(application) {

    val settingsRepository = SettingsRepository(application)
    val gameStateRepository = GameStateRepository()

    private val _uiState = MutableStateFlow(ConnectionUiState())
    val uiState: StateFlow<ConnectionUiState> = _uiState.asStateFlow()

    init {
        loadSavedSettings()
    }

    private fun loadSavedSettings() {
        viewModelScope.launch {
            val settings = settingsRepository.connectionSettings.first()
            _uiState.value = _uiState.value.copy(
                ipAddress = settings.ipAddress,
                port = settings.port.toString(),
                keepScreenOn = settings.keepScreenOn,
                isInputValid = isValidInput(settings.ipAddress, settings.port.toString())
            )
        }
    }

    /**
     * Update IP address input
     */
    fun onIpAddressChange(ip: String) {
        _uiState.value = _uiState.value.copy(
            ipAddress = ip,
            isInputValid = isValidInput(ip, _uiState.value.port),
            connectionState = ConnectionState.Idle
        )
    }

    /**
     * Update port input
     */
    fun onPortChange(port: String) {
        // Only allow digits
        val filteredPort = port.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(
            port = filteredPort,
            isInputValid = isValidInput(_uiState.value.ipAddress, filteredPort),
            connectionState = ConnectionState.Idle
        )
    }

    /**
     * Attempt to connect to the server
     */
    fun connect() {
        val state = _uiState.value
        if (!state.isInputValid) return

        val port = state.port.toIntOrNull() ?: return

        _uiState.value = state.copy(connectionState = ConnectionState.Connecting)

        viewModelScope.launch {
            // Save settings
            settingsRepository.saveConnectionSettings(state.ipAddress, port)

            // Configure repository
            gameStateRepository.configure(state.ipAddress, port)

            // Test connection
            when (val result = gameStateRepository.testConnection()) {
                is ApiResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.Connected(result.data.version)
                    )
                }
                is ApiResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        connectionState = ConnectionState.Error(result.message)
                    )
                }
                is ApiResult.Loading -> { /* Already handled */ }
            }
        }
    }

    /**
     * Reset connection state
     */
    fun disconnect() {
        _uiState.value = _uiState.value.copy(connectionState = ConnectionState.Idle)
    }

    /**
     * Toggle keep screen on setting
     */
    fun onKeepScreenOnChange(keepScreenOn: Boolean) {
        _uiState.value = _uiState.value.copy(keepScreenOn = keepScreenOn)
        viewModelScope.launch {
            settingsRepository.saveKeepScreenOn(keepScreenOn)
        }
    }

    private fun isValidInput(ip: String, port: String): Boolean {
        if (ip.isBlank()) return false
        if (port.isBlank()) return false

        val portNum = port.toIntOrNull() ?: return false
        if (portNum < 1 || portNum > 65535) return false

        // Basic IP validation (allow hostnames too)
        val ipPattern = Regex("""^(\d{1,3}\.){3}\d{1,3}$|^[a-zA-Z0-9.-]+$""")
        return ipPattern.matches(ip)
    }
}
