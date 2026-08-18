package com.winnyking.bookstackcompanion.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
        val HISTORY_LIMIT = intPreferencesKey("history_limit")
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

    val historyLimit: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[Keys.HISTORY_LIMIT] ?: DEFAULT_HISTORY_LIMIT
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

    suspend fun setHistoryLimit(limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY_LIMIT] = limit
        }
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 100
        val HISTORY_LIMIT_OPTIONS = listOf(50, 100, 200, 500)
    }
}
