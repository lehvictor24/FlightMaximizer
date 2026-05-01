package com.flightmaximizer.data.remote.google

import com.flightmaximizer.data.remote.model.FlightSearchParams
import com.flightmaximizer.data.remote.model.MultiCityLegParam
import com.flightmaximizer.domain.model.TripType
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleFlightsUrlBuilder @Inject constructor() {
    private val base = "https://www.google.com/travel/flights"

    fun build(params: FlightSearchParams): String = when (params.tripType) {
        TripType.ONE_WAY -> buildOneWay(params.origin, params.destination, params.departDate, params.currency)
        TripType.ROUND_TRIP -> buildRoundTrip(
            params.origin, params.destination,
            params.departDate, params.returnDate ?: params.departDate.plusDays(7),
            params.currency
        )
        TripType.MULTI_CITY -> buildMultiCity(params.legs, params.currency)
    }

    fun buildOneWay(origin: String, dest: String, date: LocalDate, currency: String = "USD"): String =
        "$base#flt=$origin.$dest.${date};c:$currency;e:1;sd:1;t:f"

    fun buildRoundTrip(origin: String, dest: String, depart: LocalDate, ret: LocalDate, currency: String = "USD"): String =
        "$base#flt=$origin.$dest.$depart*$dest.$origin.$ret;c:$currency;e:1;sd:1;t:r"

    fun buildMultiCity(legs: List<MultiCityLegParam>, currency: String = "USD"): String {
        val legStr = legs.joinToString("*") { "${it.origin}.${it.destination}.${it.departDate}" }
        return "$base#flt=$legStr;c:$currency;e:1;sd:1;t:m"
    }

    /**
     * Calendar/price-graph URL for flexible and "whenever" modes.
     * Uses Google Flights' month calendar view (d:m) with optional min/max stay.
     */
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
        return "$base#flt=$origin.$dest.$dateStr;c:$currency;e:1;sd:1;t:f$stayParams;d:m"
    }

    /**
     * Price matrix URL (departure × return grid) for flexible round-trip.
     * vm:p activates the grid view showing different depart/return date combos.
     */
    fun buildPriceMatrix(
        origin: String,
        dest: String,
        depart: LocalDate,
        ret: LocalDate,
        currency: String = "USD"
    ): String = "$base#flt=$origin.$dest.$depart*$dest.$origin.$ret;c:$currency;e:1;sd:1;t:r;vm:p"
}
