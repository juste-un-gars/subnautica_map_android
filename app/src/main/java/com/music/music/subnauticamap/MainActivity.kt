/**
 * @file MainActivity.kt
 * @description Main entry point for Subnautica Map Android app
 * @session SESSION_003
 * @created 2026-01-19
 */
package com.music.music.subnauticamap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.music.music.subnauticamap.data.repository.FogOfWarRepository
import com.music.music.subnauticamap.navigation.NavRoutes
import com.music.music.subnauticamap.ui.connection.ConnectionScreen
import com.music.music.subnauticamap.ui.connection.ConnectionViewModel
import com.music.music.subnauticamap.ui.map.MapScreen
import com.music.music.subnauticamap.ui.map.MapViewModel
import com.music.music.subnauticamap.ui.map.MapViewModelFactory
import com.music.music.subnauticamap.ui.theme.SubnauticaColors
import com.music.music.subnauticamap.ui.theme.SubnauticaMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SubnauticaMapTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SubnauticaColors.OceanDeep
                ) {
                    SubnauticaMapApp()
                }
            }
        }
    }
}

@Composable
fun SubnauticaMapApp() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Shared ConnectionViewModel to pass repository to MapViewModel
    val connectionViewModel: ConnectionViewModel = viewModel()

    // Fog of war repository
    val fogOfWarRepository = remember { FogOfWarRepository(context) }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONNECTION
    ) {
        composable(NavRoutes.CONNECTION) {
            ConnectionScreen(
                viewModel = connectionViewModel,
                onConnected = {
                    navController.navigate(NavRoutes.MAP) {
                        popUpTo(NavRoutes.CONNECTION) { inclusive = false }
                    }
                }
            )
        }

        composable(NavRoutes.MAP) {
            val connectionState by connectionViewModel.uiState.collectAsState()
            val mapViewModel: MapViewModel = viewModel(
                factory = MapViewModelFactory(
                    connectionViewModel.gameStateRepository,
                    fogOfWarRepository
                )
            )

            MapScreen(
                viewModel = mapViewModel,
                keepScreenOn = connectionState.keepScreenOn,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
