package com.portfoliox.data.market.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import java.util.concurrent.TimeUnit

/**
 * JSON settings shared by the API client and tests.
 * ignoreUnknownKeys: if the server adds a field later, older app versions keep working.
 */
val MarketJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Builds the Retrofit client.
 * - OkHttp does the actual HTTP. Short timeouts make "server not running" fail fast.
 * - The logging interceptor prints each request to Logcat in debug builds only.
 * - The converter parses JSON bodies with kotlinx.serialization.
 */
fun createMarketApi(baseUrl: String, enableLogging: Boolean): MarketApi {
    val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .apply {
            if (enableLogging) {
                addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(MarketJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
}
