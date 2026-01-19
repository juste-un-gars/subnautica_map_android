/**
 * @file InteractiveMapView.kt
 * @description Interactive map component with zoom, pan, markers, and fog of war
 * @session SESSION_003
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.music.music.subnauticamap.data.api.BeaconInfo
import com.music.music.subnauticamap.data.api.PlayerInfo
import com.music.music.subnauticamap.data.api.VehicleInfo
import com.music.music.subnauticamap.ui.theme.SubnauticaColors
import kotlin.math.sqrt

/**
 * Represents a marker that can be selected
 */
sealed class SelectedMarker {
    data class Player(val info: PlayerInfo) : SelectedMarker()
    data class Beacon(val info: BeaconInfo) : SelectedMarker()
    data class Vehicle(val info: VehicleInfo) : SelectedMarker()
}

/**
 * Map state holder for zoom and pan
 */
@Stable
class MapState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)

    companion object {
        const val MIN_SCALE = 0.5f
        const val MAX_SCALE = 5f
    }

    fun zoomIn() {
        scale = (scale * 1.5f).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun zoomOut() {
        scale = (scale / 1.5f).coerceIn(MIN_SCALE, MAX_SCALE)
    }

    fun reset() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }
}

@Composable
fun rememberMapState(): MapState = remember { MapState() }

/**
 * Convert world coordinates to map pixel position
 */
fun worldToMapPosition(
    worldX: Float,
    worldZ: Float,
    mapWidth: Float,
    mapHeight: Float
): Offset {
    val mapX = ((worldX + 2000f) / 4000f) * mapWidth
    val mapY = ((2000f - worldZ) / 4000f) * mapHeight
    return Offset(mapX, mapY)
}

/**
 * Convert world coordinates to map pixel position with vertical offset
 * Used when the square map is centered in a taller container
 */
fun worldToMapPositionWithOffset(
    worldX: Float,
    worldZ: Float,
    mapSize: Float,
    offsetY: Float
): Offset {
    val mapX = ((worldX + 2000f) / 4000f) * mapSize
    val mapY = ((2000f - worldZ) / 4000f) * mapSize + offsetY
    return Offset(mapX, mapY)
}

/**
 * Convert screen tap position to world coordinates
 */
fun screenToWorldPosition(
    screenX: Float,
    screenY: Float,
    containerSize: IntSize,
    mapState: MapState
): Pair<Float, Float> {
    // Reverse the transformations
    val mapX = (screenX - containerSize.width / 2f - mapState.offsetX) / mapState.scale + containerSize.width / 2f
    val mapY = (screenY - containerSize.height / 2f - mapState.offsetY) / mapState.scale + containerSize.height / 2f

    // Convert to world coordinates
    val worldX = (mapX / containerSize.width) * 4000f - 2000f
    val worldZ = 2000f - (mapY / containerSize.height) * 4000f

    return Pair(worldX, worldZ)
}

/**
 * Interactive map with zoom, pan, markers and fog of war
 */
