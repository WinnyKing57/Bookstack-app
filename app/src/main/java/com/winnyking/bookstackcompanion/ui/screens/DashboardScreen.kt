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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.HistoryItem
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.FavoriteRepository
import com.winnyking.bookstackcompanion.domain.repository.HistoryRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.domain.usecase.SyncServerUseCase
import com.winnyking.bookstackcompanion.ui.components.ServerSelectorDropdown
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val bookStackRepository: BookStackRepository,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    private val syncServerUseCase: SyncServerUseCase
) : ViewModel() {

    val allServers: StateFlow<List<ServerConfig>> = serverRepository.getAllServers()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val selectedServer: StateFlow<ServerConfig?> = serverRepository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val books: StateFlow<List<Book>> = selectedServer.flatMapLatest { server ->
        if (server != null) bookStackRepository.getBooks(server.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val history: StateFlow<List<HistoryItem>> = selectedServer.flatMapLatest { server ->
        if (server != null) historyRepository.getHistory(server.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectServer(server: ServerConfig) {
        viewModelScope.launch {
            serverRepository.selectServer(server.id)
        }
    }

    fun syncCurrentServer() {
        val server = selectedServer.value ?: return
        viewModelScope.launch {
            syncServerUseCase(server.id)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToBooks: () -> Unit,
    onNavigateToShelves: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToBookDetail: (Long) -> Unit,
    onNavigateToPageReader: (Long) -> Unit,
    onNavigateToConnectServer: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val selectedServer by viewModel.selectedServer.collectAsState()
    val allServers by viewModel.allServers.collectAsState()
    val books by viewModel.books.collectAsState()
    val history by viewModel.history.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ServerSelectorDropdown(
                        servers = allServers,
                        selectedServer = selectedServer,
                        onServerSelected = { viewModel.selectServer(it) },
                        onAddNewServer = onNavigateToConnectServer
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.syncCurrentServer() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Synchroniser")
                    }
                }
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
                Text(
                    text = "Accès rapide",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShortcutCard(
                        title = "Livres",
                        icon = Icons.Default.Book,
                        onClick = onNavigateToBooks,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = "Étagères",
                        icon = Icons.Default.FolderSpecial,
                        onClick = onNavigateToShelves,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = "Recherche",
                        icon = Icons.Default.Search,
                        onClick = onNavigateToSearch,
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = "Favoris",
                        icon = Icons.Default.Bookmark,
                        onClick = onNavigateToFavorites,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (history.isNotEmpty()) {
                item {
                    Text(
                        text = "Pages récentes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                items(history.take(5)) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToPageReader(item.pageId) },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = item.pageName,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${item.bookName} • ${item.chapterName.ifBlank { "Page directe" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (books.isNotEmpty()) {
                item {
                    Text(
                        text = "Aperçu des Livres",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(books.take(8)) { book ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(180.dp)
                                    .clickable { onNavigateToBookDetail(book.id) }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxSize(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Book,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = book.name,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
