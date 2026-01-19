/**
 * @file MapViewModel.kt
 * @description ViewModel for map screen with game state polling and fog of war
 * @session SESSION_003
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.music.subnauticamap.data.api.BeaconInfo
import com.music.music.subnauticamap.data.api.PlayerInfo
import com.music.music.subnauticamap.data.api.TimeInfo
import com.music.music.subnauticamap.data.api.VehicleInfo
import com.music.music.subnauticamap.data.repository.ApiResult
import com.music.music.subnauticamap.data.repository.ExplorationStats
import com.music.music.subnauticamap.data.repository.FogOfWarRepository
import com.music.music.subnauticamap.data.repository.GameStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Connection status for the map screen
 */
sealed class MapConnectionStatus {
    data object Connected : MapConnectionStatus()
    data object WaitingForGame : MapConnectionStatus()
    data class Error(val message: String) : MapConnectionStatus()
    data object Disconnected : MapConnectionStatus()
}

/**
 * UI state for map screen
 */
data class MapUiState(
    val connectionStatus: MapConnectionStatus = MapConnectionStatus.Disconnected,
    val player: PlayerInfo? = null,
    val time: TimeInfo? = null,
    val beacons: List<BeaconInfo> = emptyList(),
    val vehicles: List<VehicleInfo> = emptyList(),
    val lastUpdateTimestamp: Long = 0,
    val centerOnPlayerTrigger: Long = 0,
    // Fog of war state
    val fogOfWarEnabled: Boolean = true,
    val exploredChunks: Set<Long> = emptySet(),
    val explorationStats: ExplorationStats? = null,
    // Flag to track if we received at least one successful data update
    val hasReceivedData: Boolean = false
)

/**
 * ViewModel for map screen
 */
class MapViewModel(
    private val gameStateRepository: GameStateRepository,
    private val fogOfWarRepository: FogOfWarRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }

    init {
        // Load fog of war state
        viewModelScope.launch {
            val enabled = fogOfWarRepository.fogOfWarEnabled.first()
            val chunks = fogOfWarRepository.loadExploredChunks()
            val stats = fogOfWarRepository.getExplorationStats(chunks)

            _uiState.value = _uiState.value.copy(
                fogOfWarEnabled = enabled,
                exploredChunks = chunks,
                explorationStats = stats
            )
        }

        // Observe fog of war enabled changes
        viewModelScope.launch {
            fogOfWarRepository.fogOfWarEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(fogOfWarEnabled = enabled)
            }
        }
    }

    /**
     * Start polling for game state updates
     */
    fun startPolling() {
        if (pollingJob?.isActive == true) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                fetchGameState()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Stop polling
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Request to center map on player position
     */
    fun centerOnPlayer() {
        _uiState.value = _uiState.value.copy(
            centerOnPlayerTrigger = System.currentTimeMillis()
        )
    }

    /**
     * Toggle fog of war on/off
     */
    fun toggleFogOfWar() {
        viewModelScope.launch {
            val newEnabled = !_uiState.value.fogOfWarEnabled
            fogOfWarRepository.setFogOfWarEnabled(newEnabled)
        }
    }

    /**
     * Reset fog of war (clear all explored areas)
     */
    fun resetFogOfWar() {
        viewModelScope.launch {
            fogOfWarRepository.clearExploredChunks()
            _uiState.value = _uiState.value.copy(
                exploredChunks = emptySet(),
                explorationStats = fogOfWarRepository.getExplorationStats(emptySet())
            )
        }
    }

    private suspend fun fetchGameState() {
        when (val result = gameStateRepository.getGameState()) {
            is ApiResult.Success -> {
                val state = result.data
                val currentState = _uiState.value

                // Update explored chunks if player position available
                var newExploredChunks = currentState.exploredChunks
                state.player?.let { player ->
                    if (currentState.fogOfWarEnabled) {
                        newExploredChunks = fogOfWarRepository.exploreAtPosition(
                            player.position.xFloat,
                            player.position.zFloat,
                            currentState.exploredChunks
                        )
                    }
                }

                val stats = if (newExploredChunks != currentState.exploredChunks) {
                    fogOfWarRepository.getExplorationStats(newExploredChunks)
                } else {
                    currentState.explorationStats
                }

                _uiState.value = currentState.copy(
                    connectionStatus = MapConnectionStatus.Connected,
                    player = state.player,
                    time = state.time,
                    beacons = state.beacons,
                    vehicles = state.vehicles,
                    lastUpdateTimestamp = state.timestamp,
                    exploredChunks = newExploredChunks,
                    explorationStats = stats,
                    hasReceivedData = true
                )
            }
            is ApiResult.Error -> {
                val status = if (result.code == 503) {
                    MapConnectionStatus.WaitingForGame
                } else {
                    MapConnectionStatus.Error(result.message)
                }
                _uiState.value = _uiState.value.copy(
                    connectionStatus = status,
                    player = null
                )
            }
            is ApiResult.Loading -> { /* Ignore */ }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}

/**
 * Factory for creating MapViewModel with repositories
 */
class MapViewModelFactory(
    private val gameStateRepository: GameStateRepository,
    private val fogOfWarRepository: FogOfWarRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(gameStateRepository, fogOfWarRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
