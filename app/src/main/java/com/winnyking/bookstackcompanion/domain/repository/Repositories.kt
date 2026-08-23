package com.winnyking.bookstackcompanion.domain.repository

import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.Chapter
import com.winnyking.bookstackcompanion.domain.model.FavoriteItem
import com.winnyking.bookstackcompanion.domain.model.HistoryItem
import com.winnyking.bookstackcompanion.domain.model.Page
import com.winnyking.bookstackcompanion.domain.model.SearchResult
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.model.Shelf
import kotlinx.coroutines.flow.Flow

interface ServerRepository {
    fun getAllServers(): Flow<List<ServerConfig>>
    fun getSelectedServer(): Flow<ServerConfig?>
    suspend fun saveServer(name: String, baseUrl: String, tokenId: String, tokenSecret: String): Result<ServerConfig>
    suspend fun selectServer(serverId: String)
    suspend fun deleteServer(serverId: String)
    suspend fun testServerConnection(baseUrl: String, tokenId: String, tokenSecret: String): Boolean
    suspend fun testServerConnectionResult(baseUrl: String, tokenId: String, tokenSecret: String): Result<Unit>
    suspend fun updateLastSyncTimestamp(serverId: String, timestamp: Long)
}

interface BookStackRepository {
    fun getBooks(serverId: String): Flow<List<Book>>
    suspend fun refreshBooks(serverId: String): Result<Unit>
    suspend fun getBookDetail(serverId: String, bookId: Long): Result<Book>
    suspend fun getBookTree(serverId: String, bookId: Long): Result<Pair<List<Chapter>, List<Page>>>
    suspend fun getPageDetail(serverId: String, pageId: Long, forceRemote: Boolean = false): Result<Page>
    suspend fun downloadBookForOffline(serverId: String, bookId: Long, onProgress: (suspend (completed: Int, total: Int) -> Unit)? = null): Result<Unit>
    fun getShelves(serverId: String): Flow<List<Shelf>>
    suspend fun refreshShelves(serverId: String): Result<Unit>
    suspend fun search(serverId: String, query: String): Result<List<SearchResult>>
    fun getCachedPagesCount(serverId: String): Flow<Int>
    fun getCachedPagesTotalBytes(serverId: String): Flow<Long>
    suspend fun deleteBookOfflineCache(serverId: String, bookId: Long)
    suspend fun clearCache(serverId: String)
}

interface FavoriteRepository {
    fun getFavorites(serverId: String): Flow<List<FavoriteItem>>
    fun isFavorite(serverId: String, pageId: Long): Flow<Boolean>
    suspend fun toggleFavorite(serverId: String, pageId: Long, pageName: String, bookName: String, chapterName: String)
}

interface HistoryRepository {
    fun getHistory(serverId: String, limit: Int = 100): Flow<List<HistoryItem>>
    suspend fun addHistory(serverId: String, pageId: Long, pageName: String, bookName: String, chapterName: String, historyLimit: Int = 100)
    suspend fun clearHistory(serverId: String)
}
