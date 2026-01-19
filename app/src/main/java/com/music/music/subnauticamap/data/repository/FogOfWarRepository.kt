/**
 * @file FogOfWarRepository.kt
 * @description Repository for saving and loading explored map chunks per map layer
 * @session SESSION_005
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.music.music.subnauticamap.data.api.MapLayer
import com.music.music.subnauticamap.ui.map.components.packChunkKey
import com.music.music.subnauticamap.ui.map.components.worldToChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.fogDataStore: DataStore<Preferences> by preferencesDataStore(name = "fog_of_war")

/**
 * Repository for managing fog of war persistence per map layer
 * Each layer has its own separate storage file
 * Also manages map display settings
 */
class FogOfWarRepository(private val context: Context) {

    companion object {
        private val FOG_ENABLED_KEY = booleanPreferencesKey("fog_enabled")
        private val USE_DETAILED_MAP_KEY = booleanPreferencesKey("use_detailed_map")
        private val LAYER_OVERRIDE_KEY = stringPreferencesKey("layer_override")
    }

    /**
     * Get the file for storing explored chunks for a specific layer
     */
    private fun getExploredChunksFile(layer: MapLayer): File {
        val filename = "explored_chunks_${layer.name.lowercase()}.dat"
        return File(context.filesDir, filename)
    }

    // Legacy file for migration
    private val legacyExploredChunksFile: File
        get() = File(context.filesDir, "explored_chunks.dat")

    /**
     * Flow indicating if fog of war is enabled
     */
    val fogOfWarEnabled: Flow<Boolean> = context.fogDataStore.data.map { prefs ->
        prefs[FOG_ENABLED_KEY] ?: true // Enabled by default
    }

    /**
     * Flow indicating if detailed maps should be used
     */
    val useDetailedMap: Flow<Boolean> = context.fogDataStore.data.map { prefs ->
        prefs[USE_DETAILED_MAP_KEY] ?: false // Blank maps by default
    }

    /**
     * Flow for manual layer override (null = auto-detect from biome)
     */
    val layerOverride: Flow<MapLayer?> = context.fogDataStore.data.map { prefs ->
        prefs[LAYER_OVERRIDE_KEY]?.let { name ->
            try {
                MapLayer.valueOf(name)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Set fog of war enabled state
     */
    suspend fun setFogOfWarEnabled(enabled: Boolean) {
        context.fogDataStore.edit { prefs ->
            prefs[FOG_ENABLED_KEY] = enabled
        }
    }

    /**
     * Set detailed map preference
     */
    suspend fun setUseDetailedMap(useDetailed: Boolean) {
        context.fogDataStore.edit { prefs ->
            prefs[USE_DETAILED_MAP_KEY] = useDetailed
        }
    }

    /**
     * Set manual layer override (null to use auto-detect)
     */
    suspend fun setLayerOverride(layer: MapLayer?) {
        context.fogDataStore.edit { prefs ->
            if (layer != null) {
                prefs[LAYER_OVERRIDE_KEY] = layer.name
            } else {
                prefs.remove(LAYER_OVERRIDE_KEY)
            }
        }
    }

    /**
     * Load explored chunks from storage for a specific layer
     * On first load of SURFACE layer, migrates legacy data if exists
     */
    suspend fun loadExploredChunks(layer: MapLayer): Set<Long> {
        return try {
            val file = getExploredChunksFile(layer)

            // Migrate legacy data to SURFACE layer if needed
            if (layer == MapLayer.SURFACE && !file.exists() && legacyExploredChunksFile.exists()) {
                val legacyChunks = loadChunksFromFile(legacyExploredChunksFile)
                saveExploredChunks(layer, legacyChunks)
                legacyExploredChunksFile.delete()
                return legacyChunks
            }

            loadChunksFromFile(file)
        } catch (e: Exception) {
            emptySet<Long>()
        }
    }

    /**
     * Load chunks from a file
     */
    private fun loadChunksFromFile(file: File): Set<Long> {
        if (!file.exists()) {
            return emptySet<Long>()
        }

        val bytes = file.readBytes()
        val chunks = mutableSetOf<Long>()

        var i = 0
        while (i + 7 < bytes.size) {
            var value = 0L
            for (j in 0 until 8) {
                value = value or ((bytes[i + j].toLong() and 0xFF) shl (j * 8))
            }
            chunks.add(value)
            i += 8
        }

        return chunks
    }

    /**
     * Save explored chunks to storage for a specific layer
     */
    suspend fun saveExploredChunks(layer: MapLayer, chunks: Set<Long>) {
        try {
            val bytes = ByteArray(chunks.size * 8)
            var i = 0

            chunks.forEach { chunk ->
                for (j in 0 until 8) {
                    bytes[i + j] = ((chunk shr (j * 8)) and 0xFF).toByte()
                }
                i += 8
            }

            getExploredChunksFile(layer).writeBytes(bytes)
        } catch (e: Exception) {
            // Ignore write errors
        }
    }

    /**
     * Add a single explored chunk and save for a specific layer
     */
    suspend fun addExploredChunk(layer: MapLayer, chunkKey: Long, currentChunks: Set<Long>): Set<Long> {
        if (currentChunks.contains(chunkKey)) {
            return currentChunks
        }

        val newChunks = currentChunks + chunkKey
        saveExploredChunks(layer, newChunks)
        return newChunks
    }

    /**
     * Explore chunks around a world position for a specific layer
     */
    suspend fun exploreAtPosition(
        layer: MapLayer,
        worldX: Float,
        worldZ: Float,
        currentChunks: Set<Long>
    ): Set<Long> {
        val (chunkX, chunkZ) = worldToChunk(worldX, worldZ)
        val chunkKey = packChunkKey(chunkX, chunkZ)

        return addExploredChunk(layer, chunkKey, currentChunks)
    }

    /**
     * Clear explored chunks for a specific layer
     */
    suspend fun clearExploredChunks(layer: MapLayer) {
        try {
            val file = getExploredChunksFile(layer)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignore delete errors
        }
    }

    /**
     * Clear explored chunks for all layers
     */
    suspend fun clearAllExploredChunks() {
        MapLayer.entries.forEach { layer ->
            clearExploredChunks(layer)
        }
    }

    /**
     * Load explored chunks for all layers
     */
    suspend fun loadAllExploredChunks(): Map<MapLayer, Set<Long>> {
        return MapLayer.entries.associateWith { layer ->
            loadExploredChunks(layer)
        }
    }

    /**
     * Get statistics about exploration for a specific layer
     */
    fun getExplorationStats(exploredChunks: Set<Long>): ExplorationStats {
        // 4096m / 10m = ~410 chunks per axis = 168,100 total chunks
        val chunksPerAxis = 410
        val totalChunks = chunksPerAxis * chunksPerAxis
        val exploredCount = exploredChunks.size
        val percentage = (exploredCount.toFloat() / totalChunks * 100)

        return ExplorationStats(
            exploredChunks = exploredCount,
            totalChunks = totalChunks,
            percentageExplored = percentage
        )
    }
}

/**
 * Exploration statistics
 */
data class ExplorationStats(
    val exploredChunks: Int,
    val totalChunks: Int,
    val percentageExplored: Float
)
