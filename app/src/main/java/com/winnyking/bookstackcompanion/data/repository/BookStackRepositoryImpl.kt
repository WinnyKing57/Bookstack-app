package com.winnyking.bookstackcompanion.data.repository

import com.winnyking.bookstackcompanion.data.api.BookStackApi
import com.winnyking.bookstackcompanion.data.api.DynamicApiClientFactory
import com.winnyking.bookstackcompanion.data.database.dao.BookDao
import com.winnyking.bookstackcompanion.data.database.dao.ChapterDao
import com.winnyking.bookstackcompanion.data.database.dao.FavoriteDao
import com.winnyking.bookstackcompanion.data.database.dao.PageDao
import com.winnyking.bookstackcompanion.data.database.dao.ServerDao
import com.winnyking.bookstackcompanion.data.database.dao.ShelfDao
import com.winnyking.bookstackcompanion.data.database.entity.BookEntity
import com.winnyking.bookstackcompanion.data.database.entity.ChapterEntity
import com.winnyking.bookstackcompanion.data.database.entity.PageEntity
import com.winnyking.bookstackcompanion.data.database.entity.ShelfEntity
import com.winnyking.bookstackcompanion.data.offline.OfflineImageCache
import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.Chapter
import com.winnyking.bookstackcompanion.domain.model.Page
import com.winnyking.bookstackcompanion.domain.model.SearchResult
import com.winnyking.bookstackcompanion.domain.model.Shelf
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookStackRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao,
    private val chapterDao: ChapterDao,
    private val pageDao: PageDao,
    private val favoriteDao: FavoriteDao,
    private val apiClientFactory: DynamicApiClientFactory,
    private val offlineImageCache: OfflineImageCache
) : BookStackRepository {

    private suspend fun getServerOrThrow(serverId: String) =
        serverDao.getServerById(serverId)
            ?: throw IllegalStateException("Serveur non trouvé pour l'id: $serverId")

    private suspend fun getApiForServer(serverId: String): BookStackApi {
        val server = getServerOrThrow(serverId)
        return apiClientFactory.createApi(server.baseUrl, server.id)
    }

    override fun getBooks(serverId: String): Flow<List<Book>> {
        return bookDao.getBooksByServer(serverId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshBooks(serverId: String): Result<Unit> {
        return try {
            val api = getApiForServer(serverId)
            val pagedResponse = api.getBooks(count = 500)
            val downloadedIds = bookDao.getBooksByServer(serverId).first()
                .filter { it.isDownloaded }
                .map { it.id }
                .toSet()
            val entities = pagedResponse.data.map { dto ->
                BookEntity(
                    id = dto.id,
                    serverId = serverId,
                    name = dto.name,
                    slug = dto.slug,
                    description = dto.description ?: "",
                    coverUrl = dto.cover?.url,
                    isDownloaded = downloadedIds.contains(dto.id),
                    lastUpdated = dto.updated_at,
                    lastSyncedAt = System.currentTimeMillis()
                )
            }
            bookDao.deleteBooksForServer(serverId)
            bookDao.insertBooks(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBookDetail(serverId: String, bookId: Long): Result<Book> {
        return try {
            val local = bookDao.getBookById(serverId, bookId)
            if (local != null) {
                Result.success(local.toDomain())
            } else {
                val api = getApiForServer(serverId)
                val dto = api.getBookDetail(bookId)
                val entity = BookEntity(
                    id = dto.id,
                    serverId = serverId,
                    name = dto.name,
                    slug = dto.slug,
                    description = dto.description ?: "",
                    coverUrl = dto.cover?.url,
                    isDownloaded = false,
                    lastUpdated = dto.updated_at,
                    lastSyncedAt = System.currentTimeMillis()
                )
                bookDao.insertBooks(listOf(entity))
                Result.success(entity.toDomain())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getBookTree(
        serverId: String,
        bookId: Long
    ): Result<Pair<List<Chapter>, List<Page>>> {
        return try {
            val api = getApiForServer(serverId)
            val bookDto = api.getBookDetail(bookId)

            val favoritePageIds = favoriteDao.getFavoritesForServer(serverId).first().map { it.pageId }.toSet()

            val chaptersList = mutableListOf<Chapter>()
            val directPagesList = mutableListOf<Page>()

            bookDto.contents?.forEach { item ->
                if (item.type == "chapter") {
                    val chapterPages = item.pages?.map { pageDto ->
                        Page(
                            id = pageDto.id,
                            serverId = serverId,
                            bookId = bookId,
                            chapterId = item.id,
                            name = pageDto.name,
                            slug = pageDto.slug,
                            htmlContent = pageDto.html ?: "",
                            isFavorite = favoritePageIds.contains(pageDto.id),
                            isCached = false
                        )
                    } ?: emptyList()

                    chaptersList.add(
                        Chapter(
                            id = item.id,
                            serverId = serverId,
                            bookId = bookId,
                            name = item.name,
                            slug = item.slug,
                            description = "",
                            pages = chapterPages
                        )
                    )
                } else if (item.type == "page") {
                    directPagesList.add(
                        Page(
                            id = item.id,
                            serverId = serverId,
                            bookId = bookId,
                            chapterId = 0,
                            name = item.name,
                            slug = item.slug,
                            htmlContent = "",
                            isFavorite = favoritePageIds.contains(item.id),
                            isCached = false
                        )
                    )
                }
            }

            // Save tree structures to DB
            chapterDao.insertChapters(chaptersList.map { chapter ->
                ChapterEntity(
                    id = chapter.id,
                    serverId = serverId,
                    bookId = bookId,
                    name = chapter.name,
                    slug = chapter.slug,
                    description = chapter.description
                )
            })

            val allPages = directPagesList + chaptersList.flatMap { it.pages }
            if (allPages.isNotEmpty()) {
                pageDao.insertPages(allPages.map { page ->
                    PageEntity(
                        id = page.id,
                        serverId = serverId,
                        bookId = bookId,
                        chapterId = page.chapterId,
                        name = page.name,
                        slug = page.slug,
                        htmlContent = page.htmlContent,
                        isCached = false,
                        lastAccessed = System.currentTimeMillis()
                    )
                })
            }

            Result.success(Pair(chaptersList, directPagesList))
        } catch (e: Exception) {
            // Fallback to local DB
            val localChapters = chapterDao.getChaptersForBook(serverId, bookId).firstOrNull() ?: emptyList()
            val localPages = pageDao.getPagesForBook(serverId, bookId).firstOrNull() ?: emptyList()
            val favoritePageIds = favoriteDao.getFavoritesForServer(serverId).first().map { it.pageId }.toSet()

            val directPages = localPages.filter { it.chapterId == 0L }.map {
                Page(
                    id = it.id,
                    serverId = it.serverId,
                    bookId = it.bookId,
                    chapterId = it.chapterId,
                    name = it.name,
                    slug = it.slug,
                    htmlContent = it.htmlContent,
                    isFavorite = favoritePageIds.contains(it.id),
                    isCached = it.isCached,
                    lastAccessed = it.lastAccessed
                )
            }
            val chaptersWithPages = localChapters.map { chapterEntity ->
                val pagesForChapter = localPages.filter { it.chapterId == chapterEntity.id }.map {
                    Page(
                        id = it.id,
                        serverId = it.serverId,
                        bookId = it.bookId,
                        chapterId = it.chapterId,
                        name = it.name,
                        slug = it.slug,
                        htmlContent = it.htmlContent,
                        isFavorite = favoritePageIds.contains(it.id),
                        isCached = it.isCached,
                        lastAccessed = it.lastAccessed
                    )
                }
                Chapter(
                    id = chapterEntity.id,
                    serverId = serverId,
                    bookId = bookId,
                    name = chapterEntity.name,
                    slug = chapterEntity.slug,
                    description = chapterEntity.description,
                    pages = pagesForChapter
                )
            }

            if (localChapters.isNotEmpty() || localPages.isNotEmpty()) {
                Result.success(Pair(chaptersWithPages, directPages))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getPageDetail(serverId: String, pageId: Long, forceRemote: Boolean): Result<Page> {
        return try {
            val local = pageDao.getPageById(serverId, pageId)
            if (local != null && local.htmlContent.isNotBlank() && !forceRemote) {
                Result.success(local.toDomain())
            } else {
                val api = getApiForServer(serverId)
                val dto = api.getPageDetail(pageId)
                val updatedEntity = PageEntity(
                    id = dto.id,
                    serverId = serverId,
                    bookId = dto.book_id,
                    chapterId = dto.chapter_id ?: 0L,
                    name = dto.name,
                    slug = dto.slug,
                    htmlContent = dto.html ?: dto.raw_html ?: "",
                    isCached = true,
                    lastAccessed = System.currentTimeMillis()
                )
                pageDao.insertPages(listOf(updatedEntity))
                Result.success(updatedEntity.toDomain())
            }
        } catch (e: Exception) {
            val local = pageDao.getPageById(serverId, pageId)
            if (local != null) {
                Result.success(local.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }
    override suspend fun downloadBookForOffline(
        serverId: String,
        bookId: Long,
        onProgress: (suspend (completed: Int, total: Int) -> Unit)?
    ): Result<Unit> {
        return try {
            val server = getServerOrThrow(serverId)
            val api = apiClientFactory.createApi(server.baseUrl, server.id)
            val bookDto = api.getBookDetail(bookId)

            val pageIdsToFetch = mutableListOf<Long>()
            bookDto.contents?.forEach { content ->
                if (content.type == "chapter") {
                    content.pages?.forEach { p -> pageIdsToFetch.add(p.id) }
                } else if (content.type == "page") {
                    pageIdsToFetch.add(content.id)
                }
            }

            val total = pageIdsToFetch.size
            if (total == 0) {
                bookDao.updateBookDownloadState(serverId, bookId, true)
                bookDao.updateLastSyncedAt(serverId, bookId, System.currentTimeMillis())
                return Result.success(Unit)
            }

            for ((index, pageId) in pageIdsToFetch.withIndex()) {
                onProgress?.invoke(index, total)
                val pageDto = api.getPageDetail(pageId)
                val html = pageDto.html ?: pageDto.raw_html ?: ""
                offlineImageCache.cacheImagesForPage(serverId, bookId, server.baseUrl, html)
                val pageEntity = PageEntity(
                    id = pageDto.id,
                    serverId = serverId,
                    bookId = bookId,
                    chapterId = pageDto.chapter_id ?: 0L,
                    name = pageDto.name,
                    slug = pageDto.slug,
                    htmlContent = html,
                    isCached = true,
                    lastAccessed = System.currentTimeMillis()
                )
                pageDao.insertPages(listOf(pageEntity))
            }
            onProgress?.invoke(total, total)

            bookDao.updateBookDownloadState(serverId, bookId, true)
            bookDao.updateLastSyncedAt(serverId, bookId, System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getShelves(serverId: String): Flow<List<Shelf>> {
        return shelfDao.getShelvesByServer(serverId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun refreshShelves(serverId: String): Result<Unit> {
        return try {
            val api = getApiForServer(serverId)
            val response = api.getShelves(count = 200)
            val entities = response.data.map { dto ->
                ShelfEntity(
                    id = dto.id,
                    serverId = serverId,
                    name = dto.name,
                    slug = dto.slug,
                    description = dto.description ?: "",
                    coverUrl = dto.cover?.url
                )
            }
            shelfDao.deleteShelvesForServer(serverId)
            shelfDao.insertShelves(entities)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun search(serverId: String, query: String): Result<List<SearchResult>> {
        return try {
            val api = getApiForServer(serverId)
            val response = api.search(query = query)
            val results = response.data.map { dto ->
                SearchResult(
                    id = dto.id,
                    serverId = serverId,
                    name = dto.name,
                    slug = dto.slug,
                    type = dto.type,
                    previewContent = dto.preview_html?.content ?: "",
                    bookId = dto.book_id,
                    chapterId = dto.chapter_id
                )
            }
            Result.success(results)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCachedPagesCount(serverId: String): Flow<Int> {
        return pageDao.getCachedPagesCount(serverId)
    }

    override fun getCachedPagesTotalBytes(serverId: String): Flow<Long> {
        return pageDao.getCachedPagesTotalBytes(serverId).map { it ?: 0L }
    }

    override suspend fun deleteBookOfflineCache(serverId: String, bookId: Long) {
        pageDao.deleteBookPages(serverId, bookId)
        offlineImageCache.deleteImagesForBook(serverId, bookId)
        bookDao.updateBookDownloadState(serverId, bookId, false)
        bookDao.updateLastSyncedAt(serverId, bookId, 0L)
    }

    override suspend fun clearCache(serverId: String) {
        pageDao.clearCachedPages(serverId)
        offlineImageCache.deleteAllImages(serverId)
    }

    private fun BookEntity.toDomain() = Book(
        id = id,
        serverId = serverId,
        name = name,
        slug = slug,
        description = description,
        coverUrl = coverUrl,
        isDownloaded = isDownloaded,
        lastUpdated = lastUpdated,
        lastSyncedAt = lastSyncedAt
    )

    private fun ShelfEntity.toDomain() = Shelf(
        id = id,
        serverId = serverId,
        name = name,
        slug = slug,
        description = description,
        coverUrl = coverUrl
    )

    private fun PageEntity.toDomain() = Page(
        id = id,
        serverId = serverId,
        bookId = bookId,
        chapterId = chapterId,
        name = name,
        slug = slug,
        htmlContent = htmlContent,
        isFavorite = false,
        isCached = isCached,
        lastAccessed = lastAccessed
    )
}
