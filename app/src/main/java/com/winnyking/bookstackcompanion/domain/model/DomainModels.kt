package com.winnyking.bookstackcompanion.domain.model

data class ServerConfig(
    val id: String,
    val name: String,
    val baseUrl: String,
    val tokenId: String,
    val tokenSecret: String,
    val isSelected: Boolean = false,
    val lastSyncTimestamp: Long = 0L
) {
    override fun toString(): String {
        return "ServerConfig(id=$id, name=$name, baseUrl=$baseUrl, isSelected=$isSelected, lastSyncTimestamp=$lastSyncTimestamp)"
    }
}

data class Book(
    val id: Long,
    val serverId: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val coverUrl: String? = null,
    val isDownloaded: Boolean = false,
    val lastUpdated: String? = null,
    val lastSyncedAt: Long = 0L
)

data class Shelf(
    val id: Long,
    val serverId: String,
    val name: String,
    val slug: String,
    val description: String = "",
    val coverUrl: String? = null,
    val bookIds: List<Long> = emptyList()
)

data class Chapter(
    val id: Long,
    val serverId: String,
    val bookId: Long,
    val name: String,
    val slug: String,
    val description: String = "",
    val pages: List<Page> = emptyList()
)

data class Page(
    val id: Long,
    val serverId: String,
    val bookId: Long,
    val chapterId: Long = 0,
    val name: String,
    val slug: String,
    val htmlContent: String = "",
    val isFavorite: Boolean = false,
    val isCached: Boolean = false,
    val lastAccessed: Long = 0L
)

data class SearchResult(
    val id: Long,
    val serverId: String,
    val name: String,
    val slug: String,
    val type: String,
    val previewContent: String = "",
    val bookId: Long? = null,
    val chapterId: Long? = null
)

data class FavoriteItem(
    val id: String,
    val serverId: String,
    val pageId: Long,
    val pageName: String,
    val bookName: String,
    val chapterName: String,
    val addedAt: Long
)

data class HistoryItem(
    val id: String,
    val serverId: String,
    val pageId: Long,
    val pageName: String,
    val bookName: String,
    val chapterName: String,
    val accessedAt: Long
)
