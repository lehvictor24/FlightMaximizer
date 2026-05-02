package com.flightmaximizer.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.data.remote.DataSourceException
import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.util.DelayStrategy
import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.domain.model.DateMode
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.model.Route
import com.flightmaximizer.domain.model.TripType
import com.flightmaximizer.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate
import java.util.UUID

@HiltWorker
class FlightScanWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val flightRepository: FlightRepository,
    private val notificationHelper: NotificationHelper,
    private val appPreferences: AppPreferences,
    private val delayStrategy: DelayStrategy
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "FlightScanPeriodicWork"
        const val WORK_TAG = "flight_scan"
    }

    override suspend fun doWork(): Result {
        val maxRetries = appPreferences.maxRetries.first()
        if (runAttemptCount > maxRetries) {
            Timber.w("Max retries exceeded for scan worker")
            return Result.failure()
        }

        Timber.d("FlightScanWorker starting (attempt $runAttemptCount)")

        val activeRoutes = flightRepository.getActiveRoutes()
        if (activeRoutes.isEmpty()) {
            Timber.d("No active routes — skipping scan")
            return Result.success()
        }

        val scanRunId = UUID.randomUUID().toString()
        var overallCheapest: FlightResult? = null
        var totalFound = 0
        val routeName = if (activeRoutes.size == 1) activeRoutes[0].name else "${activeRoutes.size} routes"

        for (route in activeRoutes) {
            val combinations = route.origins.flatMap { origin ->
                route.destinations.map { dest -> origin to dest }
            }

            for ((origin, dest) in combinations) {
                val searchParamsList = expandDates(route, origin, dest)

                for (params in searchParamsList) {
                    try {
                        val results = flightRepository.searchAndStore(params, route.id, scanRunId)
                        totalFound += results.size
                        results.minByOrNull { it.priceCents }?.let { candidate ->
                            if (overallCheapest == null || candidate.priceCents < overallCheapest!!.priceCents) {
                                overallCheapest = candidate
                            }
                        }
                    } catch (e: DataSourceException.RateLimited) {
                        Timber.w("Rate limited — retrying worker")
                        return Result.retry()
                    } catch (e: Exception) {
                        Timber.w(e, "Scan failed for $origin→$dest")
                    }
                    delayStrategy.betweenRequestsDelay()
                }
            }
        }

        notificationHelper.postScanSummary(
            cheapestResult = overallCheapest,
            totalScanned = totalFound,
            routeName = routeName
        )

        val maxHistoryDays = appPreferences.maxHistoryDays.first()
        flightRepository.cleanOldData(maxHistoryDays)

        Timber.d("FlightScanWorker complete: $totalFound results found")
        return Result.success(
            workDataOf(
                "last_scan_ms" to System.currentTimeMillis(),
                "results_count" to totalFound
            )
        )
    }

    private fun expandDates(route: Route, origin: String, dest: String): List<FlightSearchParams> {
        return when (route.dateMode) {
            DateMode.FIXED -> {
                val depart = route.departDate ?: LocalDate.now().plusDays(30)
                listOf(
                    FlightSearchParams(
                        origin = origin,
                        destination = dest,
                        tripType = route.tripType,
                        departDate = depart,
                        returnDate = route.returnDate
                    )
                )
            }
            DateMode.FLEXIBLE -> {
                val base = route.departDate ?: LocalDate.now().plusDays(14)
                val flexDays = route.flexDays.coerceAtLeast(1)
                (-flexDays..flexDays step flexDays).map { offset ->
                    FlightSearchParams(
                        origin = origin,
                        destination = dest,
                        tripType = route.tripType,
                        departDate = base.plusDays(offset.toLong()),
                        returnDate = route.returnDate?.plusDays(offset.toLong())
                    )
                }
            }
            DateMode.WHENEVER -> {
                val months = route.wheneverMonths.coerceAtLeast(1)
                (0 until months * 2).map { biweekOffset ->
                    FlightSearchParams(
                        origin = origin,
                        destination = dest,
                        tripType = route.tripType,
                        departDate = LocalDate.now().plusWeeks(biweekOffset.toLong() + 1),
                        returnDate = null
                    )
                }
            }
        }
    }
}
