package com.winnyking.bookstackcompanion

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class BookStackApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var secureStorageManager: SecureStorageManager

    @Inject
    lateinit var serverRepository: ServerRepository

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request()
                val server = runBlocking { serverRepository.getSelectedServer().firstOrNull() }
                if (server != null) {
                    val tokenId = secureStorageManager.getTokenId(server.id)
                    val tokenSecret = secureStorageManager.getTokenSecret(server.id)
                    if (tokenId.isNotBlank() && tokenSecret.isNotBlank()) {
                        val authRequest = request.newBuilder()
                            .header("Authorization", "Token $tokenId:$tokenSecret")
                            .build()
                        return@addInterceptor chain.proceed(authRequest)
                    }
                }
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
