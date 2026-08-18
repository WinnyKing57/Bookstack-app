package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.data.api.model.BookContentItemDto
import com.winnyking.bookstackcompanion.data.api.model.BookDto
import com.winnyking.bookstackcompanion.data.api.model.ChapterDto
import com.winnyking.bookstackcompanion.data.api.model.ImageDto
import com.winnyking.bookstackcompanion.data.api.model.PageDto
import com.winnyking.bookstackcompanion.data.api.model.PagedResponse
import com.winnyking.bookstackcompanion.data.api.model.PreviewHtmlDto
import com.winnyking.bookstackcompanion.data.api.model.SearchResultDto
import com.winnyking.bookstackcompanion.data.api.model.ShelfDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ApiParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

    @Test
    fun testBookDtoParsing() {
        val sampleResponseJson = """
            {
                "data": [
                    {
                        "id": 42,
                        "name": "Guide BookStack",
                        "slug": "guide-bookstack",
                        "description": "Documentation officielle",
                        "created_at": "2024-01-01T10:00:00Z",
                        "updated_at": "2024-01-02T12:00:00Z",
                        "cover": {
                            "url": "https://example.com/cover.png"
                        }
                    }
                ],
                "total": 1
            }
        """.trimIndent()

        val parsed = json.decodeFromString<PagedResponse<BookDto>>(sampleResponseJson)

        assertEquals(1, parsed.total)
        assertEquals(1, parsed.data.size)

        val book = parsed.data.first()
        assertEquals(42L, book.id)
        assertEquals("Guide BookStack", book.name)
        assertEquals("https://example.com/cover.png", book.cover?.url)
    }

    @Test
    fun testShelfDtoParsing() {
        val jsonStr = """
            {
                "id": 10,
                "name": "Documentation API",
                "slug": "documentation-api",
                "description": "Docs REST",
                "books": [
                    {
                        "id": 1,
                        "name": "Livre 1",
                        "slug": "livre-1"
                    }
                ]
            }
        """.trimIndent()

        val shelf = json.decodeFromString<ShelfDto>(jsonStr)

        assertEquals(10L, shelf.id)
        assertEquals("Documentation API", shelf.name)
        assertEquals("documentation-api", shelf.slug)
        assertEquals(1, shelf.books?.size)
        assertEquals("Livre 1", shelf.books?.first()?.name)
    }

    @Test
    fun testChapterDtoParsing() {
        val jsonStr = """
            {
                "id": 5,
                "book_id": 42,
                "name": "Introduction",
                "slug": "introduction",
                "description": "Chapitre d'intro",
                "priority": 1,
                "pages": [
                    {
                        "id": 100,
                        "book_id": 42,
                        "chapter_id": 5,
                        "name": "Page 1",
                        "slug": "page-1",
                        "html": "<p>Contenu</p>",
                        "raw_html": "<p>Raw</p>"
                    }
                ]
            }
        """.trimIndent()

        val chapter = json.decodeFromString<ChapterDto>(jsonStr)

        assertEquals(5L, chapter.id)
        assertEquals(42L, chapter.book_id)
        assertEquals("Introduction", chapter.name)
        assertEquals(1, chapter.pages?.size)
        assertEquals("<p>Contenu</p>", chapter.pages?.first()?.html)
    }

    @Test
    fun testPageDtoParsing() {
        val jsonStr = """
            {
                "id": 100,
                "book_id": 42,
                "chapter_id": 5,
                "name": "Page de test",
                "slug": "page-de-test",
                "html": "<h1>Titre</h1><p>Contenu HTML</p>",
                "raw_html": "<h1>Titre</h1>",
                "markdown": "# Titre",
                "priority": 0,
                "draft": false,
                "revision_count": 3
            }
        """.trimIndent()

        val page = json.decodeFromString<PageDto>(jsonStr)

        assertEquals(100L, page.id)
        assertEquals(42L, page.book_id)
        assertEquals(5L, page.chapter_id)
        assertEquals("Page de test", page.name)
        assertEquals("<h1>Titre</h1><p>Contenu HTML</p>", page.html)
        assertEquals("<h1>Titre</h1>", page.raw_html)
        assertEquals("# Titre", page.markdown)
        assertEquals(3, page.revision_count)
    }

    @Test
    fun testSearchResultDtoParsing() {
        val jsonStr = """
            {
                "id": 200,
                "name": "Résultat recherche",
                "slug": "resultat-recherche",
                "type": "page",
                "url": "https://example.com/page/200",
                "preview_html": {
                    "name": "Preview",
                    "content": "<p>Aperçu</p>"
                },
                "book_id": 42,
                "chapter_id": 5
            }
        """.trimIndent()

        val result = json.decodeFromString<SearchResultDto>(jsonStr)

        assertEquals(200L, result.id)
        assertEquals("Résultat recherche", result.name)
        assertEquals("page", result.type)
        assertEquals("https://example.com/page/200", result.url)
        assertEquals("<p>Aperçu</p>", result.preview_html?.content)
        assertEquals(42L, result.book_id)
        assertEquals(5L, result.chapter_id)
    }

    @Test
    fun testBookContentItemParsing() {
        val jsonStr = """
            {
                "id": 10,
                "name": "Chapitre Test",
                "slug": "chapitre-test",
                "type": "chapter",
                "chapter_id": null,
                "draft": false,
                "pages": [
                    {"id": 100, "book_id": 1, "name": "Page A", "slug": "page-a"},
                    {"id": 101, "book_id": 1, "name": "Page B", "slug": "page-b"}
                ]
            }
        """.trimIndent()

        val item = json.decodeFromString<BookContentItemDto>(jsonStr)

        assertEquals(10L, item.id)
        assertEquals("Chapitre Test", item.name)
        assertEquals("chapter", item.type)
        assertEquals(2, item.pages?.size)
    }

    @Test
    fun testPagedResponseEmptyData() {
        val jsonStr = """{"data": [], "total": 0}"""
        val parsed = json.decodeFromString<PagedResponse<BookDto>>(jsonStr)

        assertEquals(0, parsed.total)
        assertEquals(0, parsed.data.size)
    }

    @Test
    fun testShelfDtoWithNullDescription() {
        val jsonStr = """
            {
                "id": 1,
                "name": "Shelf",
                "slug": "shelf"
            }
        """.trimIndent()

        val shelf = json.decodeFromString<ShelfDto>(jsonStr)
        assertEquals("", shelf.description)
        assertNull(shelf.cover)
        assertNull(shelf.books)
    }

    @Test
    fun testBookDtoWithNullCover() {
        val jsonStr = """
            {
                "id": 1,
                "name": "No Cover Book",
                "slug": "no-cover-book"
            }
        """.trimIndent()

        val book = json.decodeFromString<BookDto>(jsonStr)
        assertEquals(1L, book.id)
        assertNull(book.cover)
        assertNull(book.contents)
        assertEquals("", book.description)
    }

    @Test
    fun testPageDtoDefaultValues() {
        val jsonStr = """
            {
                "id": 1,
                "book_id": 1,
                "name": "Minimal Page",
                "slug": "minimal-page"
            }
        """.trimIndent()

        val page = json.decodeFromString<PageDto>(jsonStr)
        assertEquals(0L, page.chapter_id)
        assertEquals("", page.html)
        assertEquals("", page.raw_html)
        assertEquals(0, page.priority)
        assertEquals(false, page.draft)
        assertEquals(0, page.revision_count)
    }
}
