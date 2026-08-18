package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.domain.model.Book
import com.winnyking.bookstackcompanion.domain.model.Chapter
import com.winnyking.bookstackcompanion.domain.model.FavoriteItem
import com.winnyking.bookstackcompanion.domain.model.HistoryItem
import com.winnyking.bookstackcompanion.domain.model.Page
import com.winnyking.bookstackcompanion.domain.model.SearchResult
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.model.Shelf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainModelsTest {

    @Test
    fun testServerConfigDefaults() {
        val server = ServerConfig(
            id = "uuid-123",
            name = "Mon Serveur",
            baseUrl = "https://bookstack.example.com",
            tokenId = "token-id",
            tokenSecret = "token-secret"
        )

        assertFalse(server.isSelected)
        assertEquals(0L, server.lastSyncTimestamp)
    }

    @Test
    fun testBookDefaults() {
        val book = Book(
            id = 1L,
            serverId = "server-1",
            name = "Mon Livre",
            slug = "mon-livre"
        )

        assertEquals("", book.description)
        assertNull(book.coverUrl)
        assertFalse(book.isDownloaded)
        assertNull(book.lastUpdated)
    }

    @Test
    fun testShelfDefaults() {
        val shelf = Shelf(
            id = 1L,
            serverId = "server-1",
            name = "Ma Étagère",
            slug = "ma-etagere"
        )

        assertEquals("", shelf.description)
        assertNull(shelf.coverUrl)
        assertTrue(shelf.bookIds.isEmpty())
    }

    @Test
    fun testChapterDefaults() {
        val chapter = Chapter(
            id = 1L,
            serverId = "server-1",
            bookId = 10L,
            name = "Chapitre 1",
            slug = "chapitre-1"
        )

        assertEquals("", chapter.description)
        assertTrue(chapter.pages.isEmpty())
    }

    @Test
    fun testChapterWithPages() {
        val page1 = Page(
            id = 100L,
            serverId = "server-1",
            bookId = 10L,
            chapterId = 1L,
            name = "Page 1",
            slug = "page-1"
        )
        val page2 = Page(
            id = 101L,
            serverId = "server-1",
            bookId = 10L,
            chapterId = 1L,
            name = "Page 2",
            slug = "page-2"
        )

        val chapter = Chapter(
            id = 1L,
            serverId = "server-1",
            bookId = 10L,
            name = "Chapitre 1",
            slug = "chapitre-1",
            pages = listOf(page1, page2)
        )

        assertEquals(2, chapter.pages.size)
        assertEquals("Page 1", chapter.pages[0].name)
        assertEquals("Page 2", chapter.pages[1].name)
    }

    @Test
    fun testPageDefaults() {
        val page = Page(
            id = 1L,
            serverId = "server-1",
            bookId = 10L,
            name = "Page Test",
            slug = "page-test"
        )

        assertEquals(0L, page.chapterId)
        assertEquals("", page.htmlContent)
        assertFalse(page.isFavorite)
        assertFalse(page.isCached)
        assertEquals(0L, page.lastAccessed)
    }

    @Test
    fun testPageAsDirectPage() {
        val page = Page(
            id = 1L,
            serverId = "server-1",
            bookId = 10L,
            chapterId = 0,
            name = "Page Directe",
            slug = "page-directe"
        )

        assertEquals(0L, page.chapterId)
    }

    @Test
    fun testSearchResultDefaults() {
        val result = SearchResult(
            id = 1L,
            serverId = "server-1",
            name = "Result",
            slug = "result",
            type = "book"
        )

        assertEquals("", result.previewContent)
        assertNull(result.bookId)
        assertNull(result.chapterId)
    }

    @Test
    fun testSearchResultTypes() {
        val bookResult = SearchResult(id = 1, serverId = "s1", name = "Book", slug = "book", type = "book")
        val chapterResult = SearchResult(id = 2, serverId = "s1", name = "Chapter", slug = "chapter", type = "chapter")
        val pageResult = SearchResult(id = 3, serverId = "s1", name = "Page", slug = "page", type = "page")

        assertEquals("book", bookResult.type)
        assertEquals("chapter", chapterResult.type)
        assertEquals("page", pageResult.type)
    }

    @Test
    fun testFavoriteItemCreation() {
        val fav = FavoriteItem(
            id = "server-1_100",
            serverId = "server-1",
            pageId = 100L,
            pageName = "Ma Page",
            bookName = "Mon Livre",
            chapterName = "Mon Chapitre",
            addedAt = System.currentTimeMillis()
        )

        assertEquals("server-1_100", fav.id)
        assertEquals(100L, fav.pageId)
        assertEquals("Ma Page", fav.pageName)
        assertEquals("Mon Livre", fav.bookName)
    }

    @Test
    fun testHistoryItemCreation() {
        val history = HistoryItem(
            id = "server-1_100",
            serverId = "server-1",
            pageId = 100L,
            pageName = "Ma Page",
            bookName = "Mon Livre",
            chapterName = "Mon Chapitre",
            accessedAt = System.currentTimeMillis()
        )

        assertEquals("server-1_100", history.id)
        assertEquals(100L, history.pageId)
        assertTrue(history.accessedAt > 0)
    }

    @Test
    fun testBookEquality() {
        val book1 = Book(id = 1, serverId = "s1", name = "Book", slug = "book")
        val book2 = Book(id = 1, serverId = "s1", name = "Book", slug = "book")

        assertEquals(book1, book2)
    }

    @Test
    fun testBookCopy() {
        val book = Book(id = 1, serverId = "s1", name = "Original", slug = "original")
        val copy = book.copy(name = "Modified", isDownloaded = true)

        assertEquals("Modified", copy.name)
        assertTrue(copy.isDownloaded)
        assertEquals(book.id, copy.id)
    }
}
