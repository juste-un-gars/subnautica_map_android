/**
 * @file ApiModels.kt
 * @description Data classes for Subnautica MapAPI responses
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.api

import com.google.gson.annotations.SerializedName

/**
 * Main game state response from /api/state
 */
data class GameState(
    @SerializedName("Timestamp") val timestamp: Long,
    @SerializedName("Player") val player: PlayerInfo?,
    @SerializedName("Time") val time: TimeInfo?,
    @SerializedName("Beacons") val beacons: List<BeaconInfo>,
    @SerializedName("Vehicles") val vehicles: List<VehicleInfo>
)

/**
 * Player information
 */
data class PlayerInfo(
    @SerializedName("Position") val position: Vector3,
    @SerializedName("Heading") val heading: Float,
    @SerializedName("Depth") val depth: String,
    @SerializedName("Biome") val biome: String
) {
    val depthFloat: Float
        get() = depth.toFloatOrNull() ?: 0f
}

/**
 * Time/day-night cycle information
 */
data class TimeInfo(
    @SerializedName("DayNightValue") val dayNightValue: String,
    @SerializedName("IsDay") val isDay: Boolean
)

/**
 * Beacon/ping marker information
 */
data class BeaconInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Label") val label: String,
    @SerializedName("Position") val position: Vector3,
    @SerializedName("ColorIndex") val colorIndex: Int,
    @SerializedName("Visible") val visible: Boolean
)

/**
 * Vehicle information (Seamoth, Cyclops, Prawn Suit)
 */
data class VehicleInfo(
    @SerializedName("Id") val id: String,
    @SerializedName("Type") val type: String,
    @SerializedName("Name") val name: String,
    @SerializedName("Position") val position: Vector3
)

/**
 * 3D position vector
 * Values are strings in the API response
 */
data class Vector3(
    @SerializedName("X") val x: String,
    @SerializedName("Y") val y: String,
    @SerializedName("Z") val z: String
) {
    val xFloat: Float get() = x.toFloatOrNull() ?: 0f
    val yFloat: Float get() = y.toFloatOrNull() ?: 0f
    val zFloat: Float get() = z.toFloatOrNull() ?: 0f
}

/**
 * Health check response from /api/ping
 */
data class PingResponse(
    @SerializedName("status") val status: String,
    @SerializedName("version") val version: String
)

/**
 * Error response when player not in game
 */
data class ErrorResponse(
    @SerializedName("error") val error: String,
    @SerializedName("message") val message: String
)
