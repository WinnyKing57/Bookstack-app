package com.winnyking.bookstackcompanion

import com.winnyking.bookstackcompanion.data.api.model.BookDto
import com.winnyking.bookstackcompanion.data.api.model.PagedResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
}
