package com.flightmaximizer.data.repository

import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.model.Route
import com.flightmaximizer.domain.model.ScanRun
import kotlinx.coroutines.flow.Flow

interface FlightRepository {
    // Routes
    fun getAllRoutes(): Flow<List<Route>>
    suspend fun getActiveRoutes(): List<Route>
    suspend fun getRoute(id: String): Route?
    suspend fun saveRoute(route: Route)
    suspend fun deleteRoute(id: String)
    suspend fun toggleRouteActive(id: String, active: Boolean)

    // Results
    fun getCheapestPerRoute(maxAgeDays: Int = 30): Flow<List<FlightResult>>
    fun getResults(origin: String?, destination: String?, maxAgeDays: Int = 30): Flow<List<FlightResult>>
    fun getTotalResultCount(): Flow<Int>

    // Scanning
    suspend fun runScanForRoute(route: Route): ScanRun
    suspend fun searchAndStore(
        params: FlightSearchParams,
        routeId: String,
        scanRunId: String
    ): List<FlightResult>

    // Cleanup
    suspend fun pruneOldData(maxHistoryDays: Int)
    suspend fun cleanOldData(maxHistoryDays: Int)
}
