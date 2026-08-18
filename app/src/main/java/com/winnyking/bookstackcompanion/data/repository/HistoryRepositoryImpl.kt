package com.winnyking.bookstackcompanion.data.repository

import com.winnyking.bookstackcompanion.data.database.dao.HistoryDao
import com.winnyking.bookstackcompanion.data.database.entity.HistoryEntity
import com.winnyking.bookstackcompanion.domain.model.HistoryItem
import com.winnyking.bookstackcompanion.domain.repository.HistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryRepositoryImpl @Inject constructor(
    private val historyDao: HistoryDao
) : HistoryRepository {

    override fun getHistory(serverId: String): Flow<List<HistoryItem>> {
        return historyDao.getHistoryForServer(serverId).map { entities ->
            entities.map {
                HistoryItem(
                    id = "${it.serverId}_${it.pageId}",
                    serverId = it.serverId,
                    pageId = it.pageId,
                    pageName = it.pageName,
                    bookName = it.bookName,
                    chapterName = it.chapterName,
                    accessedAt = it.accessedAt
                )
            }
        }
    }

    override suspend fun addHistory(
        serverId: String,
        pageId: Long,
        pageName: String,
        bookName: String,
        chapterName: String
    ) {
        historyDao.addHistory(
            HistoryEntity(
                serverId = serverId,
                pageId = pageId,
                pageName = pageName,
                bookName = bookName,
                chapterName = chapterName,
                accessedAt = System.currentTimeMillis()
            )
        )
        historyDao.trimHistory(serverId)
    }

    override suspend fun clearHistory(serverId: String) {
        historyDao.clearHistoryForServer(serverId)
    }
}
