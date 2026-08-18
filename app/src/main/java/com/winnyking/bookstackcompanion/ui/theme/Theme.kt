package com.winnyking.bookstackcompanion.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.winnyking.bookstackcompanion.data.datastore.ThemeMode

val BookStackBlue = Color(0xFF0288D1)
val BookStackBlueDark = Color(0xFF005B9f)
val BookStackAccent = Color(0xFF00ACC1)

private val DarkColorScheme = darkColorScheme(
    primary = BookStackAccent,
    secondary = BookStackBlue,
    tertiary = Color(0xFF80DEEA)
)

private val LightColorScheme = lightColorScheme(
    primary = BookStackBlue,
    secondary = BookStackBlueDark,
    tertiary = BookStackAccent
)

@Composable
fun BookStackTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
