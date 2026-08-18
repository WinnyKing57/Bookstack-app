package com.winnyking.bookstackcompanion.domain.usecase

import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import com.winnyking.bookstackcompanion.domain.repository.ServerRepository
import javax.inject.Inject

class SyncServerUseCase @Inject constructor(
    private val bookStackRepository: BookStackRepository,
    private val serverRepository: ServerRepository
) {
    suspend operator fun invoke(serverId: String): Result<Unit> {
        val booksResult = bookStackRepository.refreshBooks(serverId)
        val shelvesResult = bookStackRepository.refreshShelves(serverId)

        return if (booksResult.isSuccess && shelvesResult.isSuccess) {
            serverRepository.updateLastSyncTimestamp(serverId, System.currentTimeMillis())
            Result.success(Unit)
        } else {
            val error = booksResult.exceptionOrNull() ?: shelvesResult.exceptionOrNull()
            Result.failure(error ?: Exception("Erreur lors de la synchronisation"))
        }
    }
}
