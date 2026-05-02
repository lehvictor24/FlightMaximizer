package com.flightmaximizer.data.remote.google

import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.model.MultiCityLegParam
import com.flightmaximizer.domain.model.TripType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleFlightsUrlBuilder @Inject constructor() {

    fun build(params: FlightSearchParams): String = when (params.tripType) {
        TripType.ONE_WAY -> buildOneWay(params.origin, params.destination, params.departDate, params.currency)
        TripType.ROUND_TRIP -> buildRoundTrip(
            params.origin, params.destination,
            params.departDate, params.returnDate ?: params.departDate.plusDays(7),
            params.currency
        )
        TripType.MULTI_CITY -> buildMultiCity(params.legs, params.currency)
    }

    // Query-parameter form — the server receives these and may include data in the
    // initial HTML response, unlike the hash-fragment (#flt=…) form which is
    // stripped by the HTTP client before the request is sent.
    fun buildOneWay(origin: String, dest: String, date: LocalDate, currency: String = "USD"): String =
        "https://www.google.com/travel/flights/search" +
            "?tfs=CBwQARowEgoyMDI1LTA2LTAxagcIARID${origin}cgcIARID${dest}" +
            "&curr=$currency&gl=us&hl=en" +
            "&q=${origin}+to+${dest}+on+${date}"

    fun buildRoundTrip(origin: String, dest: String, depart: LocalDate, ret: LocalDate, currency: String = "USD"): String =
        "https://www.google.com/travel/flights/search" +
            "?curr=$currency&gl=us&hl=en" +
            "&q=${origin}+to+${dest}+from+${depart}+return+${ret}"

    fun buildMultiCity(legs: List<MultiCityLegParam>, currency: String = "USD"): String {
        val q = legs.joinToString("+and+") { "${it.origin}+to+${it.destination}+on+${it.departDate}" }
        return "https://www.google.com/travel/flights/search?curr=$currency&gl=us&hl=en&q=$q"
    }

    // Booking deep-link (opened in the browser from a notification or card tap).
    // Uses the hash fragment form which is correct for browser navigation — the
    // browser JS client reads it. This is separate from the scraping URL.
    fun buildBookingUrl(params: FlightSearchParams): String = when (params.tripType) {
        TripType.ONE_WAY ->
            "https://www.google.com/travel/flights#flt=${params.origin}.${params.destination}.${params.departDate}" +
                ";c:${params.currency};e:1;sd:1;t:f"
        TripType.ROUND_TRIP -> {
            val ret = params.returnDate ?: params.departDate.plusDays(7)
            "https://www.google.com/travel/flights#flt=${params.origin}.${params.destination}.${params.departDate}" +
                "*${params.destination}.${params.origin}.${ret};c:${params.currency};e:1;sd:1;t:r"
        }
        TripType.MULTI_CITY -> "https://www.google.com/travel/flights"
    }

    fun buildCalendarView(
        origin: String,
        dest: String,
        year: Int,
        month: Int,
        minNights: Int? = null,
        maxNights: Int? = null,
        currency: String = "USD"
    ): String {
        val dateStr = LocalDate.of(year, month, 1).toString()
        val stayParams = buildString {
            if (minNights != null) append(";li:$minNights")
            if (maxNights != null) append(";lx:$maxNights")
        }
        return "https://www.google.com/travel/flights#flt=$origin.$dest.$dateStr;c:$currency;e:1;sd:1;t:f$stayParams;d:m"
    }

    fun buildPriceMatrix(
        origin: String,
        dest: String,
        depart: LocalDate,
        ret: LocalDate,
        currency: String = "USD"
    ): String = "https://www.google.com/travel/flights#flt=$origin.$dest.$depart*$dest.$origin.$ret;c:$currency;e:1;sd:1;t:r;vm:p"
}
