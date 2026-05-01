package com.flightmaximizer.domain.model

sealed class PricePrediction {
    data class Available(
        val trend: Trend,
        val confidence: Float,          // 0.0 to 1.0
        val suggestedAction: String,    // "Buy now", "Wait", "Price stable"
        val dataPoints: Int
    ) : PricePrediction()

    object Insufficient : PricePrediction()  // < 5 data points
}
