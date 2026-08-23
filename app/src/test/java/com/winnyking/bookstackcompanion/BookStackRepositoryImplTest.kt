package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.data.api.BookStackApi
import com.winnyking.bookstackcompanion.data.api.DynamicApiClientFactory
import com.winnyking.bookstackcompanion.data.api.model.BookContentItemDto
import com.winnyking.bookstackcompanion.data.api.model.BookDto
import com.winnyking.bookstackcompanion.data.api.model.PagedResponse
import com.winnyking.bookstackcompanion.data.api.model.PageDto
import com.winnyking.bookstackcompanion.data.database.dao.BookDao
import com.winnyking.bookstackcompanion.data.database.dao.ChapterDao
import com.winnyking.bookstackcompanion.data.database.dao.FavoriteDao
import com.winnyking.bookstackcompanion.data.database.dao.PageDao
import com.winnyking.bookstackcompanion.data.database.dao.ServerDao
import com.winnyking.bookstackcompanion.data.database.dao.ShelfDao
import com.winnyking.bookstackcompanion.data.database.entity.BookEntity
import com.winnyking.bookstackcompanion.data.database.entity.ServerEntity
import com.winnyking.bookstackcompanion.data.offline.OfflineImageCache
import com.winnyking.bookstackcompanion.data.repository.BookStackRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class BookStackRepositoryImplTest {

    private lateinit var serverDao: ServerDao
    private lateinit var bookDao: BookDao
    private lateinit var shelfDao: ShelfDao
    private lateinit var chapterDao: ChapterDao
    private lateinit var pageDao: PageDao
    private lateinit var favoriteDao: FavoriteDao
    private lateinit var apiClientFactory: DynamicApiClientFactory
    private lateinit var offlineImageCache: OfflineImageCache
    private lateinit var api: BookStackApi
    private lateinit var repository: BookStackRepositoryImpl

    private val server = ServerEntity(
        id = "server-1",
        name = "Mon serveur",
        baseUrl = "https://bookstack.example.com",
        isSelected = true,
        lastSyncTimestamp = 0L
    )

    @Before
    fun setup() {
        serverDao = mockk()
        bookDao = mockk(relaxUnitFun = true)
        shelfDao = mockk(relaxUnitFun = true)
        chapterDao = mockk(relaxUnitFun = true)
        pageDao = mockk(relaxUnitFun = true)
        favoriteDao = mockk()
        apiClientFactory = mockk()
        offlineImageCache = mockk(relaxUnitFun = true)
        api = mockk()

        coEvery { serverDao.getServerById("server-1") } returns server
        every { apiClientFactory.createApi(server.baseUrl, server.id) } returns api

        repository = BookStackRepositoryImpl(
            serverDao = serverDao,
            bookDao = bookDao,
            shelfDao = shelfDao,
            chapterDao = chapterDao,
            pageDao = pageDao,
            favoriteDao = favoriteDao,
            apiClientFactory = apiClientFactory,
            offlineImageCache = offlineImageCache
        )
    }

    @Test
    fun `refreshBooks preserves downloaded state and sets lastSyncedAt`() = runTest {
        val existing = BookEntity(
            id = 1L,
            serverId = "server-1",
            name = "Ancien nom",
            slug = "ancien",
            description = "",
            coverUrl = null,
            isDownloaded = true,
            lastUpdated = null,
            lastSyncedAt = 12345L
        )
        every { bookDao.getBooksByServer("server-1") } returns flowOf(listOf(existing))
        coEvery { api.getBooks(any(), any()) } returns PagedResponse(
            data = listOf(bookDto(1L, "Livre A"), bookDto(2L, "Livre B"))
        )
        val insertedBatches = mutableListOf<List<BookEntity>>()
        coEvery { bookDao.insertBooks(any<List<BookEntity>>()) } coAnswers { insertedBatches.add(firstArg()); Unit }

        val result = repository.refreshBooks("server-1")

        assertTrue(result.isSuccess)
        coVerify(exactly = 1) { bookDao.insertBooks(any<List<BookEntity>>()) }
        val bookA = insertedBatches.single().first { it.id == 1L }
        val bookB = insertedBatches.single().first { it.id == 2L }
        assertTrue(bookA.isDownloaded)
        assertFalse(bookB.isDownloaded)
        assertTrue(insertedBatches.single().all { it.lastSyncedAt > 0 })
    }

    @Test
    fun `refreshBooks returns failure on network error`() = runTest {
        every { bookDao.getBooksByServer("server-1") } returns flowOf(emptyList())
        coEvery { api.getBooks(any(), any()) } throws IOException("network down")

        val result = repository.refreshBooks("server-1")

        assertTrue(result.isFailure)
        assertEquals("network down", result.exceptionOrNull()?.message)
    }

    @Test
    fun `downloadBookForOffline caches all pages and marks book as downloaded`() = runTest {
        val contents = listOf(
            BookContentItemDto(id = 10L, name = "Chapitre", slug = "chapitre", type = "chapter", pages = listOf(pageDto(101L))),
            BookContentItemDto(id = 20L, name = "Page directe", slug = "page-directe", type = "page")
        )
        coEvery { api.getBookDetail(1L) } returns bookDto(1L, "Livre A").copy(contents = contents)
        coEvery { api.getPageDetail(101L) } returns pageDto(101L)
        coEvery { api.getPageDetail(20L) } returns pageDto(20L)

        val progressUpdates = mutableListOf<Pair<Int, Int>>()
        val result = repository.downloadBookForOffline("server-1", 1L) { c, t -> progressUpdates.add(c to t) }

        assertTrue(result.isSuccess)
        coVerify(exactly = 2) {
            pageDao.insertPages(withArg<List<com.winnyking.bookstackcompanion.data.database.entity.PageEntity>> { pages ->
                assertTrue(pages.all { it.isCached })
                assertEquals(1L, pages.first().bookId)
            })
        }
        coVerify(exactly = 2) {
            offlineImageCache.cacheImagesForPage(eq("server-1"), eq(1L), eq(server.baseUrl), any())
        }
        coVerify { bookDao.updateBookDownloadState("server-1", 1L, true) }
        coVerify { bookDao.updateLastSyncedAt(eq("server-1"), eq(1L), any()) }
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progressUpdates)
    }

    @Test
    fun `downloadBookForOffline with empty book only flags download`() = runTest {
        coEvery { api.getBookDetail(1L) } returns bookDto(1L, "Livre vide").copy(contents = null)

        val result = repository.downloadBookForOffline("server-1", 1L)

        assertTrue(result.isSuccess)
        coVerify(exactly = 0) { pageDao.insertPages(any()) }
        coVerify(exactly = 0) { offlineImageCache.cacheImagesForPage(any(), any(), any(), any()) }
        coVerify { bookDao.updateBookDownloadState("server-1", 1L, true) }
        coVerify { bookDao.updateLastSyncedAt(eq("server-1"), eq(1L), any()) }
    }

    @Test
    fun `downloadBookForOffline returns failure on error`() = runTest {
        coEvery { api.getBookDetail(1L) } throws IOException("boom")

        val result = repository.downloadBookForOffline("server-1", 1L)

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { bookDao.updateBookDownloadState(any(), any(), true) }
    }

    @Test
    fun `deleteBookOfflineCache removes pages images and resets state`() = runTest {
        repository.deleteBookOfflineCache("server-1", 1L)

        coVerify { pageDao.deleteBookPages("server-1", 1L) }
        coVerify { offlineImageCache.deleteImagesForBook("server-1", 1L) }
        coVerify { bookDao.updateBookDownloadState("server-1", 1L, false) }
        coVerify { bookDao.updateLastSyncedAt("server-1", 1L, 0L) }
    }

    @Test
    fun `clearCache clears pages and all images of the server`() = runTest {
        repository.clearCache("server-1")

        coVerify { pageDao.clearCachedPages("server-1") }
        coVerify { offlineImageCache.deleteAllImages("server-1") }
    }

    private fun bookDto(id: Long, name: String) = BookDto(
        id = id,
        name = name,
        slug = "slug-$id",
        description = "Description $name"
    )

    private fun pageDto(id: Long) = PageDto(
        id = id,
        book_id = 1L,
        chapter_id = 0L,
        name = "Page $id",
        slug = "page-$id",
        html = "<p>Contenu $id</p>"
    )
}
