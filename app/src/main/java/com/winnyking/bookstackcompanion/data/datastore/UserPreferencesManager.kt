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

enum class ReaderFontFamily(val cssValue: String) {
    SANS("sans-serif"),
    SERIF("serif"),
    MONOSPACE("monospace")
}

enum class LineHeight(val cssValue: String) {
    COMPACT("1.4"),
    NORMAL("1.6"),
    RELAXED("1.8"),
    LOOSE("2.0")
}

@Singleton
class UserPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val READER_FONT_FAMILY = stringPreferencesKey("reader_font_family")
        val READER_LINE_HEIGHT = stringPreferencesKey("reader_line_height")
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

    val readerFontFamily: Flow<ReaderFontFamily> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.READER_FONT_FAMILY]) {
            ReaderFontFamily.SERIF.name -> ReaderFontFamily.SERIF
            ReaderFontFamily.MONOSPACE.name -> ReaderFontFamily.MONOSPACE
            else -> ReaderFontFamily.SANS
        }
    }

    val lineHeight: Flow<LineHeight> = context.dataStore.data.map { prefs ->
        when (prefs[Keys.READER_LINE_HEIGHT]) {
            LineHeight.COMPACT.name -> LineHeight.COMPACT
            LineHeight.RELAXED.name -> LineHeight.RELAXED
            LineHeight.LOOSE.name -> LineHeight.LOOSE
            else -> LineHeight.NORMAL
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

    suspend fun setHistoryLimit(limit: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HISTORY_LIMIT] = limit
        }
    }

    suspend fun setReaderFontFamily(family: ReaderFontFamily) {
        context.dataStore.edit { prefs ->
            prefs[Keys.READER_FONT_FAMILY] = family.name
        }
    }

    suspend fun setLineHeight(lineHeight: LineHeight) {
        context.dataStore.edit { prefs ->
            prefs[Keys.READER_LINE_HEIGHT] = lineHeight.name
        }
    }

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 100
        val HISTORY_LIMIT_OPTIONS = listOf(50, 100, 200, 500)
    }
}
