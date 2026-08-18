package com.winnyking.bookstackcompanion.ui.screens

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.data.datastore.FontSize
import com.winnyking.bookstackcompanion.data.datastore.ThemeMode
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.model.Page
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.FavoriteRepository
import com.winnyking.bookstackcompanion.domain.repository.HistoryRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.components.OfflineBanner
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
class PageReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val bookStackRepository: BookStackRepository,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    val userPreferencesManager: UserPreferencesManager
) : ViewModel() {

    val pageId: Long = checkNotNull(savedStateHandle["pageId"])

    val selectedServer: StateFlow<ServerConfig?> = serverRepository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val themeMode: StateFlow<ThemeMode> = userPreferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.SYSTEM)

    val fontSize: StateFlow<FontSize> = userPreferencesManager.fontSize
        .stateIn(viewModelScope, SharingStarted.Lazily, FontSize.NORMAL)

    private val _page = MutableStateFlow<Page?>(null)
    val page: StateFlow<Page?> = _page

    val isFavorite: StateFlow<Boolean> = selectedServer.flatMapLatest { server ->
        if (server != null) favoriteRepository.isFavorite(server.id, pageId) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    fun loadPage(forceRemote: Boolean = false) {
        val server = selectedServer.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = bookStackRepository.getPageDetail(server.id, pageId, forceRemote)
            if (result.isSuccess) {
                val fetchedPage = result.getOrNull()
                _page.value = fetchedPage
                _isOffline.value = fetchedPage?.isCached == true && forceRemote

                if (fetchedPage != null) {
                    historyRepository.addHistory(
                        serverId = server.id,
                        pageId = fetchedPage.id,
                        pageName = fetchedPage.name,
                        bookName = "Livre",
                        chapterName = ""
                    )
                }
            } else {
                _isOffline.value = true
            }
            _isLoading.value = false
        }
    }

    fun toggleFavorite() {
        val server = selectedServer.value ?: return
        val current = _page.value ?: return
        viewModelScope.launch {
            favoriteRepository.toggleFavorite(
                serverId = server.id,
                pageId = current.id,
                pageName = current.name,
                bookName = "Livre",
                chapterName = ""
            )
        }
    }

    fun cycleFontSize() {
        viewModelScope.launch {
            val nextSize = when (fontSize.value) {
                FontSize.SMALL -> FontSize.NORMAL
                FontSize.NORMAL -> FontSize.LARGE
                FontSize.LARGE -> FontSize.SMALL
            }
            userPreferencesManager.setFontSize(nextSize)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageReaderScreen(
    onNavigateBack: () -> Unit,
    viewModel: PageReaderViewModel = hiltViewModel()
) {
    val page by viewModel.page.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOffline by viewModel.isOffline.collectAsState()
    val selectedServer by viewModel.selectedServer.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()

    val isDarkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LaunchedEffect(selectedServer) {
        if (selectedServer != null) {
            viewModel.loadPage()
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background.toArgb()
    val textColor = MaterialTheme.colorScheme.onBackground.toArgb()

    val htmlFontCss = when (fontSize) {
        FontSize.SMALL -> "font-size: 14px;"
        FontSize.NORMAL -> "font-size: 17px;"
        FontSize.LARGE -> "font-size: 21px;"
    }

    val htmlThemeCss = if (isDarkTheme) {
        "body { background-color: #121212; color: #E0E0E0; $htmlFontCss font-family: sans-serif; padding: 16px; line-height: 1.6; } a { color: #80DEEA; } code, pre { background-color: #1E1E1E; padding: 4px; border-radius: 4px; }"
    } else {
        "body { background-color: #FFFFFF; color: #212121; $htmlFontCss font-family: sans-serif; padding: 16px; line-height: 1.6; } a { color: #0288D1; } code, pre { background-color: #F5F5F5; padding: 4px; border-radius: 4px; }"
    }

    val styledHtml = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
                $htmlThemeCss
                img { max-width: 100%; height: auto; border-radius: 8px; }
                table { width: 100%; border-collapse: collapse; margin: 12px 0; }
                th, td { border: 1px solid #888; padding: 8px; text-align: left; }
                blockquote { border-left: 4px solid #0288D1; margin: 12px 0; padding-left: 12px; font-style: italic; }
            </style>
        </head>
        <body>
            ${page?.htmlContent ?: "<p>Aucun contenu disponible</p>"}
        </body>
        </html>
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page?.name ?: "Lecture", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.cycleFontSize() }) {
                        Icon(Icons.Default.FormatSize, contentDescription = "Taille du texte")
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Favori",
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.loadPage(forceRemote = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Rafraîchir")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isOffline) {
                OfflineBanner()
            }

            if (isLoading && page == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(
                            selectedServer?.baseUrl,
                            styledHtml,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                )
            }
        }
    }
}
