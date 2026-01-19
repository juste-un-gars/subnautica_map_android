/**
 * @file GameStateRepository.kt
 * @description Repository for fetching game state from MapAPI
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.repository

import com.music.music.subnauticamap.data.api.ApiClient
import com.music.music.subnauticamap.data.api.GameState
import com.music.music.subnauticamap.data.api.MapApiService
import com.music.music.subnauticamap.data.api.PingResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result wrapper for API calls
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

/**
 * Repository for managing game state data from MapAPI
 */
class GameStateRepository {

    private var apiService: MapApiService? = null
    private var currentIp: String = ""
    private var currentPort: Int = 63030

    /**
     * Configure the API connection
     *
     * @param ipAddress IP address of the PC
     * @param port Port number
     */
    fun configure(ipAddress: String, port: Int) {
        if (ipAddress != currentIp || port != currentPort) {
            currentIp = ipAddress
            currentPort = port
            apiService = ApiClient.create(ipAddress, port)
        }
    }

    /**
     * Test connection to the MapAPI server
     *
     * @return ApiResult with PingResponse or error
     */
    suspend fun testConnection(): ApiResult<PingResponse> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext ApiResult.Error("Not configured")

        try {
            val response = service.ping()
            if (response.isSuccessful) {
                response.body()?.let {
                    ApiResult.Success(it)
                } ?: ApiResult.Error("Empty response")
            } else {
                ApiResult.Error("Server error", response.code())
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Fetch current game state
     *
     * @return ApiResult with GameState or error
     */
    suspend fun getGameState(): ApiResult<GameState> = withContext(Dispatchers.IO) {
        val service = apiService ?: return@withContext ApiResult.Error("Not configured")

        try {
            val response = service.getState()
            when {
                response.isSuccessful -> {
                    response.body()?.let {
                        ApiResult.Success(it)
                    } ?: ApiResult.Error("Empty response")
                }
                response.code() == 503 -> {
                    ApiResult.Error("Player not in game", 503)
                }
                else -> {
                    ApiResult.Error("Server error", response.code())
                }
            }
        } catch (e: Exception) {
            ApiResult.Error(e.message ?: "Connection failed")
        }
    }

    /**
     * Check if repository is configured
     */
    fun isConfigured(): Boolean = apiService != null && currentIp.isNotEmpty()
}
