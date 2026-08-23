package com.winnyking.bookstackcompanion.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.winnyking.bookstackcompanion.domain.repository.BookStackRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class BookDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val bookStackRepository: BookStackRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val serverId = inputData.getString(KEY_SERVER_ID)
            ?: return Result.failure()
        val bookId = inputData.getLong(KEY_BOOK_ID, -1L)
        if (bookId <= 0L) return Result.failure()

        return try {
            bookStackRepository.downloadBookForOffline(serverId, bookId) { completed, total ->
                setProgress(
                    workDataOf(
                        KEY_BOOK_ID to bookId,
                        KEY_COMPLETED to completed,
                        KEY_TOTAL to total
                    )
                )
            }
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_SERVER_ID = "server_id"
        const val KEY_BOOK_ID = "book_id"
        const val KEY_COMPLETED = "completed"
        const val KEY_TOTAL = "total"

        const val WORK_TAG = "book_download"
        fun workName(bookId: Long) = "book_download_$bookId"

        private const val MAX_RETRIES = 3
    }
}
