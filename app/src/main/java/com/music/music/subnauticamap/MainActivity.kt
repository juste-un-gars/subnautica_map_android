/**
 * @file MainActivity.kt
 * @description Main entry point for Subnautica Map Android app
 * @session SESSION_004
 * @created 2026-01-19
 */
package com.music.music.subnauticamap

import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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

        // Configure fullscreen BEFORE setContent
        setupFullscreen()

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

        // Apply immersive mode after content is set
        hideSystemBars()
    }

    /**
     * Setup window for fullscreen display
     */
    private fun setupFullscreen() {
        // Extend content behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Keep screen awake and make sure we're in fullscreen mode
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)
    }

    /**
     * Hide system bars (status bar and navigation bar) for fullscreen immersive mode
     */
    private fun hideSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.apply {
            // Hide both status bar and navigation bar
            hide(WindowInsetsCompat.Type.systemBars())
            // Use immersive sticky mode - bars appear temporarily on swipe
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Re-hide system bars when window gains focus
        if (hasFocus) {
            hideSystemBars()
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure fullscreen is maintained when returning to the app
        hideSystemBars()
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

    // Double back press to exit
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    val backPressThreshold = 2000L // 2 seconds

    BackHandler {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < backPressThreshold) {
            // Second press within threshold - exit the app
            (context as? ComponentActivity)?.finish()
        } else {
            // First press - show toast
            lastBackPressTime = currentTime
            Toast.makeText(
                context,
                "Press back again to exit",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

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
