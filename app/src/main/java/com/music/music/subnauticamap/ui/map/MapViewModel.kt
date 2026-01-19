/**
 * @file MapViewModel.kt
 * @description ViewModel for map screen with game state polling and multi-layer fog of war
 * @session SESSION_005
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.music.subnauticamap.data.api.BeaconInfo
import com.music.music.subnauticamap.data.api.MapLayer
import com.music.music.subnauticamap.data.api.PlayerInfo
import com.music.music.subnauticamap.data.api.TimeInfo
import com.music.music.subnauticamap.data.api.VehicleInfo
import com.music.music.subnauticamap.data.repository.ApiResult
import com.music.music.subnauticamap.data.repository.CustomMarker
import com.music.music.subnauticamap.data.repository.CustomMarkersRepository
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
 * Visibility settings for map elements
 */
data class MapVisibility(
    val showPlayer: Boolean = true,
    val showBeacons: Boolean = true,
    val showVehicles: Boolean = true,
    val showCustomMarkers: Boolean = true
)

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
    // Current map layer based on biome (or manual override)
    val currentLayer: MapLayer = MapLayer.SURFACE,
    // Manual layer override (null = auto-detect from biome)
    val layerOverride: MapLayer? = null,
    // Fog of war state - per layer
    val fogOfWarEnabled: Boolean = true,
    val exploredChunksPerLayer: Map<MapLayer, Set<Long>> = MapLayer.entries.associateWith { emptySet<Long>() },
    val explorationStats: ExplorationStats? = null,
    // Flag to track if we received at least one successful data update
    val hasReceivedData: Boolean = false,
    // Visibility settings for map elements
    val visibility: MapVisibility = MapVisibility(),
    // Map display settings
    val useDetailedMap: Boolean = false,
    // Custom markers (user-created points of interest)
    val customMarkers: List<CustomMarker> = emptyList()
) {
    /**
     * Get explored chunks for the current layer
     */
    val currentExploredChunks: Set<Long>
        get() = exploredChunksPerLayer[currentLayer] ?: emptySet<Long>()

    /**
     * Get the map URL for the current layer (respects detailed/blank setting)
     */
    val currentMapUrl: String
        get() = currentLayer.getMapUrl(useDetailedMap)

    /**
     * Check if using auto layer detection
     */
    val isAutoLayerDetection: Boolean
        get() = layerOverride == null

    /**
     * Get custom markers for the current layer
     */
    val currentLayerCustomMarkers: List<CustomMarker>
        get() = customMarkers.filter { it.layer == currentLayer }
}

/**
 * ViewModel for map screen
 */
