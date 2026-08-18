package com.winnyking.bookstackcompanion.data.api

import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor(
    private val secureStorageManager: SecureStorageManager,
    private val currentServerIdProvider: () -> String?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val serverId = currentServerIdProvider()

        if (serverId.isNull_or_empty()) {
            return chain.proceed(originalRequest)
        }

        val tokenId = secureStorageManager.getTokenId(serverId)
        val tokenSecret = secureStorageManager.getTokenSecret(serverId)

        if (tokenId.isBlank() || tokenSecret.isBlank()) {
            return chain.proceed(originalRequest)
        }

        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Token $tokenId:$tokenSecret")
            .build()

        return chain.proceed(authenticatedRequest)
    }
}

private fun String?.isNull_or_empty(): Boolean = this == null || this.isEmpty()
