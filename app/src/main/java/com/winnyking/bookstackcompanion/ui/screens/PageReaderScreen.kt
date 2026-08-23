package com.winnyking.bookstackcompanion.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.automirrored.filled.Toc
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Toc
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.winnyking.bookstackcompanion.R
import com.winnyking.bookstackcompanion.data.datastore.FontSize
import com.winnyking.bookstackcompanion.data.datastore.LineHeight
import com.winnyking.bookstackcompanion.data.datastore.ReaderFontFamily
import com.winnyking.bookstackcompanion.data.datastore.ThemeMode
import com.winnyking.bookstackcompanion.data.datastore.UserPreferencesManager
import com.winnyking.bookstackcompanion.domain.model.Page
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.FavoriteRepository
import com.winnyking.bookstackcompanion.domain.repository.HistoryRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import com.winnyking.bookstackcompanion.ui.components.OfflineBanner
import com.winnyking.bookstackcompanion.util.HtmlToMarkdownConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TocItem(
    val level: Int,
    val title: String,
    val anchorId: String
)

class ObservableWebView(context: Context) : WebView(context) {
    var onScrollProgressChanged: ((Float) -> Unit)? = null

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        val maxScroll = computeVerticalScrollRange() - height
        val progress = if (maxScroll > 0) (t.toFloat() / maxScroll.toFloat()).coerceIn(0f, 1f) else 0f
        onScrollProgressChanged?.invoke(progress)
    }
}

