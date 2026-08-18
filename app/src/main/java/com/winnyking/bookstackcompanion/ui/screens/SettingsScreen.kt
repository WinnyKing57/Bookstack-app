package com.winnyking.bookstackcompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.data.datastore.FontSize
import com.winnyking.bookstackcompanion.data.datastore.ThemeMode
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.domain.usecase.SyncServerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val bookStackRepository: BookStackRepository,
    private val syncServerUseCase: SyncServerUseCase,
    private val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val allServers: StateFlow<List<ServerConfig>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedServer: StateFlow<ServerConfig?> = serverRepository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val themeMode: StateFlow<ThemeMode> = userPreferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.SYSTEM)

    val fontSize: StateFlow<FontSize> = userPreferencesManager.fontSize
        .stateIn(viewModelScope, SharingStarted.Lazily, FontSize.NORMAL)

    val cachedPagesCount: StateFlow<Int> = selectedServer.flatMapLatest { server ->
        if (server != null) bookStackRepository.getCachedPagesCount(server.id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    fun selectServer(serverId: String) {
        viewModelScope.launch { serverRepository.selectServer(serverId) }
    }

    fun deleteServer(serverId: String) {
        viewModelScope.launch { serverRepository.deleteServer(serverId) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { userPreferencesManager.setThemeMode(mode) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { userPreferencesManager.setFontSize(size) }
    }

    fun clearCache() {
        val server = selectedServer.value ?: return
        viewModelScope.launch { bookStackRepository.clearCache(server.id) }
    }

    fun syncNow() {
        val server = selectedServer.value ?: return
        viewModelScope.launch { syncServerUseCase(server.id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToConnectServer: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val allServers by viewModel.allServers.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val cachedCount by viewModel.cachedPagesCount.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Serveurs BookStack", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allServers) { server ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectServer(server.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (server.id == selectedServer?.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = server.id == selectedServer?.id,
                            onClick = { viewModel.selectServer(server.id) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(server.name, fontWeight = FontWeight.Bold)
                            Text(server.baseUrl, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { viewModel.deleteServer(server.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = onNavigateToConnectServer, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Ajouter un serveur")
                }
            }

            item {
                Text("Apparence & Thème", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                            Text("Système")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                            Text("Clair")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
                            Text("Sombre")
                        }
                    }
                }
            }

            item {
                Text("Taille du texte", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.SMALL, onClick = { viewModel.setFontSize(FontSize.SMALL) })
                            Text("Petit")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.NORMAL, onClick = { viewModel.setFontSize(FontSize.NORMAL) })
                            Text("Normal")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.LARGE, onClick = { viewModel.setFontSize(FontSize.LARGE) })
                            Text("Grand")
                        }
                    }
                }
            }

            item {
                Text("Cache & Synchronisation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Pages en cache : $cachedCount")
                        val lastSyncStr = selectedServer?.lastSyncTimestamp?.let { ts ->
                            if (ts > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts)) else "Jamais"
                        } ?: "Jamais"
                        Text("Dernière synchro : $lastSyncStr")
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Button(onClick = { viewModel.syncNow() }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                Text("Synchroniser")
                            }
                            OutlinedButton(onClick = { viewModel.clearCache() }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text("Vider le cache")
                            }
                        }
                    }
                }
            }

            item {
                Text("À propos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("WinBook-Stack v1.0.0", fontWeight = FontWeight.Bold)
                        Text("Développeur : WinnyKing")
                        Text("Site web : winnyking.cloud")
                    }
                }
            }
        }
    }
}
