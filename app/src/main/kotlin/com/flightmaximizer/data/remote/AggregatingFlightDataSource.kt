package com.flightmaximizer.data.remote

import com.flightmaximizer.data.remote.model.FlightOffer
import com.flightmaximizer.data.remote.model.FlightSearchParams
import timber.log.Timber

class AggregatingFlightDataSource(
    private val sources: List<FlightDataSource>
) : FlightDataSource {
    override val sourceName = "Aggregated (${sources.joinToString { it.sourceName }})"

    override suspend fun searchFlights(params: FlightSearchParams): List<FlightOffer> =
        sources.flatMap { source ->
            runCatching { source.searchFlights(params) }
                .onFailure { e ->
                    if (e is DataSourceException.RateLimited) throw e
                    Timber.w(e, "${source.sourceName} failed for ${params.origin}→${params.destination}")
                }
                .getOrElse { emptyList() }
        }
}
