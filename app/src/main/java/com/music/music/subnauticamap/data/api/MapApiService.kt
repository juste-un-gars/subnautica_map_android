/**
 * @file MapApiService.kt
 * @description Retrofit interface for Subnautica MapAPI
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.api

import retrofit2.Response
import retrofit2.http.GET

/**
 * Retrofit service interface for MapAPI endpoints
 */
interface MapApiService {

    /**
     * Health check endpoint
     * @return PingResponse with status and version
     */
    @GET("/api/ping")
    suspend fun ping(): Response<PingResponse>

    /**
     * Get current game state
     * @return GameState with player, beacons, vehicles, time
     */
    @GET("/api/state")
    suspend fun getState(): Response<GameState>
}
