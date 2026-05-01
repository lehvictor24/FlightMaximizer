package com.flightmaximizer.domain.model

import java.time.LocalDate

data class Leg(
    val id: String,
    val routeId: String,
    val legOrder: Int,
    val origin: String,
    val destination: String,
    val departDate: LocalDate?
)
