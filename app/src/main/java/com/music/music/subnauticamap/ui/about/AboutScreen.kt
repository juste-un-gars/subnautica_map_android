/**
 * @file AboutScreen.kt
 * @description About screen with credits, app information, and backup/restore
 * @session SESSION_008
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.ui.about

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.music.subnauticamap.data.repository.BackupRepository
import com.music.music.subnauticamap.data.repository.CustomMarkersRepository
import com.music.music.subnauticamap.data.repository.FogOfWarRepository
import com.music.music.subnauticamap.ui.theme.SubnauticaColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Repositories for backup
    val fogOfWarRepository = remember { FogOfWarRepository(context) }
    val customMarkersRepository = remember { CustomMarkersRepository(context) }
    val backupRepository = remember {
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
                Toast.makeText(
                    context,
                    if (success) "Backup restored successfully" else "Import failed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
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
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SubnauticaColors.OceanDeep,
                            SubnauticaColors.OceanMedium
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App title
                Text(
                    text = "Subnautica Map",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubnauticaColors.BioluminescentBlue,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Companion App",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Version 1.0.0",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Description
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SubnauticaColors.OceanLight.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "A companion app for Subnautica that displays an interactive map with real-time player position, beacons, and vehicles.",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Credits section
                Text(
                    text = "Credits",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubnauticaColors.BioluminescentBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Map Images Credit
                CreditCard(
                    title = "Map Images",
                    description = "Multi-layer Subnautica maps",
                    author = "Rocketsoup",
                    url = "https://blog.rocketsoup.net/2024/07/16/subnautica-maps/",
                    icon = Icons.Default.Map,
                    onOpenUrl = { uriHandler.openUri(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Windows Mod Credits
                CreditCard(
                    title = "MapAPI Mod",
                    description = "Subnautica mod that exposes game data via HTTP",
                    author = "juste-un-gars",
                    url = "https://github.com/juste-un-gars/subnautica_map_windows",
                    icon = Icons.Default.Computer,
                    onOpenUrl = { uriHandler.openUri(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Modding Framework Credits
                CreditCard(
                    title = "Modding Frameworks",
                    description = "BepInEx plugin framework & Nautilus modding API",
                    author = "BepInEx Team & Subnautica Modding",
                    url = "https://github.com/BepInEx/BepInEx",
                    icon = Icons.Default.Build,
                    onOpenUrl = { uriHandler.openUri(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // HTTP Server
                CreditCard(
                    title = "Embedded HTTP Server",
                    description = "Lightweight HTTP server for Unity/Mono",
                    author = "EmbedIO",
                    url = "https://github.com/unosquare/embedio",
                    icon = Icons.Default.Cloud,
                    onOpenUrl = { uriHandler.openUri(it) }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Disclaimer
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SubnauticaColors.OceanLight.copy(alpha = 0.2f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Disclaimer",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "This app is not affiliated with Unknown Worlds Entertainment. Subnautica is a trademark of Unknown Worlds Entertainment.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            lineHeight = 18.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Backup & Restore section
                Text(
                    text = "Backup & Restore",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = SubnauticaColors.BioluminescentBlue,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Export or import your exploration data, custom markers, and settings.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Export button
                    Button(
                        onClick = {
                            exportLauncher.launch(backupRepository.getBackupFileName())
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SubnauticaColors.BioluminescentGreen
                        )
                    ) {
                        if (isExporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Upload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }

                    // Import button
                    OutlinedButton(
                        onClick = {
                            importLauncher.launch(arrayOf("application/json"))
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isExporting && !isImporting,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SubnauticaColors.CoralOrange
                        ),
                        border = ButtonDefaults.outlinedButtonBorder(
                            enabled = true
                        ).copy(
                            brush = Brush.linearGradient(
                                colors = listOf(SubnauticaColors.CoralOrange, SubnauticaColors.CoralOrange)
                            )
                        )
                    ) {
                        if (isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = SubnauticaColors.CoralOrange,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun CreditCard(
    title: String,
    description: String,
    author: String,
    url: String,
    icon: ImageVector,
    onOpenUrl: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = SubnauticaColors.OceanLight.copy(alpha = 0.4f)
        ),
        onClick = { onOpenUrl(url) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SubnauticaColors.BioluminescentBlue,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Text(
                    text = "by $author",
                    fontSize = 11.sp,
                    color = SubnauticaColors.BioluminescentGreen.copy(alpha = 0.8f)
                )
            }

            Icon(
                imageVector = Icons.Default.OpenInNew,
                contentDescription = "Open link",
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
