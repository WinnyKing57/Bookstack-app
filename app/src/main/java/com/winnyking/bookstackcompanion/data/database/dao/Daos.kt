package com.winnyking.bookstackcompanion.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.winnyking.bookstackcompanion.data.database.entity.BookEntity
import com.winnyking.bookstackcompanion.data.database.entity.ChapterEntity
import com.winnyking.bookstackcompanion.data.database.entity.FavoriteEntity
import com.winnyking.bookstackcompanion.data.database.entity.HistoryEntity
import com.winnyking.bookstackcompanion.data.database.entity.PageEntity
import com.winnyking.bookstackcompanion.data.database.entity.ServerEntity
import com.winnyking.bookstackcompanion.data.database.entity.ShelfEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerDao {
    @Query("SELECT * FROM servers")
    fun getAllServers(): Flow<List<ServerEntity>>

    @Query("SELECT * FROM servers WHERE isSelected = 1 LIMIT 1")
    fun getSelectedServer(): Flow<ServerEntity?>

    @Query("SELECT * FROM servers WHERE id = :id")
    suspend fun getServerById(id: String): ServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServer(server: ServerEntity)

    @Query("UPDATE servers SET isSelected = (id = :selectedId)")
    suspend fun setSelectedServer(selectedId: String)

    @Query("DELETE FROM servers WHERE id = :id")
    suspend fun deleteServer(id: String)
}

@Dao
interface BookDao {
    @Query("SELECT * FROM books WHERE serverId = :serverId ORDER BY name ASC")
    fun getBooksByServer(serverId: String): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE serverId = :serverId AND id = :id")
    suspend fun getBookById(serverId: String, id: Long): BookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Query("UPDATE books SET isDownloaded = :isDownloaded WHERE serverId = :serverId AND id = :id")
    suspend fun updateBookDownloadState(serverId: String, id: Long, isDownloaded: Boolean)

    @Query("UPDATE books SET lastSyncedAt = :timestamp WHERE serverId = :serverId AND id = :id")
    suspend fun updateLastSyncedAt(serverId: String, id: Long, timestamp: Long)

    @Query("DELETE FROM books WHERE serverId = :serverId")
    suspend fun deleteBooksForServer(serverId: String)
}

@Dao
interface ShelfDao {
    @Query("SELECT * FROM shelves WHERE serverId = :serverId ORDER BY name ASC")
    fun getShelvesByServer(serverId: String): Flow<List<ShelfEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShelves(shelves: List<ShelfEntity>)

    @Query("DELETE FROM shelves WHERE serverId = :serverId")
    suspend fun deleteShelvesForServer(serverId: String)
}

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE serverId = :serverId AND bookId = :bookId")
    fun getChaptersForBook(serverId: String, bookId: Long): Flow<List<ChapterEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)
}

@Dao
interface PageDao {
    @Query("SELECT * FROM pages WHERE serverId = :serverId AND id = :id")
    suspend fun getPageById(serverId: String, id: Long): PageEntity?

    @Query("SELECT * FROM pages WHERE serverId = :serverId AND bookId = :bookId")
    fun getPagesForBook(serverId: String, bookId: Long): Flow<List<PageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Query("SELECT COUNT(*) FROM pages WHERE serverId = :serverId AND isCached = 1")
    fun getCachedPagesCount(serverId: String): Flow<Int>

    @Query("SELECT SUM(LENGTH(htmlContent)) FROM pages WHERE serverId = :serverId AND isCached = 1")
    fun getCachedPagesTotalBytes(serverId: String): Flow<Long?>

    @Query("DELETE FROM pages WHERE serverId = :serverId AND isCached = 1")
    suspend fun clearCachedPages(serverId: String)

    @Query("DELETE FROM pages WHERE serverId = :serverId AND bookId = :bookId")
    suspend fun deleteBookPages(serverId: String, bookId: Long)
}

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites WHERE serverId = :serverId ORDER BY addedAt DESC")
    fun getFavoritesForServer(serverId: String): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE serverId = :serverId AND pageId = :pageId)")
    fun isFavorite(serverId: String, pageId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE serverId = :serverId AND pageId = :pageId")
    suspend fun removeFavorite(serverId: String, pageId: Long)

    @Query("DELETE FROM favorites WHERE serverId = :serverId")
    suspend fun clearFavoritesForServer(serverId: String)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history WHERE serverId = :serverId ORDER BY accessedAt DESC LIMIT :limit")
    fun getHistoryForServer(serverId: String, limit: Int = 100): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addHistory(history: HistoryEntity)

    @Query("DELETE FROM history WHERE serverId = :serverId AND pageId NOT IN (SELECT pageId FROM history WHERE serverId = :serverId ORDER BY accessedAt DESC LIMIT :limit)")
    suspend fun trimHistory(serverId: String, limit: Int = 100)

    @Query("DELETE FROM history WHERE serverId = :serverId")
    suspend fun clearHistoryForServer(serverId: String)
}
