package com.winnyking.bookstackcompanion.data.repository

import com.winnyking.bookstackcompanion.data.database.dao.FavoriteDao
import com.winnyking.bookstackcompanion.data.database.entity.FavoriteEntity
import com.winnyking.bookstackcompanion.domain.model.FavoriteItem
import com.winnyking.bookstackcompanion.domain.repository.FavoriteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoriteRepositoryImpl @Inject constructor(
    private val favoriteDao: FavoriteDao
) : FavoriteRepository {

    override fun getFavorites(serverId: String): Flow<List<FavoriteItem>> {
        return favoriteDao.getFavoritesForServer(serverId).map { entities ->
            entities.map {
                FavoriteItem(
                    id = "${it.serverId}_${it.pageId}",
                    serverId = it.serverId,
                    pageId = it.pageId,
                    pageName = it.pageName,
                    bookName = it.bookName,
                    chapterName = it.chapterName,
                    addedAt = it.addedAt
                )
            }
        }
    }

    override fun isFavorite(serverId: String, pageId: Long): Flow<Boolean> {
        return favoriteDao.isFavorite(serverId, pageId)
    }

    override suspend fun toggleFavorite(
        serverId: String,
        pageId: Long,
        pageName: String,
        bookName: String,
        chapterName: String
    ) {
        val currentlyFav = favoriteDao.isFavorite(serverId, pageId).first()
        if (currentlyFav) {
            favoriteDao.removeFavorite(serverId, pageId)
        } else {
            favoriteDao.addFavorite(
                FavoriteEntity(
                    serverId = serverId,
                    pageId = pageId,
                    pageName = pageName,
                    bookName = bookName,
                    chapterName = chapterName,
                    addedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
