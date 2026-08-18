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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.BuildConfig
import com.winnyking.bookstackcompanion.R
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

    val historyLimit: StateFlow<Int> = userPreferencesManager.historyLimit
        .stateIn(viewModelScope, SharingStarted.Lazily, UserPreferencesManager.DEFAULT_HISTORY_LIMIT)

    val cachedPagesCount: StateFlow<Int> = selectedServer.flatMapLatest { server ->
        if (server != null) bookStackRepository.getCachedPagesCount(server.id) else flowOf(0)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val cachedPagesTotalBytes: StateFlow<Long> = selectedServer.flatMapLatest { server ->
        if (server != null) bookStackRepository.getCachedPagesTotalBytes(server.id) else flowOf(0L)
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0L)

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

    fun setHistoryLimit(limit: Int) {
        viewModelScope.launch { userPreferencesManager.setHistoryLimit(limit) }
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
    val historyLimit by viewModel.historyLimit.collectAsState()
    val cachedCount by viewModel.cachedPagesCount.collectAsState()

    var serverToDelete by remember { mutableStateOf<ServerConfig?>(null) }
    var showClearCacheDialog by remember { mutableStateOf(false) }

    serverToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(stringResource(R.string.delete_server_dialog_title)) },
            text = { Text(stringResource(R.string.delete_server_dialog_message, server.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteServer(server.id)
                        serverToDelete = null
                    }
                ) {
                    Text(stringResource(R.string.confirm_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text(stringResource(R.string.clear_cache_dialog_title)) },
            text = { Text(stringResource(R.string.clear_cache_dialog_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text(stringResource(R.string.confirm_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) }
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
                Text(stringResource(R.string.settings_servers_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }

            items(allServers) { server ->
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectServer(server.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.elevatedCardColors(
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
                        IconButton(onClick = { serverToDelete = server }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.settings_delete_server), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            item {
                OutlinedButton(onClick = onNavigateToConnectServer, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_add_server))
                }
            }

            item {
                Text(stringResource(R.string.settings_appearance_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.SYSTEM, onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) })
                            Text(stringResource(R.string.settings_theme_system))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.LIGHT, onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) })
                            Text(stringResource(R.string.settings_theme_light))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = themeMode == ThemeMode.DARK, onClick = { viewModel.setThemeMode(ThemeMode.DARK) })
                            Text(stringResource(R.string.settings_theme_dark))
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.settings_font_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.SMALL, onClick = { viewModel.setFontSize(FontSize.SMALL) })
                            Text(stringResource(R.string.settings_font_small))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.NORMAL, onClick = { viewModel.setFontSize(FontSize.NORMAL) })
                            Text(stringResource(R.string.settings_font_normal))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = fontSize == FontSize.LARGE, onClick = { viewModel.setFontSize(FontSize.LARGE) })
                            Text(stringResource(R.string.settings_font_large))
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.settings_history_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.settings_history_limit), style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        UserPreferencesManager.HISTORY_LIMIT_OPTIONS.forEach { option ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = historyLimit == option,
                                    onClick = { viewModel.setHistoryLimit(option) }
                                )
                                Text(stringResource(R.string.settings_history_limit_value, option))
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.settings_cache_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val cachedBytes by viewModel.cachedPagesTotalBytes.collectAsState()
                        val cacheSizeMb = String.format(Locale.getDefault(), "%.2f Mo", cachedBytes / (1024.0 * 1024.0))
                        Text(stringResource(R.string.settings_cached_pages, cachedCount) + " ($cacheSizeMb)")
                        val lastSyncStr = selectedServer?.lastSyncTimestamp?.let { ts ->
                            if (ts > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ts)) else stringResource(R.string.settings_sync_never)
                        } ?: stringResource(R.string.settings_sync_never)
                        Text(stringResource(R.string.settings_last_sync, lastSyncStr))
                        Spacer(modifier = Modifier.height(12.dp))
                        Row {
                            Button(onClick = { viewModel.syncNow() }, modifier = Modifier.weight(1f).padding(end = 4.dp)) {
                                Text(stringResource(R.string.settings_sync_now))
                            }
                            OutlinedButton(onClick = { showClearCacheDialog = true }, modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(stringResource(R.string.settings_clear_cache))
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.settings_about_section), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.settings_developer))
                        Text(stringResource(R.string.settings_website))
                    }
                }
            }
        }
    }
}
