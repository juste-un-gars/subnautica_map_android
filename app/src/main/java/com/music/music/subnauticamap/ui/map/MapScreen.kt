/**
 * @file MapScreen.kt
 * @description Map screen UI with interactive map, markers, and multi-layer fog of war
 * @session SESSION_005
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.map

import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.music.music.subnauticamap.data.repository.BackupRepository
import com.music.music.subnauticamap.data.repository.CustomMarkersRepository
import com.music.music.subnauticamap.data.repository.FogOfWarRepository
import kotlinx.coroutines.launch
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
import com.music.music.subnauticamap.data.api.MapLayer
import com.music.music.subnauticamap.data.api.PlayerInfo
import com.music.music.subnauticamap.data.api.TimeInfo
import com.music.music.subnauticamap.data.repository.ExplorationStats
import com.music.music.subnauticamap.ui.map.components.InteractiveMapView
import com.music.music.subnauticamap.ui.map.components.MapState
import com.music.music.subnauticamap.ui.map.components.rememberMapState
import com.music.music.subnauticamap.ui.theme.SubnauticaColors

// World dimensions
private const val WORLD_SIZE = 4096f
private const val WORLD_HALF = 2048f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel,
    keepScreenOn: Boolean,
    fogOfWarRepository: FogOfWarRepository,
    customMarkersRepository: CustomMarkersRepository,
    onBack: () -> Unit,
    onAbout: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapState = rememberMapState()
    var showInfoPanel by remember { mutableStateOf(true) }
    var showFogMenu by remember { mutableStateOf(false) }
    var showVisibilityMenu by remember { mutableStateOf(false) }
    var showMapMenu by remember { mutableStateOf(false) }
    var showSettingsMenu by remember { mutableStateOf(false) }
    var showAddMarkerDialog by remember { mutableStateOf(false) }
    var showResetFogConfirmDialog by remember { mutableStateOf(false) }
    var newMarkerName by remember { mutableStateOf("") }
    var newMarkerColor by remember { mutableIntStateOf(0) }

    // Edit marker dialog state
    var showEditMarkerDialog by remember { mutableStateOf(false) }
    var markerToEdit by remember { mutableStateOf<com.music.music.subnauticamap.data.repository.CustomMarker?>(null) }
    var editMarkerName by remember { mutableStateOf("") }
    var editMarkerColor by remember { mutableIntStateOf(0) }

    // Follow player mode - enabled by default
    var isFollowingPlayer by remember { mutableStateOf(true) }
    var hasInitializedPosition by remember { mutableStateOf(false) }

    // Backup repository using the shared repositories
    val backupRepository = remember(fogOfWarRepository, customMarkersRepository) {
        BackupRepository(context, fogOfWarRepository, customMarkersRepository)
    }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    // File picker for export
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isExporting = true
                val success = backupRepository.exportBackup(uri)
                isExporting = false
                Toast.makeText(
                    context,
                    if (success) "Backup exported successfully" else "Export failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // File picker for import
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isImporting = true
                val success = backupRepository.importBackup(uri)
                isImporting = false
                if (success) {
                    // Reload all data in ViewModel after successful import
                    viewModel.reloadData()
                }
                Toast.makeText(
                    context,
                    if (success) "Backup restored successfully" else "Import failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

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

    // Center on player when trigger changes (re-enable follow mode at 5x)
    LaunchedEffect(uiState.centerOnPlayerTrigger) {
        if (uiState.centerOnPlayerTrigger > 0 && uiState.player != null) {
            mapState.scale = 5f
            centerMapOnPlayer(mapState, uiState.player!!)
            isFollowingPlayer = true
        }
    }

    // Initialize position at 5x on first data received
    LaunchedEffect(uiState.player, hasInitializedPosition) {
        if (uiState.player != null && !hasInitializedPosition) {
            mapState.scale = 5f
            centerMapOnPlayer(mapState, uiState.player!!)
            hasInitializedPosition = true
        }
    }

    // Follow player mode - update map position when player moves
    LaunchedEffect(uiState.player, isFollowingPlayer) {
        if (isFollowingPlayer && uiState.player != null) {
            centerMapOnPlayer(mapState, uiState.player!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SubnauticaColors.OceanDeep.copy(alpha = 0.9f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                actions = {
                    // Visibility toggle (eye button)
                    Box {
                        val visibility = uiState.visibility
                        val allVisible = visibility.showPlayer && visibility.showBeacons && visibility.showVehicles

                        IconButton(onClick = { showVisibilityMenu = true }) {
                            Icon(
                                if (allVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle visibility",
                                tint = if (allVisible) SubnauticaColors.BioluminescentBlue else Color.White.copy(alpha = 0.5f)
                            )
                        }

                        DropdownMenu(
                            expanded = showVisibilityMenu,
                            onDismissRequest = { showVisibilityMenu = false }
                        ) {
                            // Player visibility
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Person,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (visibility.showPlayer) SubnauticaColors.BioluminescentBlue else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Player")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = visibility.showPlayer,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SubnauticaColors.BioluminescentBlue
                                            )
                                        )
                                    }
                                },
                                onClick = { viewModel.togglePlayerVisibility() }
                            )
                            // Beacons visibility
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (visibility.showBeacons) SubnauticaColors.CoralOrange else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Beacons")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = visibility.showBeacons,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SubnauticaColors.BioluminescentBlue
                                            )
                                        )
                                    }
                                },
                                onClick = { viewModel.toggleBeaconsVisibility() }
                            )
                            // Vehicles visibility
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.DirectionsBoat,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (visibility.showVehicles) SubnauticaColors.BioluminescentGreen else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Vehicles")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = visibility.showVehicles,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SubnauticaColors.BioluminescentBlue
                                            )
                                        )
                                    }
                                },
                                onClick = { viewModel.toggleVehiclesVisibility() }
                            )
                            // Custom markers visibility
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (visibility.showCustomMarkers) SubnauticaColors.CoralOrange else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("My Markers")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = visibility.showCustomMarkers,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SubnauticaColors.BioluminescentBlue
                                            )
                                        )
                                    }
                                },
                                onClick = { viewModel.toggleCustomMarkersVisibility() }
                            )
                        }
                    }

                    // Map settings (detailed/blank + layer selector)
                    Box {
                        IconButton(onClick = { showMapMenu = true }) {
                            Icon(
                                Icons.Default.Layers,
                                contentDescription = "Map settings",
                                tint = if (uiState.useDetailedMap || uiState.layerOverride != null)
                                    SubnauticaColors.BioluminescentGreen else Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMapMenu,
                            onDismissRequest = { showMapMenu = false }
                        ) {
                            // Detailed map toggle
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (uiState.useDetailedMap) Icons.Default.Image else Icons.Default.GridOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = if (uiState.useDetailedMap) SubnauticaColors.BioluminescentGreen else Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Detailed Map")
                                        Spacer(modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = uiState.useDetailedMap,
                                            onCheckedChange = null,
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = SubnauticaColors.BioluminescentGreen
                                            )
                                        )
                                    }
                                },
                                onClick = { viewModel.toggleDetailedMap() }
                            )

                            HorizontalDivider()

                            // Layer selector header
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Map,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = SubnauticaColors.BioluminescentBlue
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Map Layer", fontWeight = FontWeight.Bold)
                                    }
                                },
                                onClick = { },
                                enabled = false
                            )

                            // Auto-detect option
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(28.dp))
                                        Text("Auto (from biome)")
                                        Spacer(modifier = Modifier.weight(1f))
                                        if (uiState.layerOverride == null) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = SubnauticaColors.BioluminescentBlue
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setLayerOverride(null)
                                    showMapMenu = false
                                }
                            )

                            // Each layer option
                            MapLayer.entries.forEach { layer ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Spacer(modifier = Modifier.width(28.dp))
                                            Text(layer.displayName)
                                            Spacer(modifier = Modifier.weight(1f))
                                            if (uiState.layerOverride == layer) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(20.dp),
                                                    tint = SubnauticaColors.BioluminescentBlue
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        viewModel.setLayerOverride(layer)
                                        showMapMenu = false
                                    }
                                )
                            }
                        }
                    }

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
                                            modifier = Modifier.size(20.dp),
                                            tint = SubnauticaColors.WarningRed
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Reset Exploration", color = SubnauticaColors.WarningRed)
                                    }
                                },
                                onClick = {
                                    showFogMenu = false
                                    showResetFogConfirmDialog = true
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

                    // Settings menu (gear icon)
                    Box {
                        IconButton(onClick = { showSettingsMenu = true }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showSettingsMenu,
                            onDismissRequest = { showSettingsMenu = false }
                        ) {
                            // About
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = SubnauticaColors.BioluminescentBlue
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("About")
                                    }
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    onAbout()
                                }
                            )

                            HorizontalDivider()

                            // Export backup
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isExporting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = SubnauticaColors.BioluminescentGreen
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Upload,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = SubnauticaColors.BioluminescentGreen
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Export Backup")
                                    }
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    exportLauncher.launch(backupRepository.getBackupFileName())
                                },
                                enabled = !isExporting && !isImporting
                            )

                            // Import backup
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (isImporting) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp,
                                                color = SubnauticaColors.CoralOrange
                                            )
                                        } else {
                                            Icon(
                                                Icons.Default.Download,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = SubnauticaColors.CoralOrange
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Restore Backup")
                                    }
                                },
                                onClick = {
                                    showSettingsMenu = false
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                                enabled = !isExporting && !isImporting
                            )
                        }
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
            // Interactive Map - uses current layer's map URL and explored chunks
            InteractiveMapView(
                mapUrl = uiState.currentMapUrl,
                player = uiState.player,
                beacons = uiState.beacons,
                vehicles = uiState.vehicles,
                customMarkers = uiState.currentLayerCustomMarkers,
                mapState = mapState,
                exploredChunks = uiState.currentExploredChunks,
                fogOfWarEnabled = uiState.fogOfWarEnabled,
                hasReceivedData = uiState.hasReceivedData,
                isFollowingPlayer = isFollowingPlayer,
                onCenterOnPlayer = { viewModel.centerOnPlayer() },
                onUserInteraction = { isFollowingPlayer = false },
                onAddMarker = {
                    newMarkerName = ""
                    newMarkerColor = 0
                    showAddMarkerDialog = true
                },
                onCustomMarkerDelete = { viewModel.deleteCustomMarker(it) },
                onCustomMarkerEdit = { marker ->
                    markerToEdit = marker
                    editMarkerName = marker.label
                    editMarkerColor = marker.colorIndex
                    showEditMarkerDialog = true
                },
                modifier = Modifier.fillMaxSize(),
                showPlayer = uiState.visibility.showPlayer,
                showBeacons = uiState.visibility.showBeacons,
                showVehicles = uiState.visibility.showVehicles,
                showCustomMarkers = uiState.visibility.showCustomMarkers
            )

            // Info overlay panel (top-left)
            if (showInfoPanel) {
                InfoOverlayPanel(
                    player = uiState.player,
                    time = uiState.time,
                    connectionStatus = uiState.connectionStatus,
                    currentLayer = uiState.currentLayer,
                    explorationStats = uiState.explorationStats,
                    fogOfWarEnabled = uiState.fogOfWarEnabled,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                )
            }
        }
    }

    // Add marker dialog
    if (showAddMarkerDialog) {
        AlertDialog(
            onDismissRequest = { showAddMarkerDialog = false },
            title = {
                Text(
                    "Save Current Position",
                    color = Color.White
                )
            },
            text = {
                Column {
                    Text(
                        "Save your current position as a marker on the map.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newMarkerName,
                        onValueChange = { newMarkerName = it },
                        label = { Text("Marker name") },
                        placeholder = { Text("e.g., Resource deposit") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = SubnauticaColors.BioluminescentBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Color:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(8) { colorIndex ->
                            val isSelected = newMarkerColor == colorIndex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SubnauticaColors.getBeaconColor(colorIndex))
                                    .then(
                                        if (isSelected) Modifier.background(
                                            Color.White.copy(alpha = 0.3f),
                                            CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { newMarkerColor = colorIndex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    uiState.player?.let { player ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Position: X=%.0f, Z=%.0f\nDepth: %.1f m".format(
                                player.position.xFloat,
                                player.position.zFloat,
                                player.depthFloat
                            ),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMarkerName.isNotBlank()) {
                            viewModel.saveCurrentPositionAsMarker(newMarkerName.trim(), newMarkerColor)
                            showAddMarkerDialog = false
                        }
                    },
                    enabled = newMarkerName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SubnauticaColors.BioluminescentBlue
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMarkerDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SubnauticaColors.OceanDeep,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Reset fog of war confirmation dialog
    if (showResetFogConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetFogConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = SubnauticaColors.WarningRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    "Reset Exploration?",
                    color = Color.White
                )
            },
            text = {
                Text(
                    "This will erase all your exploration progress for ALL map layers. This action cannot be undone.",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetFogOfWar()
                        showResetFogConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SubnauticaColors.WarningRed
                    )
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetFogConfirmDialog = false }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SubnauticaColors.OceanDeep,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }

    // Edit marker dialog
    if (showEditMarkerDialog && markerToEdit != null) {
        AlertDialog(
            onDismissRequest = {
                showEditMarkerDialog = false
                markerToEdit = null
            },
            title = {
                Text(
                    "Edit Marker",
                    color = Color.White
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = editMarkerName,
                        onValueChange = { editMarkerName = it },
                        label = { Text("Marker name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                            focusedLabelColor = SubnauticaColors.BioluminescentBlue,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            cursorColor = SubnauticaColors.BioluminescentBlue
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Color:",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        repeat(8) { colorIndex ->
                            val isSelected = editMarkerColor == colorIndex
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(SubnauticaColors.getBeaconColor(colorIndex))
                                    .then(
                                        if (isSelected) Modifier.background(
                                            Color.White.copy(alpha = 0.3f),
                                            CircleShape
                                        ) else Modifier
                                    )
                                    .clickable { editMarkerColor = colorIndex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    markerToEdit?.let { marker ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Position: X=%.0f, Z=%.0f\nDepth: %.1f m".format(
                                marker.x,
                                marker.z,
                                marker.depth
                            ),
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editMarkerName.isNotBlank() && markerToEdit != null) {
                            val updated = markerToEdit!!.copy(
                                label = editMarkerName.trim(),
                                colorIndex = editMarkerColor
                            )
                            viewModel.updateCustomMarker(updated)
                            showEditMarkerDialog = false
                            markerToEdit = null
                        }
                    },
                    enabled = editMarkerName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SubnauticaColors.BioluminescentBlue
                    )
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEditMarkerDialog = false
                    markerToEdit = null
                }) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = SubnauticaColors.OceanDeep,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

/**
 * Center the map on player position (keeps current scale)
 * Uses the actual map size from mapState for accurate centering
 */
