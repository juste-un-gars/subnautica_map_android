/**
 * @file ApiClient.kt
 * @description Retrofit client factory for MapAPI
 * @created 2026-01-19
 */
package com.music.music.subnauticamap.data.api

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory for creating MapApiService instances
 */
object ApiClient {

    private const val DEFAULT_TIMEOUT = 5L // seconds

    /**
     * Create a new MapApiService instance for the given server
     *
     * @param ipAddress IP address of the PC running Subnautica
     * @param port Port number (default 63030)
     * @return MapApiService instance
     */
    fun create(ipAddress: String, port: Int = 63030): MapApiService {
        val baseUrl = "http://$ipAddress:$port/"

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(MapApiService::class.java)
    }
}
