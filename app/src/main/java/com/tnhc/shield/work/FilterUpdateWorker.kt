package com.tnhc.shield.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.tnhc.shield.data.prefs.ShieldPreferences
import com.tnhc.shield.filter.FilterListManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class FilterUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val filterListManager: FilterListManager,
    private val prefs: ShieldPreferences
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "filter_update"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FilterUpdateWorker>(
                12, TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun scheduleOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<FilterUpdateWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "${WORK_NAME}_once",
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result {
        return try {
            if (!prefs.autoUpdateFilters.first()) return Result.success()
            filterListManager.downloadAll()
            prefs.setLastFilterUpdate(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
