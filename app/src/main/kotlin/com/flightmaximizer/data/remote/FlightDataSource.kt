package com.flightmaximizer.data.remote

import com.flightmaximizer.data.remote.model.FlightOffer
import com.flightmaximizer.data.remote.model.FlightSearchParams

interface FlightDataSource {
    val sourceName: String
    suspend fun searchFlights(params: FlightSearchParams): List<FlightOffer>
}

sealed class DataSourceException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class RateLimited(val retryAfterMs: Long = 60_000L) : DataSourceException("Rate limited by server")
    class ParseError(message: String, cause: Throwable? = null) : DataSourceException(message, cause)
    class NetworkError(cause: Throwable) : DataSourceException("Network error: ${cause.message}", cause)
    class NoResults(val params: FlightSearchParams) : DataSourceException("No results for ${params.origin}→${params.destination}")
}
