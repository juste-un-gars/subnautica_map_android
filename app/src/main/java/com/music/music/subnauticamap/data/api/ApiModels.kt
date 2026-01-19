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

/**
 * Map layers corresponding to different areas of Subnautica
 * Each layer has its own map image and fog of war
 * Maps are available in both detailed and blank versions
 */
enum class MapLayer(
    val displayName: String,
    val blankMapUrl: String,
    val detailedMapUrl: String
) {
    SURFACE(
        displayName = "Surface",
        blankMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-blank.jpg",
        detailedMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-surface.jpg"
    ),
    JELLYSHROOM_CAVES(
        displayName = "Jellyshroom Caves",
        blankMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-jellyshroom-cave.jpg",
        detailedMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-jellyshroom-cave.jpg"
    ),
    LOST_RIVER(
        displayName = "Lost River",
        blankMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-1-lost-river.jpg",
        detailedMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-1-lost-river.jpg"
    ),
    INACTIVE_LAVA_ZONE(
        displayName = "Inactive Lava Zone",
        blankMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-2-inactive-lava-zone.jpg",
        detailedMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-2-inactive-lava-zone.jpg"
    ),
    LAVA_LAKES(
        displayName = "Lava Lakes",
        blankMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-3-lava-lakes.jpg",
        detailedMapUrl = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-3-lava-lakes.jpg"
    );

    /**
     * Get the map URL based on the detailed/blank preference
     */
    fun getMapUrl(useDetailedMap: Boolean): String {
        return if (useDetailedMap) detailedMapUrl else blankMapUrl
    }

    companion object {
        /**
         * Determine the appropriate map layer based on biome name
         * Uses priority: Lava Lakes > Inactive Lava > Lost River > Jellyshroom > Surface
         */
        fun fromBiome(biome: String?): MapLayer {
            if (biome.isNullOrEmpty()) return SURFACE

            val biomeLower = biome.lowercase()

            // Priority 1: Lava Lakes (deepest)
            if (biomeLower.contains("lavalakes") ||
                biomeLower.contains("introlava") ||
                biomeLower.contains("activelava")) {
                return LAVA_LAKES
            }

            // Priority 2: Inactive Lava Zone
            if (biomeLower.contains("ilz") ||
                biomeLower.contains("inactivelava") ||
                biomeLower.contains("intactivelava") ||
                biomeLower.contains("lavacastle") ||
                biomeLower.contains("lavapit")) {
                return INACTIVE_LAVA_ZONE
            }

            // Priority 3: Lost River
            if (biomeLower.contains("lostriver")) {
                return LOST_RIVER
            }

            // Priority 4: Jellyshroom Caves
            if (biomeLower.contains("jellyshroom")) {
                return JELLYSHROOM_CAVES
            }

            // Default: Surface
            return SURFACE
        }
    }
}
