package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.data.local.db.dao.PriceSnapshotDao
import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.domain.model.FlightResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetCheapestFlightsUseCase @Inject constructor(
    private val repository: FlightRepository,
    private val prefs: AppPreferences,
    private val snapshotDao: PriceSnapshotDao,
    private val predictionEngine: PricePredictionEngine
) {
    operator fun invoke(): Flow<List<FlightResult>> =
        combine(
            repository.getCheapestPerRoute(),
            prefs.maxHistoryDays
        ) { results, _ ->
            results.map { result ->
                // Attach prediction if enough history exists
                val departDay = result.departDateTime.toLocalDate().toEpochDay()
                val snapshots = snapshotDao.getSnapshots(
                    result.origin, result.destination, departDay)
                val prediction = predictionEngine.predict(snapshots.map { it.priceCents })
                result.copy(prediction = prediction)
            }
        }
}
