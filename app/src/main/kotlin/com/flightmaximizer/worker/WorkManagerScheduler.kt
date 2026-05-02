package com.flightmaximizer.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    fun schedule(intervalMinutes: Int, requireWifi: Boolean = false, requireCharging: Boolean = false) {
        val clamped = intervalMinutes.coerceAtLeast(15)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (requireWifi) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .setRequiresCharging(requireCharging)
            .build()

        val request = PeriodicWorkRequestBuilder<FlightScanWorker>(
            clamped.toLong(), TimeUnit.MINUTES,
            5L, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1L, TimeUnit.MINUTES)
            .addTag(FlightScanWorker.WORK_TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            FlightScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request
        )
    }

    fun cancel() = workManager.cancelUniqueWork(FlightScanWorker.WORK_NAME)

    fun reschedule(intervalMinutes: Int, requireWifi: Boolean = false, requireCharging: Boolean = false) {
        cancel()
        schedule(intervalMinutes, requireWifi, requireCharging)
    }

    fun triggerNow() = runNow()

    fun runNow() {
        val request = OneTimeWorkRequestBuilder<FlightScanWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag(FlightScanWorker.WORK_TAG)
            .build()
        workManager.enqueue(request)
    }

    fun getWorkInfo(): Flow<WorkInfo?> =
        // Observe by tag so both the periodic work and one-shot "Scan Now" requests are visible.
        // Prefer a currently-running entry; fall back to whatever is most recent.
        workManager.getWorkInfosByTagFlow(FlightScanWorker.WORK_TAG)
            .map { infos ->
                infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
                    ?: infos.firstOrNull()
            }
}