private fun centerMapOnPlayer(mapState: MapState, player: PlayerInfo) {
    // Skip if container not measured yet
    if (mapState.effectiveMapSize <= 0f) return

    val normalizedX = (player.position.xFloat + WORLD_HALF) / WORLD_SIZE
    val normalizedZ = (WORLD_HALF - player.position.zFloat) / WORLD_SIZE

    // Use the actual effective map size for accurate centering
    mapState.offsetX = (0.5f - normalizedX) * mapState.effectiveMapSize * mapState.scale
    mapState.offsetY = (0.5f - normalizedZ) * mapState.effectiveMapSize * mapState.scale
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
    currentLayer: MapLayer,
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
                        .rotate(normalizeHeading(player?.heading ?: 0f))
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
                        normalizeHeading(player.heading),
                        getHeadingDirection(player.heading)
                    )
                )

                // Exploration progress and layer info
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(4.dp))

                // Show current map layer
                InfoRow(
                    label = "Map",
                    value = currentLayer.displayName,
                    valueColor = if (fogOfWarEnabled) SubnauticaColors.BioluminescentBlue else SubnauticaColors.BioluminescentGreen
                )

                if (fogOfWarEnabled && explorationStats != null) {
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

/**
 * Normalize heading to 0-360 range (handles negative values from API)
 */
private fun normalizeHeading(heading: Float): Float {
    return ((heading % 360) + 360) % 360
}

private fun getHeadingDirection(heading: Float): String {
    val normalizedHeading = normalizeHeading(heading)
    return when {
        normalizedHeading < 22.5 || normalizedHeading >= 337.5 -> "N"
        normalizedHeading < 67.5 -> "NE"
        normalizedHeading < 112.5 -> "E"
        normalizedHeading < 157.5 -> "SE"
        normalizedHeading < 202.5 -> "S"
        normalizedHeading < 247.5 -> "SW"
        normalizedHeading < 292.5 -> "W"
        else -> "NW"
    }
}

private fun formatBiomeName(biome: String): String {
    return biome.replace(Regex("([a-z])([A-Z])")) { "${it.groupValues[1]} ${it.groupValues[2]}" }
        .replaceFirstChar { it.uppercase() }
}
