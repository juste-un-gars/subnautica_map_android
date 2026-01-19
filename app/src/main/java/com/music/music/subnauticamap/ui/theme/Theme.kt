/**
 * @file Theme.kt
 * @description Subnautica-inspired theme colors and styling
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Subnautica-inspired color palette
 */
object SubnauticaColors {
    val OceanDeep = Color(0xFF0A1628)
    val OceanMedium = Color(0xFF0D2137)
    val OceanLight = Color(0xFF1A3A5C)
    val BioluminescentBlue = Color(0xFF00D4FF)
    val BioluminescentGreen = Color(0xFF00FF88)
    val CoralOrange = Color(0xFFFF6B35)
    val WarningRed = Color(0xFFFF4444)
    val SurfaceLight = Color(0xFF87CEEB)

    // Vehicle colors
    val SeamothYellow = Color(0xFFFFCC00)
    val CyclopsGray = Color(0xFF888888)
    val PrawnOrange = Color(0xFFFF8800)

    // Beacon colors (matching Subnautica)
    val BeaconColors = listOf(
        Color(0xFFFF0000), // 0 - Red
        Color(0xFFFF8800), // 1 - Orange
        Color(0xFFFFFF00), // 2 - Yellow
        Color(0xFF00FF00), // 3 - Green
        Color(0xFF00FFFF), // 4 - Cyan
        Color(0xFF0088FF), // 5 - Blue
        Color(0xFFFF00FF), // 6 - Magenta
        Color(0xFFFFFFFF)  // 7 - White
    )

    fun getBeaconColor(index: Int): Color {
        return BeaconColors.getOrElse(index) { BeaconColors[0] }
    }

    fun getVehicleColor(type: String): Color {
        return when (type.lowercase()) {
            "seamoth" -> SeamothYellow
            "cyclops" -> CyclopsGray
            "exosuit" -> PrawnOrange
            else -> BioluminescentBlue
        }
    }
}

private val DarkColorScheme = darkColorScheme(
    primary = SubnauticaColors.BioluminescentBlue,
    secondary = SubnauticaColors.BioluminescentGreen,
    tertiary = SubnauticaColors.CoralOrange,
    background = SubnauticaColors.OceanDeep,
    surface = SubnauticaColors.OceanMedium,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = SubnauticaColors.WarningRed
)

@Composable
fun SubnauticaMapTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
