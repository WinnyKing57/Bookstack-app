package com.winnyking.bookstackcompanion.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "user_settings")

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

enum class FontSize {
    SMALL, NORMAL, LARGE
}

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.THEME_MODE]) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val fontSize: Flow<FontSize> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.FONT_SIZE]) {
            FontSize.SMALL.name -> FontSize.SMALL
            FontSize.LARGE.name -> FontSize.LARGE
            else -> FontSize.NORMAL
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setFontSize(size: FontSize) {
        context.dataStore.edit { prefs ->
            prefs[Keys.FONT_SIZE] = size.name
        }
    }
}
