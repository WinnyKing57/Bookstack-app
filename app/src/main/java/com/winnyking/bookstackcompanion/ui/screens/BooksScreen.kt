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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.winnyking.bookstackcompanion.R
import com.winnyking.bookstackcompanion.data.work.BookDownloadWorker
import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.components.BookCoverImage
import com.winnyking.bookstackcompanion.ui.components.EmptyState
import com.winnyking.bookstackcompanion.ui.components.SkeletonItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DownloadProgressState(
    val isDownloading: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0
) {
    val progressFraction: Float
        get() = if (total > 0) completed.toFloat() / total.toFloat() else 0f
}

@HiltViewModel
class BooksViewModel @Inject constructor(
    private val serverRepository: ServerRepository,
    private val bookStackRepository: BookStackRepository,
    private val workManager: WorkManager
) : ViewModel() {

    val selectedServer: StateFlow<ServerConfig?> = serverRepository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val books: StateFlow<List<Book>> = selectedServer.flatMapLatest { server ->
        if (server != null) bookStackRepository.getBooks(server.id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _downloadProgressMap = MutableStateFlow<Map<Long, DownloadProgressState>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<Long, DownloadProgressState>> = _downloadProgressMap

    init {
        refresh()
        observeDownloads()
    }

    fun refresh() {
        val server = selectedServer.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            bookStackRepository.refreshBooks(server.id)
            _isLoading.value = false
        }
    }

    fun downloadBook(bookId: Long) {
        val server = selectedServer.value ?: return
        val request = OneTimeWorkRequestBuilder<BookDownloadWorker>()
            .setInputData(
                workDataOf(
                    BookDownloadWorker.KEY_SERVER_ID to server.id,
                    BookDownloadWorker.KEY_BOOK_ID to bookId
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(BookDownloadWorker.WORK_TAG)
            .addTag("${BookDownloadWorker.WORK_TAG}_$bookId")
            .build()
        workManager.enqueueUniqueWork(
            BookDownloadWorker.workName(bookId),
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            workManager.getWorkInfosByTagFlow(BookDownloadWorker.WORK_TAG).collect { infos ->
                val map = buildMap {
                    for (info in infos) {
                        if (info.state.isFinished) continue
                        val bookId = info.tags
                            .firstOrNull { it.startsWith("${BookDownloadWorker.WORK_TAG}_") }
                            ?.removePrefix("${BookDownloadWorker.WORK_TAG}_")
                            ?.toLongOrNull() ?: continue
                        val completed = info.progress.getInt(BookDownloadWorker.KEY_COMPLETED, 0)
                        val total = info.progress.getInt(BookDownloadWorker.KEY_TOTAL, 0)
                        put(bookId, DownloadProgressState(isDownloading = true, completed = completed, total = total))
                    }
                }
                _downloadProgressMap.value = map
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BooksScreen(
    onNavigateToBookDetail: (Long) -> Unit,
    viewModel: BooksViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    val filteredBooks = books.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.books_title)) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
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
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.books_search_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    singleLine = true
                )

                if (isLoading && books.isEmpty()) {
                    LazyColumn {
                        items(5) {
                            SkeletonItem()
                        }
                    }
                } else if (filteredBooks.isEmpty()) {
                    if (searchQuery.isBlank()) {
                        EmptyState(
                            icon = Icons.Default.Book,
                            message = stringResource(R.string.books_empty)
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            message = stringResource(R.string.books_no_results)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredBooks) { book ->
                            val downloadProgress = downloadProgressMap[book.id]
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToBookDetail(book.id) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .padding(16.dp)
                                            .fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BookCoverImage(
                                            coverUrl = book.coverUrl,
                                            baseUrl = selectedServer?.baseUrl,
                                            contentDescription = book.name,
                                            modifier = Modifier.size(50.dp),
                                            fallbackIcon = Icons.Default.Book
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = book.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            if (book.description.isNotBlank()) {
                                                Text(
                                                    text = book.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                        if (downloadProgress?.isDownloading == true) {
                                            CircularProgressIndicator(
                                                progress = { downloadProgress.progressFraction },
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            IconButton(onClick = { viewModel.downloadBook(book.id) }) {
                                                Icon(
                                                    imageVector = if (book.isDownloaded) Icons.Default.CheckCircle else Icons.Default.Download,
                                                    contentDescription = stringResource(R.string.books_download),
                                                    tint = if (book.isDownloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (downloadProgress?.isDownloading == true) {
                                        LinearProgressIndicator(
                                            progress = { downloadProgress.progressFraction },
                                            modifier = Modifier.fillMaxWidth()
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

