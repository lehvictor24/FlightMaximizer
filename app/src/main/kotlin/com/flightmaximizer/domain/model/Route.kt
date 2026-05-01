package com.flightmaximizer.domain.model

import java.time.LocalDate

data class Route(
    val id: String,
    val name: String,
    val origins: List<String>,       // IATA codes
    val destinations: List<String>,  // IATA codes
    val tripType: TripType,
    val dateMode: DateMode,
    val departDate: LocalDate?,
    val returnDate: LocalDate?,
    val flexDays: Int,
    val wheneverMonths: Int,
    val isActive: Boolean,
    val multiCityLegs: List<Leg>
)