class MapViewModel(
    private val gameStateRepository: GameStateRepository,
    private val fogOfWarRepository: FogOfWarRepository,
    private val customMarkersRepository: CustomMarkersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    companion object {
        private const val POLL_INTERVAL_MS = 1000L
    }

    init {
        // Load all settings
        viewModelScope.launch {
            val enabled = fogOfWarRepository.fogOfWarEnabled.first()
            val allChunks = fogOfWarRepository.loadAllExploredChunks()
            val currentLayer = _uiState.value.currentLayer
            val stats = fogOfWarRepository.getExplorationStats(allChunks[currentLayer] ?: emptySet<Long>())
            val useDetailed = fogOfWarRepository.useDetailedMap.first()
            val layerOverride = fogOfWarRepository.layerOverride.first()

            _uiState.value = _uiState.value.copy(
                fogOfWarEnabled = enabled,
                exploredChunksPerLayer = allChunks,
                explorationStats = stats,
                useDetailedMap = useDetailed,
                layerOverride = layerOverride,
                currentLayer = layerOverride ?: currentLayer
            )
        }

        // Observe fog of war enabled changes
        viewModelScope.launch {
            fogOfWarRepository.fogOfWarEnabled.collect { enabled ->
                _uiState.value = _uiState.value.copy(fogOfWarEnabled = enabled)
            }
        }

        // Observe detailed map preference changes
        viewModelScope.launch {
            fogOfWarRepository.useDetailedMap.collect { useDetailed ->
                _uiState.value = _uiState.value.copy(useDetailedMap = useDetailed)
            }
        }

        // Observe layer override changes
        viewModelScope.launch {
            fogOfWarRepository.layerOverride.collect { override ->
                val newLayer = override ?: _uiState.value.currentLayer
                _uiState.value = _uiState.value.copy(
                    layerOverride = override,
                    currentLayer = newLayer
                )
            }
        }

        // Load custom markers
        viewModelScope.launch {
            customMarkersRepository.loadMarkers()
        }

        // Observe custom markers changes
        viewModelScope.launch {
            customMarkersRepository.markers.collect { markers ->
                _uiState.value = _uiState.value.copy(customMarkers = markers)
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
     * Reset fog of war for current layer only
     */
    fun resetFogOfWar() {
        viewModelScope.launch {
            val currentLayer = _uiState.value.currentLayer
            fogOfWarRepository.clearExploredChunks(currentLayer)

            val updatedChunks = _uiState.value.exploredChunksPerLayer.toMutableMap()
            updatedChunks[currentLayer] = emptySet<Long>()

            _uiState.value = _uiState.value.copy(
                exploredChunksPerLayer = updatedChunks,
                explorationStats = fogOfWarRepository.getExplorationStats(emptySet<Long>())
            )
        }
    }

    /**
     * Reset fog of war for all layers
     */
    fun resetAllFogOfWar() {
        viewModelScope.launch {
            fogOfWarRepository.clearAllExploredChunks()
            _uiState.value = _uiState.value.copy(
                exploredChunksPerLayer = MapLayer.entries.associateWith { emptySet<Long>() },
                explorationStats = fogOfWarRepository.getExplorationStats(emptySet<Long>())
            )
        }
    }

    /**
     * Reload all data from repositories (call after backup restore)
     */
    fun reloadData() {
        viewModelScope.launch {
            // Reload fog of war chunks for all layers
            val allChunks = fogOfWarRepository.loadAllExploredChunks()
            val currentLayer = _uiState.value.currentLayer
            val stats = fogOfWarRepository.getExplorationStats(allChunks[currentLayer] ?: emptySet<Long>())

            // Reload settings
            val enabled = fogOfWarRepository.fogOfWarEnabled.first()
            val useDetailed = fogOfWarRepository.useDetailedMap.first()
            val layerOverride = fogOfWarRepository.layerOverride.first()

            // Reload custom markers
            customMarkersRepository.loadMarkers()

            _uiState.value = _uiState.value.copy(
                fogOfWarEnabled = enabled,
                exploredChunksPerLayer = allChunks,
                explorationStats = stats,
                useDetailedMap = useDetailed,
                layerOverride = layerOverride,
                currentLayer = layerOverride ?: currentLayer
            )
        }
    }

    /**
     * Toggle player visibility
     */
    fun togglePlayerVisibility() {
        val current = _uiState.value.visibility
        _uiState.value = _uiState.value.copy(
            visibility = current.copy(showPlayer = !current.showPlayer)
        )
    }

    /**
     * Toggle beacons visibility
     */
    fun toggleBeaconsVisibility() {
        val current = _uiState.value.visibility
        _uiState.value = _uiState.value.copy(
            visibility = current.copy(showBeacons = !current.showBeacons)
        )
    }

    /**
     * Toggle vehicles visibility
     */
    fun toggleVehiclesVisibility() {
        val current = _uiState.value.visibility
        _uiState.value = _uiState.value.copy(
            visibility = current.copy(showVehicles = !current.showVehicles)
        )
    }

    /**
     * Toggle custom markers visibility
     */
    fun toggleCustomMarkersVisibility() {
        val current = _uiState.value.visibility
        _uiState.value = _uiState.value.copy(
            visibility = current.copy(showCustomMarkers = !current.showCustomMarkers)
        )
    }

    /**
     * Toggle detailed map mode
     */
    fun toggleDetailedMap() {
        viewModelScope.launch {
            val newValue = !_uiState.value.useDetailedMap
            fogOfWarRepository.setUseDetailedMap(newValue)
        }
    }

    /**
     * Set manual layer override
     * @param layer The layer to force, or null for auto-detect
     */
    fun setLayerOverride(layer: MapLayer?) {
        viewModelScope.launch {
            fogOfWarRepository.setLayerOverride(layer)
            // If setting an override, immediately switch to that layer
            if (layer != null) {
                val stats = fogOfWarRepository.getExplorationStats(
                    _uiState.value.exploredChunksPerLayer[layer] ?: emptySet<Long>()
                )
                _uiState.value = _uiState.value.copy(
                    currentLayer = layer,
                    layerOverride = layer,
                    explorationStats = stats
                )
            }
        }
    }

    /**
     * Save current player position as a custom marker
     */
    fun saveCurrentPositionAsMarker(label: String, colorIndex: Int = 0) {
        val player = _uiState.value.player ?: return
        val currentLayer = _uiState.value.currentLayer

        viewModelScope.launch {
            customMarkersRepository.addMarker(
                label = label,
                x = player.position.xFloat,
                y = player.position.yFloat,
                z = player.position.zFloat,
                colorIndex = colorIndex,
                layer = currentLayer
            )
        }
    }

    /**
     * Update an existing custom marker
     */
    fun updateCustomMarker(marker: CustomMarker) {
        viewModelScope.launch {
            customMarkersRepository.updateMarker(marker)
        }
    }

    /**
     * Delete a custom marker
     */
    fun deleteCustomMarker(markerId: String) {
        viewModelScope.launch {
            customMarkersRepository.deleteMarker(markerId)
        }
    }

    /**
     * Delete all custom markers
     */
    fun deleteAllCustomMarkers() {
        viewModelScope.launch {
            customMarkersRepository.deleteAllMarkers()
        }
    }

    private suspend fun fetchGameState() {
        when (val result = gameStateRepository.getGameState()) {
            is ApiResult.Success -> {
                val state = result.data
                val currentState = _uiState.value

                // Determine the map layer: use override if set, otherwise auto-detect from biome
                val detectedLayer = state.player?.let { MapLayer.fromBiome(it.biome) } ?: currentState.currentLayer
                val newLayer = currentState.layerOverride ?: detectedLayer

                // Update explored chunks for the current layer if player position available
                var newExploredChunksPerLayer = currentState.exploredChunksPerLayer
                state.player?.let { player ->
                    if (currentState.fogOfWarEnabled) {
                        val currentLayerChunks = newExploredChunksPerLayer[newLayer] ?: emptySet<Long>()
                        val updatedChunks = fogOfWarRepository.exploreAtPosition(
                            newLayer,
                            player.position.xFloat,
                            player.position.zFloat,
                            currentLayerChunks
                        )

                        if (updatedChunks != currentLayerChunks) {
                            newExploredChunksPerLayer = newExploredChunksPerLayer.toMutableMap().apply {
                                this[newLayer] = updatedChunks
                            }
                        }
                    }
                }

                // Update stats for the new layer
                val newLayerChunks = newExploredChunksPerLayer[newLayer] ?: emptySet<Long>()
                val oldLayerChunks = currentState.exploredChunksPerLayer[newLayer] ?: emptySet<Long>()
                val stats = if (newLayer != currentState.currentLayer || newLayerChunks != oldLayerChunks) {
                    fogOfWarRepository.getExplorationStats(newLayerChunks)
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
                    currentLayer = newLayer,
                    exploredChunksPerLayer = newExploredChunksPerLayer,
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
    private val fogOfWarRepository: FogOfWarRepository,
    private val customMarkersRepository: CustomMarkersRepository
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(gameStateRepository, fogOfWarRepository, customMarkersRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
