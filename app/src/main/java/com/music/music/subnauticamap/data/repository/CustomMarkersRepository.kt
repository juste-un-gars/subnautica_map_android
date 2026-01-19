/**
 * @file CustomMarkersRepository.kt
 * @description Repository for managing custom markers (user-created points of interest)
 * @session SESSION_008
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.music.music.subnauticamap.data.api.MapLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * Custom marker data class
 * Stores user-created points of interest with full 3D coordinates
 */
data class CustomMarker(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val x: Float,           // East/West coordinate
    val y: Float,           // Depth (negative = underwater)
    val z: Float,           // North/South coordinate
    val colorIndex: Int = 0, // Color index (0-7, like beacons)
    val layer: MapLayer = MapLayer.SURFACE, // Which layer this marker belongs to
    val createdAt: Long = System.currentTimeMillis()
) {
    /**
     * Get depth as positive value (for display)
     */
    val depth: Float
        get() = -y
}

/**
 * Repository for managing custom markers persistence
 */
class CustomMarkersRepository(private val context: Context) {

    private val gson = Gson()
    private val markersFile: File
        get() = File(context.filesDir, "custom_markers.json")

    private val _markers = MutableStateFlow<List<CustomMarker>>(emptyList())
    val markers: StateFlow<List<CustomMarker>> = _markers.asStateFlow()

    /**
     * Load markers from storage
     */
    suspend fun loadMarkers() {
        withContext(Dispatchers.IO) {
            try {
                if (markersFile.exists()) {
                    val json = markersFile.readText()
                    val type = object : TypeToken<List<CustomMarker>>() {}.type
                    val loadedMarkers: List<CustomMarker> = gson.fromJson(json, type) ?: emptyList()
                    _markers.value = loadedMarkers
                }
            } catch (e: Exception) {
                // If loading fails, start with empty list
                _markers.value = emptyList()
            }
        }
    }

    /**
     * Save markers to storage
     */
    private suspend fun saveMarkers() {
        withContext(Dispatchers.IO) {
            try {
                val json = gson.toJson(_markers.value)
                markersFile.writeText(json)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }

    /**
     * Add a new custom marker at the given position
     */
    suspend fun addMarker(
        label: String,
        x: Float,
        y: Float,
        z: Float,
        colorIndex: Int = 0,
        layer: MapLayer = MapLayer.SURFACE
    ): CustomMarker {
        val marker = CustomMarker(
            label = label,
            x = x,
            y = y,
            z = z,
            colorIndex = colorIndex,
            layer = layer
        )
        _markers.value = _markers.value + marker
        saveMarkers()
        return marker
    }

    /**
     * Update an existing marker
     */
    suspend fun updateMarker(marker: CustomMarker) {
        _markers.value = _markers.value.map {
            if (it.id == marker.id) marker else it
        }
        saveMarkers()
    }

    /**
     * Delete a marker by ID
     */
    suspend fun deleteMarker(markerId: String) {
        _markers.value = _markers.value.filter { it.id != markerId }
        saveMarkers()
    }

    /**
     * Get markers for a specific layer
     */
    fun getMarkersForLayer(layer: MapLayer): List<CustomMarker> {
        return _markers.value.filter { it.layer == layer }
    }

    /**
     * Delete all markers
     */
    suspend fun deleteAllMarkers() {
        _markers.value = emptyList()
        saveMarkers()
    }

    companion object {
        // Available colors for custom markers (matches beacon colors)
        val MARKER_COLORS = listOf(
            0xFF00FFFF.toInt(), // Cyan (default)
            0xFFFF0000.toInt(), // Red
            0xFF00FF00.toInt(), // Green
            0xFFFFFF00.toInt(), // Yellow
            0xFFFF00FF.toInt(), // Magenta
            0xFFFF8000.toInt(), // Orange
            0xFF8000FF.toInt(), // Purple
            0xFFFFFFFF.toInt()  // White
        )
    }
}
