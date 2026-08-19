package com.winnyking.bookstackcompanion.domain.usecase

import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class SyncServerUseCase @Inject constructor(
    private val bookStackRepository: BookStackRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(serverId: String): Result<Unit> = coroutineScope {
        val booksDeferred = async { bookStackRepository.refreshBooks(serverId) }
        val shelvesDeferred = async { bookStackRepository.refreshShelves(serverId) }

        val booksResult = booksDeferred.await()
        val shelvesResult = shelvesDeferred.await()

        if (booksResult.isSuccess && shelvesResult.isSuccess) {
            serverRepository.updateLastSyncTimestamp(serverId, System.currentTimeMillis())
            Result.success(Unit)
        } else {
            val error = booksResult.exceptionOrNull() ?: shelvesResult.exceptionOrNull()
            Result.failure(error ?: Exception("Erreur lors de la synchronisation"))
        }
    }
}
