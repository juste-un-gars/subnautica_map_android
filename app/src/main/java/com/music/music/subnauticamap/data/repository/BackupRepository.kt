/**
 * @file BackupRepository.kt
 * @description Repository for backing up and restoring all app data
 * @session SESSION_008
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.music.music.subnauticamap.data.api.MapLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Backup data structure containing all app state
 */
data class BackupData(
    val version: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val settings: BackupSettings,
    val customMarkers: List<CustomMarker>,
    val fogOfWarData: Map<String, List<Long>>
)

/**
 * Backup of settings
 */
data class BackupSettings(
    val fogOfWarEnabled: Boolean,
    val useDetailedMap: Boolean,
    val layerOverride: String?
)

/**
 * Repository for backup and restore operations
 */
class BackupRepository(
    private val context: Context,
    private val fogOfWarRepository: FogOfWarRepository,
    private val customMarkersRepository: CustomMarkersRepository
) {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Create a backup of all app data
     * @return JSON string of backup data
     */
    suspend fun createBackup(): String = withContext(Dispatchers.IO) {
        // Collect settings
        val settings = BackupSettings(
            fogOfWarEnabled = fogOfWarRepository.fogOfWarEnabled.first(),
            useDetailedMap = fogOfWarRepository.useDetailedMap.first(),
            layerOverride = fogOfWarRepository.layerOverride.first()?.name
        )

        // Collect custom markers
        val markers = customMarkersRepository.markers.value

        // Collect fog of war data for all layers
        val fogData = mutableMapOf<String, List<Long>>()
        MapLayer.entries.forEach { layer ->
            val chunks = fogOfWarRepository.loadExploredChunks(layer)
            if (chunks.isNotEmpty()) {
                fogData[layer.name] = chunks.toList()
            }
        }

        // Create backup object
        val backup = BackupData(
            settings = settings,
            customMarkers = markers,
            fogOfWarData = fogData
        )

        gson.toJson(backup)
    }

    /**
     * Export backup to a URI (e.g., Downloads folder)
     */
    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupJson = createBackup()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backupJson.toByteArray(Charsets.UTF_8))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Restore from backup JSON string
     */
    suspend fun restoreBackup(backupJson: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val backup = gson.fromJson(backupJson, BackupData::class.java)

            // Restore settings
            fogOfWarRepository.setFogOfWarEnabled(backup.settings.fogOfWarEnabled)
            fogOfWarRepository.setUseDetailedMap(backup.settings.useDetailedMap)
            val layerOverride = backup.settings.layerOverride?.let {
                try { MapLayer.valueOf(it) } catch (e: Exception) { null }
            }
            fogOfWarRepository.setLayerOverride(layerOverride)

            // Restore custom markers
            customMarkersRepository.deleteAllMarkers()
            backup.customMarkers.forEach { marker ->
                customMarkersRepository.addMarker(
                    label = marker.label,
                    x = marker.x,
                    y = marker.y,
                    z = marker.z,
                    colorIndex = marker.colorIndex,
                    layer = marker.layer
                )
            }

            // Restore fog of war data
            fogOfWarRepository.clearAllExploredChunks()
            backup.fogOfWarData.forEach { (layerName, chunks) ->
                try {
                    val layer = MapLayer.valueOf(layerName)
                    fogOfWarRepository.saveExploredChunks(layer, chunks.toSet())
                } catch (e: Exception) {
                    // Skip invalid layer names
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Import backup from a URI
     */
    suspend fun importBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val backupJson = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.bufferedReader().readText()
            } ?: return@withContext false

            restoreBackup(backupJson)
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get backup file name with timestamp
     */
    fun getBackupFileName(): String {
        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        return "subnautica_map_backup_$timestamp.json"
    }
}
