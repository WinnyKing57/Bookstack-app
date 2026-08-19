package com.winnyking.bookstackcompanion

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.util.DebugLogger
import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class BookStackApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var secureStorageManager: SecureStorageManager

    @Inject
    lateinit var serverRepository: ServerRepository

    @Volatile
    private var authHeader: String? = null

    init {
        CoroutineScope(Dispatchers.IO).launch {
            serverRepository.getSelectedServer().collect { server ->
                authHeader = if (server != null) {
                    val secret = secureStorageManager.getTokenSecret(server.id)
                    "Token ${server.tokenId}:$secret"
                } else null
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val header = authHeader
                if (header != null) {
                    chain.proceed(original.newBuilder().header("Authorization", header).build())
                } else {
                    chain.proceed(original)
                }
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .crossfade(true)
            .build()
    }
}
