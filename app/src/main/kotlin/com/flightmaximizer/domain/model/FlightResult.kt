package com.flightmaximizer.domain.model

import java.time.Instant
import java.time.ZonedDateTime

data class FlightResult(
    val id: String,
    val scanRunId: String,
    val routeId: String,
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
    val source: String,             // "google_flights" or "kiwi"
    val scrapedAt: Instant,
    val priceDelta: PriceDelta?,    // null on first scan for this route
    val prediction: PricePrediction = PricePrediction.Insufficient
)
