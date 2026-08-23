package com.winnyking.bookstackcompanion

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.navigation.AppNavigation
import com.winnyking.bookstackcompanion.ui.navigation.Screen
import com.winnyking.bookstackcompanion.ui.theme.BookStackTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var serverRepository: ServerRepository

    @Inject
    lateinit var userPreferencesManager: UserPreferencesManager

    private var startDestination by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            val selectedServer = serverRepository.getSelectedServer().first()
            startDestination = if (selectedServer != null) {
                Screen.Dashboard.route
            } else {
                Screen.ServerConnect.route
            }
        }

        setContent {
            val themeMode by userPreferencesManager.themeMode.collectAsState(initial = com.winnyking.bookstackcompanion.data.datastore.ThemeMode.SYSTEM)

            BookStackTheme(themeMode = themeMode) {
                val destination = startDestination
                if (destination != null) {
                    AppNavigation(startDestination = destination)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
