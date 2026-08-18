package com.winnyking.bookstackcompanion.data.repository

import com.winnyking.bookstackcompanion.data.api.DynamicApiClientFactory
import com.winnyking.bookstackcompanion.data.api.UrlSanitizer
import com.winnyking.bookstackcompanion.data.database.dao.BookDao
import com.winnyking.bookstackcompanion.data.database.dao.ChapterDao
import com.winnyking.bookstackcompanion.data.database.dao.FavoriteDao
import com.winnyking.bookstackcompanion.data.database.dao.HistoryDao
import com.winnyking.bookstackcompanion.data.database.dao.PageDao
import com.winnyking.bookstackcompanion.data.database.dao.ServerDao
import com.winnyking.bookstackcompanion.data.database.dao.ShelfDao
import com.winnyking.bookstackcompanion.data.security.SecureStorageManager
import com.winnyking.bookstackcompanion.domain.model.ServerConfig
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServerRepositoryImpl @Inject constructor(
    private val serverDao: ServerDao,
    private val secureStorageManager: SecureStorageManager,
    private val apiClientFactory: DynamicApiClientFactory,
    private val bookDao: BookDao,
    private val shelfDao: ShelfDao,
    private val pageDao: PageDao,
    private val favoriteDao: FavoriteDao,
    private val historyDao: HistoryDao
) : ServerRepository {

    override fun getAllServers(): Flow<List<ServerConfig>> {
        return serverDao.getAllServers().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getSelectedServer(): Flow<ServerConfig?> {
        return serverDao.getSelectedServer().map { entity ->
            entity?.toDomainModel()
        }
    }

    override suspend fun saveServer(
        name: String,
        baseUrl: String,
        tokenId: String,
        tokenSecret: String
    ): Result<ServerConfig> {
        return try {
            val sanitizedUrl = UrlSanitizer.sanitizeBaseUrl(baseUrl)
            val testResult = testServerConnectionResult(sanitizedUrl, tokenId, tokenSecret)
            if (testResult.isFailure) {
                return Result.failure(testResult.exceptionOrNull() ?: Exception("Impossible de se connecter au serveur BookStack."))
            }

            val serverId = UUID.randomUUID().toString()
            secureStorageManager.saveServerCredentials(serverId, tokenId, tokenSecret)

            val serverEntity = com.winnyking.bookstackcompanion.data.database.entity.ServerEntity(
                id = serverId,
                name = name,
                baseUrl = sanitizedUrl,
                isSelected = true,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            serverDao.insertServer(serverEntity)
            serverDao.setSelectedServer(serverId)

            Result.success(serverEntity.toDomainModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectServer(serverId: String) {
        serverDao.setSelectedServer(serverId)
    }

    override suspend fun deleteServer(serverId: String) {
        secureStorageManager.deleteServerCredentials(serverId)
        bookDao.deleteBooksForServer(serverId)
        shelfDao.deleteShelvesForServer(serverId)
        pageDao.clearCachedPages(serverId)
        favoriteDao.clearFavoritesForServer(serverId)
        historyDao.clearHistoryForServer(serverId)
        serverDao.deleteServer(serverId)
    }

    override suspend fun testServerConnection(
        baseUrl: String,
        tokenId: String,
        tokenSecret: String
    ): Boolean {
        return testServerConnectionResult(baseUrl, tokenId, tokenSecret).isSuccess
    }

    override suspend fun testServerConnectionResult(
        baseUrl: String,
        tokenId: String,
        tokenSecret: String
    ): Result<Unit> {
        return apiClientFactory.testConnection(baseUrl, tokenId, tokenSecret)
    }

    override suspend fun updateLastSyncTimestamp(serverId: String, timestamp: Long) {
        val existing = serverDao.getServerById(serverId)
        if (existing != null) {
            serverDao.insertServer(existing.copy(lastSyncTimestamp = timestamp))
        }
    }

    private fun com.winnyking.bookstackcompanion.data.database.entity.ServerEntity.toDomainModel(): ServerConfig {
        return ServerConfig(
            id = id,
            name = name,
            baseUrl = baseUrl,
            tokenId = secureStorageManager.getTokenId(id),
            tokenSecret = secureStorageManager.getTokenSecret(id),
            isSelected = isSelected,
            lastSyncTimestamp = lastSyncTimestamp
        )
    }
}
