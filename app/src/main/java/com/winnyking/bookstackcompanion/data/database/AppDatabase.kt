package com.winnyking.bookstackcompanion.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.winnyking.bookstackcompanion.data.database.dao.BookDao
import com.winnyking.bookstackcompanion.data.database.dao.ChapterDao
import com.winnyking.bookstackcompanion.data.database.dao.FavoriteDao
import com.winnyking.bookstackcompanion.data.database.dao.HistoryDao
import com.winnyking.bookstackcompanion.data.database.dao.PageDao
import com.winnyking.bookstackcompanion.data.database.dao.ServerDao
import com.winnyking.bookstackcompanion.data.database.dao.ShelfDao
import com.winnyking.bookstackcompanion.data.database.entity.BookEntity
import com.winnyking.bookstackcompanion.data.database.entity.ChapterEntity
import com.winnyking.bookstackcompanion.data.database.entity.FavoriteEntity
import com.winnyking.bookstackcompanion.data.database.entity.HistoryEntity
import com.winnyking.bookstackcompanion.data.database.entity.PageEntity
import com.winnyking.bookstackcompanion.data.database.entity.ServerEntity
import com.winnyking.bookstackcompanion.data.database.entity.ShelfEntity

@Database(
    entities = [
        ServerEntity::class,
        BookEntity::class,
        ShelfEntity::class,
        ChapterEntity::class,
        PageEntity::class,
        FavoriteEntity::class,
        HistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
    abstract fun bookDao(): BookDao
    abstract fun shelfDao(): ShelfDao
    abstract fun chapterDao(): ChapterDao
    abstract fun pageDao(): PageDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
}
