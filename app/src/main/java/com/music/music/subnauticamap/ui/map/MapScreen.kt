/**
 * @file MapScreen.kt
 * @description Map screen UI with interactive map, markers, and fog of war
 * @session SESSION_003
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.map

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.music.subnauticamap.R
import com.music.music.subnauticamap.data.api.PlayerInfo
import com.music.music.subnauticamap.data.api.TimeInfo
import com.music.music.subnauticamap.data.repository.ExplorationStats
import com.music.music.subnauticamap.ui.map.components.InteractiveMapView
import com.music.music.subnauticamap.ui.map.components.MapState
import com.music.music.subnauticamap.ui.map.components.rememberMapState
import com.music.music.subnauticamap.ui.theme.SubnauticaColors

private const val MAP_URL = "https://rocketsoup.net/blogassets/subnautica/2024-07-16/map-blank.jpg"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    keepScreenOn: Boolean,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val mapState = rememberMapState()
    var showInfoPanel by remember { mutableStateOf(true) }
    var showFogMenu by remember { mutableStateOf(false) }

    // Keep screen on management
    DisposableEffect(keepScreenOn) {
        val window = (view.context as? android.app.Activity)?.window
        if (keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Start polling when screen is shown
    LaunchedEffect(Unit) {
        viewModel.startPolling()
    }

    // Stop polling when leaving
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopPolling()
        }
    }

    // Center on player when trigger changes
    LaunchedEffect(uiState.centerOnPlayerTrigger) {
        if (uiState.centerOnPlayerTrigger > 0 && uiState.player != null) {
            centerMapOnPlayer(mapState, uiState.player!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subnautica Map") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SubnauticaColors.OceanDeep.copy(alpha = 0.9f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    // Fog of war toggle
                    Box {
                        IconButton(onClick = { showFogMenu = true }) {
                            Icon(
                                if (uiState.fogOfWarEnabled) Icons.Default.Cloud else Icons.Default.CloudOff,
                                contentDescription = "Fog of war",
                                tint = if (uiState.fogOfWarEnabled) SubnauticaColors.BioluminescentBlue else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        DropdownMenu(
                            expanded = showFogMenu,
                            onDismissRequest = { showFogMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (uiState.fogOfWarEnabled) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (uiState.fogOfWarEnabled) "Disable Fog" else "Enable Fog")
                                    }
                                },
                                onClick = {
                                    viewModel.toggleFogOfWar()
                                    showFogMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset Exploration")
                                    }
                                },
                                onClick = {
                                    viewModel.resetFogOfWar()
                                    showFogMenu = false
                                }
                            )
                        }
                    }

                    // Toggle info panel
                    IconButton(onClick = { showInfoPanel = !showInfoPanel }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Toggle info",
                            tint = if (showInfoPanel) SubnauticaColors.BioluminescentBlue else Color.White
                        )
                    }

                    // Connection status indicator
                    ConnectionStatusIndicator(uiState.connectionStatus)
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Interactive Map
            InteractiveMapView(
                mapUrl = MAP_URL,
                player = uiState.player,
                beacons = uiState.beacons,
                vehicles = uiState.vehicles,
                mapState = mapState,
                exploredChunks = uiState.exploredChunks,
                fogOfWarEnabled = uiState.fogOfWarEnabled,
                onCenterOnPlayer = { viewModel.centerOnPlayer() },
                modifier = Modifier.fillMaxSize()
            )

            // Info overlay panel (top-left)
            if (showInfoPanel) {
                InfoOverlayPanel(
                    player = uiState.player,
                    time = uiState.time,
                    connectionStatus = uiState.connectionStatus,
                    explorationStats = uiState.explorationStats,
                    fogOfWarEnabled = uiState.fogOfWarEnabled,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }
        }
    }
}

/**
 * Center the map on player position
 */
