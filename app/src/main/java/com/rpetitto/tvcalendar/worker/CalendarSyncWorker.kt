package com.rpetitto.tvcalendar.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.rpetitto.tvcalendar.data.CalendarRepository

/**
 * Periodic background sync. Builds the repository from the app context (no DI
 * framework needed) and asks it to refresh the cache. Network errors retry;
 * anything else is treated as a non-fatal success so the schedule survives.
 */
class CalendarSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val repository = CalendarRepository.create(applicationContext)
        return when (val result = repository.syncEvents()) {
            is CalendarRepository.SyncResult.Success -> Result.success()
            is CalendarRepository.SyncResult.NetworkError -> {
                Log.w(TAG, "Sync network error, will retry")
                Result.retry()
            }
            is CalendarRepository.SyncResult.NeedsAuth -> {
                // Nothing the worker can do until the user re-pairs; don't churn.
                Log.w(TAG, "Sync skipped — needs authentication")
                Result.success()
            }
            is CalendarRepository.SyncResult.Error -> {
                Log.w(TAG, "Sync error: ${result.message}")
                Result.success()
            }
        }
    }

    companion object {
        private const val TAG = "CalendarSyncWorker"
    }
}
