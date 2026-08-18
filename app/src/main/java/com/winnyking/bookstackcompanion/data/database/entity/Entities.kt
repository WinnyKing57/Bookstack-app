package com.winnyking.bookstackcompanion.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val isSelected: Boolean,
    val lastSyncTimestamp: Long
)

@Entity(tableName = "books", primaryKeys = ["serverId", "id"])
data class BookEntity(
    val id: Long,
    val serverId: String,
    val name: String,
    val slug: String,
    val description: String,
    val coverUrl: String?,
    val isDownloaded: Boolean,
    val lastUpdated: String?
)

@Entity(tableName = "shelves", primaryKeys = ["serverId", "id"])
data class ShelfEntity(
    val id: Long,
    val serverId: String,
    val name: String,
    val slug: String,
    val description: String,
    val coverUrl: String?
)

@Entity(tableName = "chapters", primaryKeys = ["serverId", "id"])
data class ChapterEntity(
    val id: Long,
    val serverId: String,
    val bookId: Long,
    val name: String,
    val slug: String,
    val description: String
)

@Entity(tableName = "pages", primaryKeys = ["serverId", "id"])
data class PageEntity(
    val id: Long,
    val serverId: String,
    val bookId: Long,
    val chapterId: Long,
    val name: String,
    val slug: String,
    val htmlContent: String,
    val isCached: Boolean,
    val lastAccessed: Long
)

@Entity(tableName = "favorites", primaryKeys = ["serverId", "pageId"])
data class FavoriteEntity(
    val serverId: String,
    val pageId: Long,
    val pageName: String,
    val bookName: String,
    val chapterName: String,
    val addedAt: Long
)

@Entity(tableName = "history", primaryKeys = ["serverId", "pageId"])
data class HistoryEntity(
    val serverId: String,
    val pageId: Long,
    val pageName: String,
    val bookName: String,
    val chapterName: String,
    val accessedAt: Long
)
