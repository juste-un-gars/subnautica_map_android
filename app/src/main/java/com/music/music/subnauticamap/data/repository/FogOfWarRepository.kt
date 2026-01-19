/**
 * @file FogOfWarRepository.kt
 * @description Repository for saving and loading explored map chunks
 * @session SESSION_003
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
import com.music.music.subnauticamap.ui.map.components.packChunkKey
import com.music.music.subnauticamap.ui.map.components.worldToChunk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.fogDataStore: DataStore<Preferences> by preferencesDataStore(name = "fog_of_war")

/**
 * Repository for managing fog of war persistence
 */
class FogOfWarRepository(private val context: Context) {

    companion object {
        private val FOG_ENABLED_KEY = booleanPreferencesKey("fog_enabled")
        private const val EXPLORED_CHUNKS_FILE = "explored_chunks.dat"
    }

    private val exploredChunksFile: File
        get() = File(context.filesDir, EXPLORED_CHUNKS_FILE)

    /**
     * Flow indicating if fog of war is enabled
     */
    val fogOfWarEnabled: Flow<Boolean> = context.fogDataStore.data.map { prefs ->
        prefs[FOG_ENABLED_KEY] ?: true // Enabled by default
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
     * Load explored chunks from storage
     */
    suspend fun loadExploredChunks(): Set<Long> {
        return try {
            if (!exploredChunksFile.exists()) {
                return emptySet()
            }

            val bytes = exploredChunksFile.readBytes()
            val chunks = mutableSetOf<Long>()

            // Read as pairs of Long values (8 bytes each)
            var i = 0
            while (i + 7 < bytes.size) {
                var value = 0L
                for (j in 0 until 8) {
                    value = value or ((bytes[i + j].toLong() and 0xFF) shl (j * 8))
                }
                chunks.add(value)
                i += 8
            }

            chunks
        } catch (e: Exception) {
            emptySet()
        }
    }

    /**
     * Save explored chunks to storage
     */
    suspend fun saveExploredChunks(chunks: Set<Long>) {
        try {
            val bytes = ByteArray(chunks.size * 8)
            var i = 0

            chunks.forEach { chunk ->
                for (j in 0 until 8) {
                    bytes[i + j] = ((chunk shr (j * 8)) and 0xFF).toByte()
                }
                i += 8
            }

            exploredChunksFile.writeBytes(bytes)
        } catch (e: Exception) {
            // Ignore write errors
        }
    }

    /**
     * Add a single explored chunk and save
     */
    suspend fun addExploredChunk(chunkKey: Long, currentChunks: Set<Long>): Set<Long> {
        if (currentChunks.contains(chunkKey)) {
            return currentChunks
        }

        val newChunks = currentChunks + chunkKey
        saveExploredChunks(newChunks)
        return newChunks
    }

    /**
     * Explore chunks around a world position (reveals current chunk only for 50m visibility)
     */
    suspend fun exploreAtPosition(
        worldX: Float,
        worldZ: Float,
        currentChunks: Set<Long>
    ): Set<Long> {
        val (chunkX, chunkZ) = worldToChunk(worldX, worldZ)
        val chunkKey = packChunkKey(chunkX, chunkZ)

        return addExploredChunk(chunkKey, currentChunks)
    }

    /**
     * Clear all explored chunks (reset fog of war)
     */
    suspend fun clearExploredChunks() {
        try {
            if (exploredChunksFile.exists()) {
                exploredChunksFile.delete()
            }
        } catch (e: Exception) {
            // Ignore delete errors
        }
    }

    /**
     * Get statistics about exploration
     */
    fun getExplorationStats(exploredChunks: Set<Long>): ExplorationStats {
        val totalChunks = 80 * 80 // 6400 total chunks
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
