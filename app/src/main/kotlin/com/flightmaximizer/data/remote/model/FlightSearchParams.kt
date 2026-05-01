package com.flightmaximizer.data.remote.model

import com.flightmaximizer.domain.model.TripType
import java.time.LocalDate

data class FlightSearchParams(
    val origin: String,
    val destination: String,
    val tripType: TripType,
    val departDate: LocalDate,
    val returnDate: LocalDate? = null,
    val adults: Int = 1,
    val currency: String = "USD",
    val legs: List<MultiCityLegParam> = emptyList()
)

data class MultiCityLegParam(
    val origin: String,
    val destination: String,
    val departDate: LocalDate
)
