package com.flightmaximizer.data.remote.model

import com.flightmaximizer.domain.model.TripType
import java.time.ZonedDateTime

data class FlightOffer(
    val origin: String,
    val destination: String,
    val priceCents: Int,
    val currency: String,
    val airline: String?,
    val flightNumber: String?,
    val departDateTime: ZonedDateTime,
    val arriveDateTime: ZonedDateTime,
    val returnDateTime: ZonedDateTime?,
    val durationMinutes: Int?,
    val stops: Int,
    val bookingUrl: String?,
    val tripType: TripType,
    val source: String
)
