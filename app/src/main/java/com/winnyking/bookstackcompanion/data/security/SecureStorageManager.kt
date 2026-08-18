package com.winnyking.bookstackcompanion.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "bookstack_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveServerCredentials(serverId: String, tokenId: String, tokenSecret: String) {
        sharedPreferences.edit()
            .putString("token_id_$serverId", tokenId)
            .putString("token_secret_$serverId", tokenSecret)
            .apply()
    }

    fun getTokenId(serverId: String): String {
        return sharedPreferences.getString("token_id_$serverId", "") ?: ""
    }

    fun getTokenSecret(serverId: String): String {
        return sharedPreferences.getString("token_secret_$serverId", "") ?: ""
    }

    fun deleteServerCredentials(serverId: String) {
        sharedPreferences.edit()
            .remove("token_id_$serverId")
            .remove("token_secret_$serverId")
            .apply()
    }
}