@Composable
fun InteractiveMapView(
    mapUrl: String,
    player: PlayerInfo?,
    beacons: List<BeaconInfo>,
    vehicles: List<VehicleInfo>,
    mapState: MapState,
    exploredChunks: Set<Long>,
    fogOfWarEnabled: Boolean,
    hasReceivedData: Boolean,
    onCenterOnPlayer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var isImageLoaded by remember { mutableStateOf(false) }
    var selectedMarker by remember { mutableStateOf<SelectedMarker?>(null) }

    // Calculate the effective map size (square, based on container width)
    // The map image is square (4000x4000 world units), so we use width as the base
    val effectiveMapSize = containerSize.width.toFloat()
    // Vertical offset to center the square map in the container
    val mapOffsetY = (containerSize.height - containerSize.width) / 2f

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .background(Color.Black)
            .onSizeChanged { containerSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    val newScale = (mapState.scale * zoom).coerceIn(
                        MapState.MIN_SCALE,
                        MapState.MAX_SCALE
                    )

                    val scaleDiff = newScale - mapState.scale
                    mapState.offsetX -= (centroid.x - size.width / 2) * scaleDiff / mapState.scale
                    mapState.offsetY -= (centroid.y - size.height / 2) * scaleDiff / mapState.scale

                    mapState.scale = newScale
                    mapState.offsetX += pan.x
                    mapState.offsetY += pan.y
                }
            }
            .pointerInput(player, beacons, vehicles, mapState.scale, mapState.offsetX, mapState.offsetY, effectiveMapSize, mapOffsetY) {
                detectTapGestures { tapOffset ->
                    // Find tapped marker
                    val tappedMarker = findTappedMarker(
                        tapOffset = tapOffset,
                        containerSize = size,
                        mapState = mapState,
                        player = player,
                        beacons = beacons,
                        vehicles = vehicles,
                        effectiveMapSize = effectiveMapSize,
                        mapOffsetY = mapOffsetY
                    )
                    selectedMarker = tappedMarker
                }
            }
    ) {
        // Only show content after receiving data
        if (hasReceivedData) {
            // Map Image
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(mapUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Subnautica Map",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = mapState.scale
                        scaleY = mapState.scale
                        translationX = mapState.offsetX
                        translationY = mapState.offsetY
                    },
                onState = { state ->
                    if (state is AsyncImagePainter.State.Success) {
                        isImageLoaded = true
                    }
                }
            )

            // Fog of war overlay
            if (isImageLoaded && fogOfWarEnabled && containerSize.width > 0) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = mapState.scale
                            scaleY = mapState.scale
                            translationX = mapState.offsetX
                            translationY = mapState.offsetY
                            // Required for BlendMode.Clear to work with circles
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                ) {
                    drawFogOfWarCircles(exploredChunks, effectiveMapSize, mapOffsetY)
                }
            }

            // Markers overlay
            if (isImageLoaded && containerSize.width > 0 && containerSize.height > 0) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = mapState.scale
                            scaleY = mapState.scale
                            translationX = mapState.offsetX
                            translationY = mapState.offsetY
                        }
                ) {
                    // Draw vehicles
                    vehicles.forEach { vehicle ->
                        val pos = worldToMapPositionWithOffset(
                            vehicle.position.xFloat,
                            vehicle.position.zFloat,
                            effectiveMapSize,
                            mapOffsetY
                        )
                        val isSelected = (selectedMarker as? SelectedMarker.Vehicle)?.info?.id == vehicle.id
                        drawVehicleMarker(pos, vehicle.type, mapState.scale, isSelected)
                    }

                    // Draw beacons
                    beacons.filter { it.visible }.forEach { beacon ->
                        val pos = worldToMapPositionWithOffset(
                            beacon.position.xFloat,
                            beacon.position.zFloat,
                            effectiveMapSize,
                            mapOffsetY
                        )
                        val isSelected = (selectedMarker as? SelectedMarker.Beacon)?.info?.id == beacon.id
                        drawBeaconMarker(pos, beacon.colorIndex, mapState.scale, isSelected)
                    }

                    // Draw player
                    player?.let {
                        val playerPos = worldToMapPositionWithOffset(
                            it.position.xFloat,
                            it.position.zFloat,
                            effectiveMapSize,
                            mapOffsetY
                        )
                        val isSelected = selectedMarker is SelectedMarker.Player
                        drawPlayerMarker(playerPos, it.heading, mapState.scale, isSelected)
                    }
                }
            }
        }

        // Loading/waiting indicator
        if (!hasReceivedData || !isImageLoaded) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = SubnauticaColors.BioluminescentBlue
                )
                if (!hasReceivedData) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Waiting for game data...",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Marker info popup
        AnimatedVisibility(
            visible = selectedMarker != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        ) {
            selectedMarker?.let { marker ->
                MarkerInfoCard(
                    marker = marker,
                    onDismiss = { selectedMarker = null }
                )
            }
        }

        // Zoom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = onCenterOnPlayer,
                modifier = Modifier.size(48.dp),
                containerColor = SubnauticaColors.BioluminescentBlue,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = "Center on player",
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = { mapState.zoomIn() },
                modifier = Modifier.size(48.dp),
                containerColor = SubnauticaColors.OceanLight,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Zoom in",
                    modifier = Modifier.size(24.dp)
                )
            }

            FloatingActionButton(
                onClick = { mapState.zoomOut() },
                modifier = Modifier.size(48.dp),
                containerColor = SubnauticaColors.OceanLight,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Remove,
                    contentDescription = "Zoom out",
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Scale indicator
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
                .background(
                    SubnauticaColors.OceanDeep.copy(alpha = 0.7f),
                    shape = CircleShape
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "%.1fx".format(mapState.scale),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

/**
 * Find which marker was tapped
 */
private fun findTappedMarker(
    tapOffset: Offset,
    containerSize: androidx.compose.ui.unit.IntSize,
    mapState: MapState,
    player: PlayerInfo?,
    beacons: List<BeaconInfo>,
    vehicles: List<VehicleInfo>,
    effectiveMapSize: Float,
    mapOffsetY: Float
): SelectedMarker? {
    val tapRadius = 40f // Touch target radius in pixels

    // Convert tap to map coordinates (reverse the transform)
    val mapTapX = (tapOffset.x - containerSize.width / 2f - mapState.offsetX) / mapState.scale + containerSize.width / 2f
    val mapTapY = (tapOffset.y - containerSize.height / 2f - mapState.offsetY) / mapState.scale + containerSize.height / 2f

    // Check player first (highest priority)
    player?.let {
        val pos = worldToMapPositionWithOffset(it.position.xFloat, it.position.zFloat, effectiveMapSize, mapOffsetY)
        val distance = sqrt((mapTapX - pos.x) * (mapTapX - pos.x) + (mapTapY - pos.y) * (mapTapY - pos.y))
        if (distance < tapRadius / mapState.scale) {
            return SelectedMarker.Player(it)
        }
    }

    // Check beacons
    beacons.filter { it.visible }.forEach { beacon ->
        val pos = worldToMapPositionWithOffset(beacon.position.xFloat, beacon.position.zFloat, effectiveMapSize, mapOffsetY)
        val distance = sqrt((mapTapX - pos.x) * (mapTapX - pos.x) + (mapTapY - pos.y) * (mapTapY - pos.y))
        if (distance < tapRadius / mapState.scale) {
            return SelectedMarker.Beacon(beacon)
        }
    }

    // Check vehicles
    vehicles.forEach { vehicle ->
        val pos = worldToMapPositionWithOffset(vehicle.position.xFloat, vehicle.position.zFloat, effectiveMapSize, mapOffsetY)
        val distance = sqrt((mapTapX - pos.x) * (mapTapX - pos.x) + (mapTapY - pos.y) * (mapTapY - pos.y))
        if (distance < tapRadius / mapState.scale) {
            return SelectedMarker.Vehicle(vehicle)
        }
    }

    return null
}

/**
 * Info card for selected marker
 */
@Composable
private fun MarkerInfoCard(
    marker: SelectedMarker,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .widthIn(max = 300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = SubnauticaColors.OceanDeep.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Icon based on marker type
                    val (icon, iconColor, title) = when (marker) {
                        is SelectedMarker.Player -> Triple(
                            Icons.Default.Person,
                            SubnauticaColors.BioluminescentBlue,
                            "Player"
                        )
                        is SelectedMarker.Beacon -> Triple(
                            Icons.Default.LocationOn,
                            SubnauticaColors.getBeaconColor(marker.info.colorIndex),
                            marker.info.label
                        )
                        is SelectedMarker.Vehicle -> Triple(
                            Icons.Default.DirectionsBoat,
                            SubnauticaColors.getVehicleColor(marker.info.type),
                            marker.info.name.ifEmpty { marker.info.type }
                        )
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details based on marker type
            when (marker) {
                is SelectedMarker.Player -> {
                    MarkerDetailRow("Position", "X: %.0f  Z: %.0f".format(
                        marker.info.position.xFloat,
                        marker.info.position.zFloat
                    ))
                    MarkerDetailRow("Depth", "%.1f m".format(marker.info.depthFloat))
                    MarkerDetailRow("Biome", formatBiomeName(marker.info.biome))
                    MarkerDetailRow("Heading", "%.0f°".format(marker.info.heading))
                }
                is SelectedMarker.Beacon -> {
                    MarkerDetailRow("Type", "Beacon")
                    MarkerDetailRow("Position", "X: %.0f  Z: %.0f".format(
                        marker.info.position.xFloat,
                        marker.info.position.zFloat
                    ))
                    MarkerDetailRow("Depth", "%.0f m".format(-marker.info.position.yFloat))
                }
                is SelectedMarker.Vehicle -> {
                    MarkerDetailRow("Type", marker.info.type)
                    if (marker.info.name.isNotEmpty()) {
                        MarkerDetailRow("Name", marker.info.name)
                    }
                    MarkerDetailRow("Position", "X: %.0f  Z: %.0f".format(
                        marker.info.position.xFloat,
                        marker.info.position.zFloat
                    ))
                    MarkerDetailRow("Depth", "%.0f m".format(-marker.info.position.yFloat))
                }
            }
        }
    }
}

@Composable
private fun MarkerDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatBiomeName(biome: String): String {
    return biome.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        .replaceFirstChar { it.uppercase() }
}

/**
 * Draw fog of war overlay with circular reveals
 * Uses BlendMode.Clear to cut out circles for explored areas
 */
private fun DrawScope.drawFogOfWarCircles(exploredChunks: Set<Long>, mapSize: Float, offsetY: Float) {
    val chunkWorldSize = 50f // 50 world units per chunk (50m visibility radius)
    val chunksPerAxis = (4000f / chunkWorldSize).toInt() // 80 chunks per axis
    val chunkPixelSize = mapSize / chunksPerAxis
    // Circle radius = half of chunk size to create 50m diameter circles
    val circleRadius = chunkPixelSize * 0.7f // Slightly larger for overlap between adjacent chunks

    // Draw full black overlay covering the map area
    drawRect(
        color = Color.Black,
        topLeft = Offset(0f, offsetY),
        size = Size(mapSize, mapSize)
    )

    // Cut out circular reveals for explored chunks
    exploredChunks.forEach { chunkKey ->
        val (chunkX, chunkZ) = unpackChunkKey(chunkKey)
        if (chunkX in 0 until chunksPerAxis && chunkZ in 0 until chunksPerAxis) {
            // Center of the chunk
            val centerX = (chunkX + 0.5f) * chunkPixelSize
            val centerY = (chunkZ + 0.5f) * chunkPixelSize + offsetY

            // Clear a circle to reveal the map underneath
            drawCircle(
                color = Color.Transparent,
                radius = circleRadius,
                center = Offset(centerX, centerY),
                blendMode = BlendMode.Clear
            )
        }
    }
}

/**
 * Pack chunk coordinates into a single Long for efficient storage
 */
fun packChunkKey(chunkX: Int, chunkZ: Int): Long {
    return (chunkX.toLong() shl 32) or (chunkZ.toLong() and 0xFFFFFFFFL)
}

/**
 * Unpack chunk key to coordinates
 */
fun unpackChunkKey(key: Long): Pair<Int, Int> {
    val chunkX = (key shr 32).toInt()
    val chunkZ = key.toInt()
    return Pair(chunkX, chunkZ)
}

/**
 * Convert world position to chunk coordinates
 */
fun worldToChunk(worldX: Float, worldZ: Float): Pair<Int, Int> {
    val chunkWorldSize = 50f
    val chunkX = ((worldX + 2000f) / chunkWorldSize).toInt().coerceIn(0, 79)
    val chunkZ = ((2000f - worldZ) / chunkWorldSize).toInt().coerceIn(0, 79)
    return Pair(chunkX, chunkZ)
}

/**
 * Draw player marker with heading indicator
 */
private fun DrawScope.drawPlayerMarker(
    position: Offset,
    heading: Float,
    scale: Float,
    isSelected: Boolean
) {
    val markerSize = if (isSelected) 28f / scale else 24f / scale
    val arrowSize = markerSize * 1.2f

    // Selection ring
    if (isSelected) {
        drawCircle(
            color = Color.White,
            radius = markerSize * 2f,
            center = position,
            style = Stroke(width = 3f / scale)
        )
    }

    // Outer glow
    drawCircle(
        color = SubnauticaColors.BioluminescentBlue.copy(alpha = 0.3f),
        radius = markerSize * 1.5f,
        center = position
    )

    drawCircle(
        color = SubnauticaColors.BioluminescentBlue,
        radius = markerSize,
        center = position
    )

    drawCircle(
        color = Color.White,
        radius = markerSize * 0.6f,
        center = position
    )

    // Heading arrow
    rotate(heading, pivot = position) {
        val arrowPath = Path().apply {
            moveTo(position.x, position.y - arrowSize)
            lineTo(position.x - arrowSize * 0.4f, position.y - arrowSize * 0.3f)
            lineTo(position.x + arrowSize * 0.4f, position.y - arrowSize * 0.3f)
            close()
        }
        drawPath(
            path = arrowPath,
            color = Color.White,
            style = Fill
        )
    }
}

/**
 * Draw beacon marker
 */
private fun DrawScope.drawBeaconMarker(
    position: Offset,
    colorIndex: Int,
    scale: Float,
    isSelected: Boolean
) {
    val color = SubnauticaColors.getBeaconColor(colorIndex)
    val size = if (isSelected) 20f / scale else 16f / scale

    // Selection ring
    if (isSelected) {
        drawCircle(
            color = Color.White,
            radius = size * 1.8f,
            center = position,
            style = Stroke(width = 3f / scale)
        )
    }

    val path = Path().apply {
        moveTo(position.x, position.y - size)
        lineTo(position.x + size * 0.7f, position.y)
        lineTo(position.x, position.y + size)
        lineTo(position.x - size * 0.7f, position.y)
        close()
    }

    drawPath(path, color, style = Fill)
    drawPath(path, Color.White, style = Stroke(width = 2f / scale))
}

/**
 * Draw vehicle marker
 */
private fun DrawScope.drawVehicleMarker(
    position: Offset,
    type: String,
    scale: Float,
    isSelected: Boolean
) {
    val color = SubnauticaColors.getVehicleColor(type)
    val size = if (isSelected) 18f / scale else 14f / scale

    // Selection ring
    if (isSelected) {
        drawCircle(
            color = Color.White,
            radius = size * 1.8f,
            center = position,
            style = Stroke(width = 3f / scale)
        )
    }

    drawCircle(
        color = color,
        radius = size,
        center = position
    )
    drawCircle(
        color = Color.White,
        radius = size,
        center = position,
        style = Stroke(width = 2f / scale)
    )
}
