package com.winnyking.bookstackcompanion.data.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PagedResponse<T>(
    val data: List<T> = emptyList(),
    val total: Int = 0
)

@Serializable
data class ImageDto(
    val url: String? = null,
    val name: String? = null
)

@Serializable
data class BookDto(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String? = "",
    val created_at: String? = null,
    val updated_at: String? = null,
    val cover: ImageDto? = null,
    val contents: List<BookContentItemDto>? = null
)

@Serializable
data class BookContentItemDto(
    val id: Long,
    val name: String,
    val slug: String,
    val type: String, // "chapter" or "page"
    val pages: List<PageDto>? = null,
    val chapter_id: Long? = null,
    val draft: Boolean? = false
)

@Serializable
data class ShelfDto(
    val id: Long,
    val name: String,
    val slug: String,
    val description: String? = "",
    val created_at: String? = null,
    val updated_at: String? = null,
    val cover: ImageDto? = null,
    val books: List<BookDto>? = null
)

@Serializable
data class ChapterDto(
    val id: Long,
    val book_id: Long,
    val name: String,
    val slug: String,
    val description: String? = "",
    val priority: Int? = 0,
    val pages: List<PageDto>? = null
)

@Serializable
data class PageDto(
    val id: Long,
    val book_id: Long,
    val chapter_id: Long? = 0,
    val name: String,
    val slug: String,
    val html: String? = "",
    val raw_html: String? = "",
    val markdown: String? = "",
    val priority: Int? = 0,
    val draft: Boolean? = false,
    val revision_count: Int? = 0,
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class SearchResultDto(
    val id: Long,
    val name: String,
    val slug: String,
    val type: String, // "book", "chapter", "page"
    val url: String? = null,
    val preview_html: PreviewHtmlDto? = null,
    val book_id: Long? = null,
    val chapter_id: Long? = null
)

@Serializable
data class PreviewHtmlDto(
    val name: String? = null,
    val content: String? = null
)

@Serializable
data class ServerSystemInfoResponse(
    val version: String? = null
)
