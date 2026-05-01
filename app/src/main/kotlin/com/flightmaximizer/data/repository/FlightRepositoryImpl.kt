package com.flightmaximizer.data.repository

import com.flightmaximizer.data.local.db.dao.FlightResultDao
import com.flightmaximizer.data.local.db.dao.PriceSnapshotDao
import com.flightmaximizer.data.local.db.dao.RouteDao
import com.flightmaximizer.data.local.db.dao.ScanRunDao
import com.flightmaximizer.data.local.db.entity.FlightResultEntity
import com.flightmaximizer.data.local.db.entity.MultiCityLegEntity
import com.flightmaximizer.data.local.db.entity.PriceSnapshotEntity
import com.flightmaximizer.data.local.db.entity.RouteAirportEntity
import com.flightmaximizer.data.local.db.entity.RouteEntity
import com.flightmaximizer.data.local.db.entity.ScanRunEntity
import com.flightmaximizer.data.remote.DataSourceException
import com.flightmaximizer.data.remote.FlightDataSource
import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.model.MultiCityLegParam
import com.flightmaximizer.data.remote.util.DelayStrategy
import com.flightmaximizer.domain.model.DateMode
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.model.Leg
import com.flightmaximizer.domain.model.PriceDelta
import com.flightmaximizer.domain.model.PriceDirection
import com.flightmaximizer.domain.model.PricePrediction
import com.flightmaximizer.domain.model.Route
import com.flightmaximizer.domain.model.ScanRun
import com.flightmaximizer.domain.model.ScanStatus
import com.flightmaximizer.domain.model.TripType
import com.flightmaximizer.domain.usecase.PriceAnalyzer
import com.flightmaximizer.domain.usecase.PricePredictionEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FlightRepositoryImpl @Inject constructor(
    private val routeDao: RouteDao,
    private val flightResultDao: FlightResultDao,
    private val scanRunDao: ScanRunDao,
    private val snapshotDao: PriceSnapshotDao,
    private val flightDataSource: FlightDataSource,
    private val delayStrategy: DelayStrategy,
    private val priceAnalyzer: PriceAnalyzer,
    private val predictionEngine: PricePredictionEngine
) : FlightRepository {

    // ── Routes ──────────────────────────────────────────────────────────────

    override fun getAllRoutes(): Flow<List<Route>> =
        routeDao.getAllRoutes().map { entities -> entities.map { loadFullRoute(it) } }

    override suspend fun getActiveRoutes(): List<Route> =
        routeDao.getActiveRoutesList().map { loadFullRoute(it) }

    override suspend fun getRoute(id: String): Route? =
        routeDao.getRouteById(id)?.let { loadFullRoute(it) }

    override suspend fun saveRoute(route: Route) {
        routeDao.insertRoute(route.toEntity())

        val airports = route.origins.map { iata ->
            RouteAirportEntity(UUID.randomUUID().toString(), route.id, iata, "ORIGIN")
        } + route.destinations.map { iata ->
            RouteAirportEntity(UUID.randomUUID().toString(), route.id, iata, "DESTINATION")
        }
        routeDao.insertAirports(airports)

        if (route.tripType == TripType.MULTI_CITY && route.multiCityLegs.isNotEmpty()) {
            routeDao.insertLegs(route.multiCityLegs.map { leg ->
                MultiCityLegEntity(
                    leg.id, leg.routeId, leg.legOrder,
                    leg.origin, leg.destination,
                    leg.departDate?.toEpochDay()
                )
            })
        }
    }

    override suspend fun deleteRoute(id: String) = routeDao.deleteRoute(id)

    override suspend fun toggleRouteActive(id: String, active: Boolean) =
        routeDao.setActive(id, if (active) 1 else 0)

    // ── Results ─────────────────────────────────────────────────────────────

    override fun getCheapestPerRoute(maxAgeDays: Int): Flow<List<FlightResult>> {
        val cutoffMs = System.currentTimeMillis() - (maxAgeDays * 86_400_000L)
        return flightResultDao.getCheapestPerRoute(cutoffMs).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getResults(origin: String?, destination: String?, maxAgeDays: Int): Flow<List<FlightResult>> {
        val cutoffMs = System.currentTimeMillis() - (maxAgeDays * 86_400_000L)
        // Filter in-memory since the DAO query uses routeId grouping
        return flightResultDao.getCheapestPerRoute(cutoffMs).map { entities ->
            entities
                .filter { e -> origin == null || e.origin == origin }
                .filter { e -> destination == null || e.destination == destination }
                .map { it.toDomain() }
        }
    }

    override fun getTotalResultCount(): Flow<Int> = flightResultDao.getTotalCount()

    // ── Scanning ─────────────────────────────────────────────────────────────

    override suspend fun runScanForRoute(route: Route): ScanRun {
        val runId = UUID.randomUUID().toString()
        val startedAt = System.currentTimeMillis()
        var resultsCount = 0
        var status = ScanStatus.SUCCESS
        var errorMsg: String? = null

        try {
            val pairs = if (route.tripType == TripType.MULTI_CITY) {
                emptyList()
            } else {
                route.origins.flatMap { o -> route.destinations.map { d -> o to d } }
            }

            for ((origin, dest) in pairs) {
                val paramsList = expandDates(route, origin, dest)
                for (params in paramsList) {
                    try {
                        delayStrategy.preRequestDelay()
                        val results = searchAndStore(params, route.id, runId)
                        resultsCount += results.size
                    } catch (e: DataSourceException.RateLimited) {
                        Timber.w("Rate limited — stopping scan")
                        status = ScanStatus.PARTIAL
                        break
                    } catch (e: Exception) {
                        Timber.w(e, "Scan failed for ${params.origin}→${params.destination}")
                        status = ScanStatus.PARTIAL
                    }
                    delayStrategy.betweenRoutesDelay()
                }
            }

            // Multi-city
            if (route.tripType == TripType.MULTI_CITY && route.multiCityLegs.isNotEmpty()) {
                val mcParams = FlightSearchParams(
                    origin = route.multiCityLegs.first().origin,
                    destination = route.multiCityLegs.last().destination,
                    tripType = TripType.MULTI_CITY,
                    departDate = route.multiCityLegs.first().departDate ?: LocalDate.now().plusDays(30),
                    legs = route.multiCityLegs.map {
                        MultiCityLegParam(it.origin, it.destination,
                            it.departDate ?: LocalDate.now().plusDays(30))
                    }
                )
                delayStrategy.preRequestDelay()
                val results = searchAndStore(mcParams, route.id, runId)
                resultsCount += results.size
            }
        } catch (e: Exception) {
            Timber.e(e, "Scan run failed")
            status = ScanStatus.FAILED
            errorMsg = e.message
        }

        val finishedAt = System.currentTimeMillis()
        scanRunDao.insert(ScanRunEntity(runId, route.id, startedAt, finishedAt, status.name, errorMsg, resultsCount))
        return ScanRun(runId, route.id, Instant.ofEpochMilli(startedAt), Instant.ofEpochMilli(finishedAt),
            status, errorMsg, resultsCount)
    }

    override suspend fun searchAndStore(
        params: FlightSearchParams,
        routeId: String,
        scanRunId: String
    ): List<FlightResult> {
        val offers = try {
            flightDataSource.searchFlights(params)
        } catch (e: Exception) {
            Timber.w(e, "Search failed for ${params.origin}→${params.destination}")
            emptyList()
        }

        val entities = offers.mapNotNull { offer ->
            try {
                val delta = priceAnalyzer.computeDelta(routeId, offer.origin, offer.destination, offer.priceCents)

                snapshotDao.insert(
                    PriceSnapshotEntity(
                        id = UUID.randomUUID().toString(),
                        routeId = routeId,
                        origin = offer.origin,
                        destination = offer.destination,
                        departDate = params.departDate.toEpochDay(),
                        returnDate = params.returnDate?.toEpochDay(),
                        priceCents = offer.priceCents,
                        currency = offer.currency,
                        source = offer.source,
                        recordedAt = System.currentTimeMillis()
                    )
                )

                FlightResultEntity(
                    id = UUID.randomUUID().toString(),
                    scanRunId = scanRunId,
                    routeId = routeId,
                    origin = offer.origin,
                    destination = offer.destination,
                    priceCents = offer.priceCents,
                    currency = offer.currency,
                    airline = offer.airline,
                    flightNumber = offer.flightNumber,
                    departDateTime = offer.departDateTime.toInstant().toEpochMilli(),
                    arriveDateTime = offer.arriveDateTime.toInstant().toEpochMilli(),
                    returnDateTime = offer.returnDateTime?.toInstant()?.toEpochMilli(),
                    durationMinutes = offer.durationMinutes,
                    stops = offer.stops,
                    bookingUrl = offer.bookingUrl,
                    tripType = offer.tripType.name,
                    source = offer.source,
                    scrapedAt = System.currentTimeMillis(),
                    priceDeltaCents = delta?.deltaCents,
                    priceDirection = delta?.direction?.name
                )
            } catch (e: Exception) {
                Timber.w(e, "Failed to map offer")
                null
            }
        }

        if (entities.isNotEmpty()) flightResultDao.insertAll(entities)
        return entities.map { it.toDomain() }
    }

    // ── Cleanup ──────────────────────────────────────────────────────────────

    override suspend fun pruneOldData(maxHistoryDays: Int) = cleanOldData(maxHistoryDays)

    override suspend fun cleanOldData(maxHistoryDays: Int) {
        val cutoffMs = System.currentTimeMillis() - (maxHistoryDays * 86_400_000L)
        flightResultDao.deleteOlderThan(cutoffMs)
        scanRunDao.deleteOlderThan(cutoffMs)
        snapshotDao.deleteOlderThan(cutoffMs)
    }

    // ── Date expansion ────────────────────────────────────────────────────────

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
                (-flexDays..flexDays).map { offset ->
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

    // ── Mapping helpers ───────────────────────────────────────────────────────

    private suspend fun loadFullRoute(entity: RouteEntity): Route {
        val airports = routeDao.getAirportsForRoute(entity.id)
        val legs = routeDao.getLegsForRoute(entity.id).map { leg ->
            Leg(leg.id, leg.routeId, leg.legOrder, leg.origin, leg.destination,
                leg.departDate?.let { LocalDate.ofEpochDay(it) })
        }
        return Route(
            id = entity.id,
            name = entity.name,
            origins = airports.filter { it.role == "ORIGIN" }.map { it.iata },
            destinations = airports.filter { it.role == "DESTINATION" }.map { it.iata },
            tripType = TripType.valueOf(entity.tripType),
            dateMode = DateMode.valueOf(entity.dateMode),
            departDate = entity.departDate?.let { LocalDate.ofEpochDay(it) },
            returnDate = entity.returnDate?.let { LocalDate.ofEpochDay(it) },
            flexDays = entity.flexDays,
            wheneverMonths = entity.wheneverMonths,
            isActive = entity.isActive == 1,
            multiCityLegs = legs
        )
    }

    private fun Route.toEntity() = RouteEntity(
        id = id,
        name = name,
        tripType = tripType.name,
        dateMode = dateMode.name,
        departDate = departDate?.toEpochDay(),
        returnDate = returnDate?.toEpochDay(),
        flexDays = flexDays,
        wheneverMonths = wheneverMonths,
        isActive = if (isActive) 1 else 0,
        createdAt = System.currentTimeMillis()
    )

    private fun FlightResultEntity.toDomain(): FlightResult {
        val direction = priceDirection?.let {
            runCatching { PriceDirection.valueOf(it) }.getOrNull()
        }
        val delta = if (priceDeltaCents != null && direction != null) {
            val pct = if (priceCents > 0)
                kotlin.math.abs(priceDeltaCents.toFloat() / priceCents * 100f) else 0f
            PriceDelta(priceDeltaCents, pct, direction)
        } else null

        return FlightResult(
            id = id,
            scanRunId = scanRunId,
            routeId = routeId,
            origin = origin,
            destination = destination,
            priceCents = priceCents,
            currency = currency,
            airline = airline,
            flightNumber = flightNumber,
            departDateTime = Instant.ofEpochMilli(departDateTime).atZone(ZoneOffset.UTC),
            arriveDateTime = Instant.ofEpochMilli(arriveDateTime).atZone(ZoneOffset.UTC),
            returnDateTime = returnDateTime?.let { Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC) },
            durationMinutes = durationMinutes,
            stops = stops,
            bookingUrl = bookingUrl,
            tripType = TripType.valueOf(tripType),
            source = source,
            scrapedAt = Instant.ofEpochMilli(scrapedAt),
            priceDelta = delta
        )
    }
}
