package com.winnyking.bookstackcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.navigation.AppNavigation
import com.winnyking.bookstackcompanion.ui.navigation.Screen
import com.winnyking.bookstackcompanion.ui.theme.BookStackTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedServer = runBlocking {
            serverRepository.getSelectedServer().first()
        }

        val startDestination = if (selectedServer != null) {
            Screen.Dashboard.route
        } else {
            Screen.ServerConnect.route
        }

        setContent {
            val themeMode by userPreferencesManager.themeMode.collectAsState(initial = com.winnyking.bookstackcompanion.data.datastore.ThemeMode.SYSTEM)

            BookStackTheme(themeMode = themeMode) {
                AppNavigation(startDestination = startDestination)
            }
        }
    }
}
