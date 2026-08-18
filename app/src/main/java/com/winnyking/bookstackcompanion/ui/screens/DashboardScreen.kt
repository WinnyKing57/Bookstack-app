package com.winnyking.bookstackcompanion.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.R
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.HistoryItem
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.FavoriteRepository
import com.winnyking.bookstackcompanion.domain.repository.HistoryRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.domain.usecase.SyncServerUseCase
import com.winnyking.bookstackcompanion.ui.components.BookCoverImage
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
    private val syncServerUseCase: SyncServerUseCase,
    private val userPreferencesManager: UserPreferencesManager
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

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun selectServer(server: ServerConfig) {
        viewModelScope.launch {
            serverRepository.selectServer(server.id)
        }
    }

    fun syncCurrentServer() {
        val server = selectedServer.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            syncServerUseCase(server.id)
            _isRefreshing.value = false
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

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.syncCurrentServer()
            pullToRefreshState.endRefresh()
        }
    }

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
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.dashboard_sync))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = stringResource(R.string.dashboard_quick_access),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShortcutCard(
                            title = stringResource(R.string.dashboard_books),
                            icon = Icons.Default.Book,
                            onClick = onNavigateToBooks,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            title = stringResource(R.string.dashboard_shelves),
                            icon = Icons.Default.FolderSpecial,
                            onClick = onNavigateToShelves,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            title = stringResource(R.string.dashboard_search),
                            icon = Icons.Default.Search,
                            onClick = onNavigateToSearch,
                            modifier = Modifier.weight(1f)
                        )
                        ShortcutCard(
                            title = stringResource(R.string.dashboard_favorites),
                            icon = Icons.Default.Bookmark,
                            onClick = onNavigateToFavorites,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (history.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.dashboard_recent_pages),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(history.take(5)) { item ->
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToPageReader(item.pageId) },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = item.pageName,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${item.bookName} \u2022 ${item.chapterName.ifBlank { stringResource(R.string.dashboard_direct_page) }}",
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
                            text = stringResource(R.string.dashboard_preview_books),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(books.take(8)) { book ->
                                ElevatedCard(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(190.dp)
                                        .clickable { onNavigateToBookDetail(book.id) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        BookCoverImage(
                                            coverUrl = book.coverUrl,
                                            baseUrl = selectedServer?.baseUrl,
                                            contentDescription = book.name,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp),
                                            fallbackIcon = Icons.Default.Book
                                        )
                                        Text(
                                            text = book.name,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
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
    ElevatedCard(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
