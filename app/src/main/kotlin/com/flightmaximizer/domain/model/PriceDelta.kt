package com.flightmaximizer.domain.model

data class PriceDelta(
    val deltaCents: Int,         // positive = more expensive, negative = cheaper
    val percentage: Float,       // absolute percentage change
    val direction: PriceDirection
)