@HiltViewModel
class PageReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val serverRepository: ServerRepository,
    private val bookStackRepository: BookStackRepository,
    private val favoriteRepository: FavoriteRepository,
    private val historyRepository: HistoryRepository,
    val offlineImageCache: com.winnyking.bookstackcompanion.data.offline.OfflineImageCache,
    val userPreferencesManager: UserPreferencesManager) : ViewModel() {

    private val initialPageId: Long = checkNotNull(savedStateHandle["pageId"])

    private val _currentPageId = MutableStateFlow(initialPageId)
    val currentPageId: StateFlow<Long> = _currentPageId

    val selectedServer: StateFlow<ServerConfig?> = serverRepository.getSelectedServer()
        .stateIn(viewModelScope, SharingStarted.Lazily, null)

    val themeMode: StateFlow<ThemeMode> = userPreferencesManager.themeMode
        .stateIn(viewModelScope, SharingStarted.Lazily, ThemeMode.SYSTEM)

    val fontSize: StateFlow<FontSize> = userPreferencesManager.fontSize
        .stateIn(viewModelScope, SharingStarted.Lazily, FontSize.NORMAL)

    val readerFontFamily: StateFlow<ReaderFontFamily> = userPreferencesManager.readerFontFamily
        .stateIn(viewModelScope, SharingStarted.Lazily, ReaderFontFamily.SANS)

    val lineHeight: StateFlow<LineHeight> = userPreferencesManager.lineHeight
        .stateIn(viewModelScope, SharingStarted.Lazily, LineHeight.NORMAL)

    private val historyLimit: StateFlow<Int> = userPreferencesManager.historyLimit
        .stateIn(viewModelScope, SharingStarted.Lazily, UserPreferencesManager.DEFAULT_HISTORY_LIMIT)

    private val _page = MutableStateFlow<Page?>(null)
    val page: StateFlow<Page?> = _page

    private val _tocItems = MutableStateFlow<List<TocItem>>(emptyList())
    val tocItems: StateFlow<List<TocItem>> = _tocItems

    private val _previousPage = MutableStateFlow<Page?>(null)
    val previousPage: StateFlow<Page?> = _previousPage

    private val _nextPage = MutableStateFlow<Page?>(null)
    val nextPage: StateFlow<Page?> = _nextPage

    val isFavorite: StateFlow<Boolean> = selectedServer.flatMapLatest { server ->
        if (server != null) favoriteRepository.isFavorite(server.id, _currentPageId.value) else flowOf(false)
    }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isOffline = MutableStateFlow(false)
    val isOffline: StateFlow<Boolean> = _isOffline

    fun loadPage(targetPageId: Long = _currentPageId.value, forceRemote: Boolean = false) {
        val server = selectedServer.value ?: return
        viewModelScope.launch {
            _currentPageId.value = targetPageId
            _isLoading.value = true
            val result = bookStackRepository.getPageDetail(server.id, targetPageId, forceRemote)
            if (result.isSuccess) {
                val fetchedPage = result.getOrNull()
                if (fetchedPage != null) {
                    val localImagesHtml = offlineImageCache.rewriteHtmlWithLocalImages(
                        server.id,
                        fetchedPage.bookId,
                        fetchedPage.htmlContent
                    )
                    val processedHtml = processTocAndHtml(localImagesHtml)
                    _page.value = fetchedPage.copy(htmlContent = processedHtml)
                    _isOffline.value = fetchedPage.isCached && forceRemote
                    loadAdjacentPages(server.id, fetchedPage.bookId, targetPageId)

                    historyRepository.addHistory(
                        serverId = server.id,
                        pageId = fetchedPage.id,
                        pageName = fetchedPage.name,
                        bookName = "",
                        chapterName = "",
                        historyLimit = historyLimit.value
                    )
                }
            } else {
                _isOffline.value = true
            }
            _isLoading.value = false
        }
    }

    private fun processTocAndHtml(rawHtml: String): String {
        val regex = Regex("""<(h[1-3])([^>]*)>(.*?)</\1>""", RegexOption.IGNORE_CASE)
        val items = mutableListOf<TocItem>()
        var index = 0

        val processedHtml = regex.replace(rawHtml) { matchResult ->
            val tag = matchResult.groupValues[1].lowercase()
            val attributes = matchResult.groupValues[2]
            val innerContent = matchResult.groupValues[3]
            val cleanTitle = innerContent.replace(Regex("<[^>]*>"), "").trim()

            val anchorId = "toc_$index"
            index++

            val level = when (tag) {
                "h1" -> 1
                "h2" -> 2
                "h3" -> 3
                else -> 1
            }
            if (cleanTitle.isNotBlank()) {
                items.add(TocItem(level = level, title = cleanTitle, anchorId = anchorId))
            }

            "<$tag id=\"$anchorId\"$attributes>$innerContent</$tag>"
        }

        _tocItems.value = items
        return processedHtml
    }

    private fun loadAdjacentPages(serverId: String, bookId: Long, currentPageId: Long) {
        if (bookId <= 0) {
            _previousPage.value = null
            _nextPage.value = null
            return
        }
        viewModelScope.launch {
            val treeResult = bookStackRepository.getBookTree(serverId, bookId)
            if (treeResult.isSuccess) {
                val (chapters, directPages) = treeResult.getOrNull()!!
                val allPages = mutableListOf<Page>()
                chapters.forEach { chapter ->
                    allPages.addAll(chapter.pages)
                }
                allPages.addAll(directPages)

                val currentIndex = allPages.indexOfFirst { it.id == currentPageId }
                if (currentIndex != -1) {
                    _previousPage.value = if (currentIndex > 0) allPages[currentIndex - 1] else null
                    _nextPage.value = if (currentIndex < allPages.size - 1) allPages[currentIndex + 1] else null
                } else {
                    _previousPage.value = null
                    _nextPage.value = null
                }
            }
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
                bookName = "",
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

    fun setReaderFontFamily(family: ReaderFontFamily) {
        viewModelScope.launch { userPreferencesManager.setReaderFontFamily(family) }
    }

    fun setLineHeight(lineHeight: LineHeight) {
        viewModelScope.launch { userPreferencesManager.setLineHeight(lineHeight) }
    }

    fun setFontSize(size: FontSize) {
        viewModelScope.launch { userPreferencesManager.setFontSize(size) }
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
    val readerFontFamily by viewModel.readerFontFamily.collectAsState()
    val lineHeight by viewModel.lineHeight.collectAsState()

    val tocItems by viewModel.tocItems.collectAsState()
    val previousPage by viewModel.previousPage.collectAsState()
    val nextPage by viewModel.nextPage.collectAsState()

    var showTocSheet by remember { mutableStateOf(false) }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var pendingExportFormat by remember { mutableStateOf<ExportFormat?>(null) }
    var readingProgress by remember { mutableFloatStateOf(0f) }
    var webViewInstance by remember { mutableStateOf<ObservableWebView?>(null) }

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

    val htmlFontCss = when (fontSize) {
        FontSize.SMALL -> "font-size: 14px;"
        FontSize.NORMAL -> "font-size: 17px;"
        FontSize.LARGE -> "font-size: 21px;"
    }

    val htmlThemeCss = if (isDarkTheme) {
        "body { background-color: #121212; color: #E0E0E0; $htmlFontCss font-family: ${readerFontFamily.cssValue}; padding: 16px; line-height: ${lineHeight.cssValue}; } a { color: #80DEEA; } code, pre { background-color: #1E1E1E; padding: 4px; border-radius: 4px; }"
    } else {
        "body { background-color: #FFFFFF; color: #212121; $htmlFontCss font-family: ${readerFontFamily.cssValue}; padding: 16px; line-height: ${lineHeight.cssValue}; } a { color: #0288D1; } code, pre { background-color: #F5F5F5; padding: 4px; border-radius: 4px; }"
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
            ${page?.htmlContent ?: "<p>${stringResource(R.string.page_reader_no_content)}</p>"}
        </body>
        </html>
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page?.name ?: stringResource(R.string.page_reader_title), maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.page_reader_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.page_reader_export))
                    }
                    IconButton(onClick = { showReaderSettingsSheet = true }) {
                        Icon(Icons.Default.Tune, contentDescription = stringResource(R.string.page_reader_settings))
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.page_reader_favorite),
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.loadPage(forceRemote = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.page_reader_refresh))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { previousPage?.let { viewModel.loadPage(it.id) } },
                        enabled = previousPage != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                            contentDescription = stringResource(R.string.page_reader_previous)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = previousPage?.name ?: stringResource(R.string.page_reader_previous),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    IconButton(
                        onClick = { showTocSheet = true }
                    ) {
                        BadgedBox(
                            badge = {
                                if (tocItems.isNotEmpty()) {
                                    Badge { Text("${tocItems.size}") }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Toc,
                                contentDescription = stringResource(R.string.page_reader_toc)
                            )
                        }
                    }

                    TextButton(
                        onClick = { nextPage?.let { viewModel.loadPage(it.id) } },
                        enabled = nextPage != null,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = nextPage?.name ?: stringResource(R.string.page_reader_next),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                            contentDescription = stringResource(R.string.page_reader_next)
                        )
                    }
                }
            }
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

            if (readingProgress > 0f) {
                LinearProgressIndicator(
                    progress = { readingProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
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
                        @SuppressLint("SetJavaScriptEnabled")
                        val webView = ObservableWebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    val url = request?.url ?: return null
                                    return viewModel.offlineImageCache.interceptLocalImage(url)
                                }
                            }
                            settings.javaScriptEnabled = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.defaultTextEncodingName = "UTF-8"
                            settings.cacheMode = WebSettings.LOAD_DEFAULT

                            onScrollProgressChanged = { progress ->
                                readingProgress = progress
                            }
                        }
                        webViewInstance = webView
                        webView
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

        if (showTocSheet) {
            ModalBottomSheet(
                onDismissRequest = { showTocSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.page_reader_toc_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (tocItems.isEmpty()) {
                        Text(
                            text = stringResource(R.string.page_reader_toc_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tocItems) { item ->
                                val paddingStart = when (item.level) {
                                    1 -> 0.dp
                                    2 -> 16.dp
                                    else -> 32.dp
                                }
                                val textStyle = when (item.level) {
                                    1 -> MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    2 -> MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                    else -> MaterialTheme.typography.bodySmall
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            webViewInstance?.evaluateJavascript(
                                                "document.getElementById('${item.anchorId}')?.scrollIntoView({behavior: 'smooth'});",
                                                null
                                            )
                                            showTocSheet = false
                                        }
                                        .padding(start = paddingStart, top = 8.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bookmark,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = item.title,
                                        style = textStyle,
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

        if (showReaderSettingsSheet) {
            ReaderSettingsSheet(
                onDismiss = { showReaderSettingsSheet = false },
                currentFontSize = fontSize,
                currentFontFamily = readerFontFamily,
                currentLineHeight = lineHeight,
                onFontSizeChange = { viewModel.setFontSize(it) },
                onFontFamilyChange = { viewModel.setReaderFontFamily(it) },
                onLineHeightChange = { viewModel.setLineHeight(it) }
            )
        }

        if (showExportDialog) {
            ExportFormatDialog(
                pageTitle = page?.name ?: "",
                onDismiss = { showExportDialog = false },
                onExportMarkdown = { pendingExportFormat = ExportFormat.MARKDOWN; showExportDialog = false },
                onExportHtml = { pendingExportFormat = ExportFormat.HTML; showExportDialog = false },
                onExportPdf = {
                    showExportDialog = false
                    webViewInstance?.let { webView ->
                        val context = webView.context
                        val printManager =
                            context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        printManager.print(
                            page?.name ?: "page",
                            webView.createPrintDocumentAdapter(page?.name ?: "page"),
                            PrintAttributes.Builder().build()
                        )
                    }
                }
            )
        }

        pendingExportFormat?.let { format ->
            val context = LocalContext.current
            val fileName = (page?.name ?: "page").replace(Regex("[^\\w\\- ]"), "").trim()
            val mimeType = if (format == ExportFormat.MARKDOWN) "text/markdown" else "text/html"
            val extension = if (format == ExportFormat.MARKDOWN) "md" else "html"

            val createDocumentLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument(mimeType)
            ) { uri ->
                if (uri != null && page != null) {
                    val content = when (format) {
                        ExportFormat.MARKDOWN -> HtmlToMarkdownConverter.convert(page!!.htmlContent)
                        ExportFormat.HTML -> buildStyledExportHtml(page!!, isDarkTheme)
                    }
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
                pendingExportFormat = null
            }

            LaunchedEffect(format) {
                createDocumentLauncher.launch("$fileName.$extension")
            }
        }
    }
}

private enum class ExportFormat { MARKDOWN, HTML }

private fun buildStyledExportHtml(page: Page, isDarkTheme: Boolean): String {
    val bg = if (isDarkTheme) "#121212" else "#FFFFFF"
    val fg = if (isDarkTheme) "#E0E0E0" else "#212121"
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>${page.name}</title>
        </head>
        <body style="background-color: $bg; color: $fg;">
            ${page.htmlContent}
        </body>
        </html>
    """.trimIndent()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    onDismiss: () -> Unit,
    currentFontSize: FontSize,
    currentFontFamily: ReaderFontFamily,
    currentLineHeight: LineHeight,
    onFontSizeChange: (FontSize) -> Unit,
    onFontFamilyChange: (ReaderFontFamily) -> Unit,
    onLineHeightChange: (LineHeight) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.page_reader_settings),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.page_reader_font_size),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FontSize.entries.forEach { size ->
                    FilterChip(
                        selected = size == currentFontSize,
                        onClick = { onFontSizeChange(size) },
                        label = {
                            Text(
                                when (size) {
                                    FontSize.SMALL -> "A⁻"
                                    FontSize.NORMAL -> "A"
                                    FontSize.LARGE -> "A⁺"
                                }
                            )
                        }
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.reader_font_family),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                ReaderFontFamily.entries.forEach { family ->
                    FilterChip(
                        selected = family == currentFontFamily,
                        onClick = { onFontFamilyChange(family) },
                        label = {
                            Text(
                                text = stringResource(
                                    when (family) {
                                        ReaderFontFamily.SANS -> R.string.reader_font_sans
                                        ReaderFontFamily.SERIF -> R.string.reader_font_serif
                                        ReaderFontFamily.MONOSPACE -> R.string.reader_font_mono
                                    }
                                ),
                                fontFamily = when (family) {
                                    ReaderFontFamily.SANS -> androidx.compose.ui.text.font.FontFamily.SansSerif
                                    ReaderFontFamily.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
                                    ReaderFontFamily.MONOSPACE -> androidx.compose.ui.text.font.FontFamily.Monospace
                                }
                            )
                        }
                    )
                }
            }

            HorizontalDivider()

            Text(
                text = stringResource(R.string.reader_line_height),
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                LineHeight.entries.forEach { heightOption ->
                    FilterChip(
                        selected = heightOption == currentLineHeight,
                        onClick = { onLineHeightChange(heightOption) },
                        label = {
                            Text(
                                text = stringResource(
                                    when (heightOption) {
                                        LineHeight.COMPACT -> R.string.reader_line_compact
                                        LineHeight.NORMAL -> R.string.reader_line_normal
                                        LineHeight.RELAXED -> R.string.reader_line_relaxed
                                        LineHeight.LOOSE -> R.string.reader_line_loose
                                    }
                                )
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExportFormatDialog(
    pageTitle: String,
    onDismiss: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportHtml: () -> Unit,
    onExportPdf: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.export_dialog_title, pageTitle)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onExportMarkdown, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.export_format_markdown))
                }
                TextButton(onClick = onExportHtml, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.export_format_html))
                }
                TextButton(onClick = onExportPdf, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.export_format_pdf))
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