private fun centerMapOnPlayer(mapState: MapState, player: PlayerInfo) {
    mapState.scale = 2f

    val normalizedX = (player.position.xFloat + 2000f) / 4000f
    val normalizedZ = (2000f - player.position.zFloat) / 4000f

    mapState.offsetX = (0.5f - normalizedX) * 1000f * mapState.scale
    mapState.offsetY = (0.5f - normalizedZ) * 1000f * mapState.scale
}

@Composable
private fun ConnectionStatusIndicator(status: MapConnectionStatus) {
    val (color, text) = when (status) {
        is MapConnectionStatus.Connected -> SubnauticaColors.BioluminescentGreen to "Connected"
        is MapConnectionStatus.WaitingForGame -> SubnauticaColors.CoralOrange to "Waiting..."
        is MapConnectionStatus.Error -> SubnauticaColors.WarningRed to "Error"
        is MapConnectionStatus.Disconnected -> Color.Gray to "Disconnected"
    }

    Row(
        modifier = Modifier.padding(end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = color
        )
    }
}

@Composable
private fun InfoOverlayPanel(
    player: PlayerInfo?,
    time: TimeInfo?,
    connectionStatus: MapConnectionStatus,
    explorationStats: ExplorationStats?,
    fogOfWarEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 200.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SubnauticaColors.OceanDeep.copy(alpha = 0.85f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Player heading indicator
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Navigation,
                    contentDescription = null,
                    tint = SubnauticaColors.BioluminescentBlue,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(player?.heading ?: 0f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Player",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.weight(1f))
                time?.let {
                    Text(
                        text = if (it.isDay) "☀" else "🌙",
                        fontSize = 14.sp
                    )
                }
            }

            if (player != null) {
                InfoRow(
                    label = "Position",
                    value = "%.0f, %.0f".format(
                        player.position.xFloat,
                        player.position.zFloat
                    )
                )

                InfoRow(
                    label = stringResource(R.string.player_depth),
                    value = "%.1f m".format(player.depthFloat),
                    valueColor = getDepthColor(player.depthFloat)
                )

                InfoRow(
                    label = stringResource(R.string.player_biome),
                    value = formatBiomeName(player.biome)
                )

                InfoRow(
                    label = stringResource(R.string.player_heading),
                    value = "%.0f° %s".format(
                        player.heading,
                        getHeadingDirection(player.heading)
                    )
                )

                // Exploration progress
                if (fogOfWarEnabled && explorationStats != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(4.dp))

                    InfoRow(
                        label = "Explored",
                        value = "%.1f%%".format(explorationStats.percentageExplored),
                        valueColor = SubnauticaColors.BioluminescentGreen
                    )
                }
            } else {
                val statusText = when (connectionStatus) {
                    is MapConnectionStatus.WaitingForGame -> stringResource(R.string.waiting_for_game)
                    is MapConnectionStatus.Error -> connectionStatus.message
                    is MapConnectionStatus.Disconnected -> "Disconnected"
                    else -> stringResource(R.string.no_data)
                }
                Text(
                    text = statusText,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = valueColor,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun getDepthColor(depth: Float): Color {
    return when {
        depth < 100 -> SubnauticaColors.SurfaceLight
        depth < 300 -> SubnauticaColors.BioluminescentGreen
        depth < 500 -> SubnauticaColors.BioluminescentBlue
        depth < 900 -> SubnauticaColors.CoralOrange
        else -> SubnauticaColors.WarningRed
    }
}

private fun getHeadingDirection(heading: Float): String {
    return when {
        heading < 22.5 || heading >= 337.5 -> "N"
        heading < 67.5 -> "NE"
        heading < 112.5 -> "E"
        heading < 157.5 -> "SE"
        heading < 202.5 -> "S"
        heading < 247.5 -> "SW"
        heading < 292.5 -> "W"
        else -> "NW"
    }
}

private fun formatBiomeName(biome: String): String {
    return biome.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        .replaceFirstChar { it.uppercase() }
}
