package com.winnyking.bookstackcompanion.data.api

import com.winnyking.bookstackcompanion.BuildConfig
import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DynamicApiClientFactory @Inject constructor(
    private val secureStorageManager: SecureStorageManager
) {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = false
    }

    private val clientCache = ConcurrentHashMap<String, BookStackApi>()

    fun createApi(baseUrl: String, serverId: String): BookStackApi {
        val sanitizedBase = UrlSanitizer.sanitizeBaseUrl(baseUrl)
        val cacheKey = "$serverId:$sanitizedBase"
        return clientCache.getOrPut(cacheKey) {
            createApiInternal(sanitizedBase, serverId)
        }
    }

    private fun createApiInternal(baseUrl: String, serverId: String): BookStackApi {
        val formattedBaseUrl = "$baseUrl/"

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
            redactHeader("Authorization")
        }

        val authInterceptor = AuthInterceptor(
            secureStorageManager = secureStorageManager,
            currentServerIdProvider = { serverId }
        )

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BookStackApi::class.java)
    }

    suspend fun testConnection(baseUrl: String, tokenId: String, tokenSecret: String): Result<Unit> {
        val sanitizedBase = UrlSanitizer.sanitizeBaseUrl(baseUrl)
        if (sanitizedBase.isBlank()) {
            return Result.failure(IllegalArgumentException("L'URL saisie est vide ou invalide."))
        }
        val formattedBaseUrl = "$sanitizedBase/"
        val contentType = "application/json".toMediaType()

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Authorization", "Token $tokenId:$tokenSecret")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val api = Retrofit.Builder()
            .baseUrl(formattedBaseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(BookStackApi::class.java)

        return try {
            api.getBooks(count = 1)
            Result.success(Unit)
        } catch (e: Exception) {
            val formattedMsg = UrlSanitizer.formatErrorMessage(sanitizedBase, e)
            Result.failure(Exception(formattedMsg, e))
        }
    }

    fun invalidateCache(serverId: String) {
        clientCache.keys.removeAll { it.startsWith("$serverId:") }
    }
}
