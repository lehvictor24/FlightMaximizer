package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.local.db.dao.PriceSnapshotDao
import com.flightmaximizer.domain.model.PricePrediction
import com.flightmaximizer.domain.model.Trend
import timber.log.Timber
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PricePredictionEngine @Inject constructor(
    private val snapshotDao: PriceSnapshotDao
) {
    private val minDataPoints = 5

    /**
     * Predicts price trend for a given route and departure date using linear regression
     * over the most recent 30 price snapshots.
     */
    suspend fun predict(origin: String, destination: String, departDate: LocalDate): PricePrediction {
        val epochDay = departDate.toEpochDay()
        val snapshots = snapshotDao.getSnapshots(origin, destination, epochDay, limit = 30)
        if (snapshots.size < minDataPoints) return PricePrediction.Insufficient

        return predictFromPrices(snapshots.map { it.priceCents }, snapshots.size)
    }

    /**
     * Predicts price trend directly from an ordered list of historical prices (oldest first).
     * Uses simple linear regression: slope of price over time determines trend.
     */
    fun predict(historicalPricesCents: List<Int>): PricePrediction {
        if (historicalPricesCents.size < minDataPoints) return PricePrediction.Insufficient
        return predictFromPrices(historicalPricesCents, historicalPricesCents.size)
    }

    private fun predictFromPrices(prices: List<Int>, dataPoints: Int): PricePrediction {
        val n = prices.size.toDouble()
        val points = prices.mapIndexed { i, price -> i.toDouble() to price.toDouble() }

        val sumX = points.sumOf { it.first }
        val sumY = points.sumOf { it.second }
        val sumXY = points.sumOf { it.first * it.second }
        val sumX2 = points.sumOf { it.first * it.first }

        val denominator = n * sumX2 - sumX * sumX
        if (denominator == 0.0) return PricePrediction.Insufficient

        val slope = (n * sumXY - sumX * sumY) / denominator
        val intercept = (sumY - slope * sumX) / n

        // Confidence: R² value
        val meanY = sumY / n
        val ssTot = points.sumOf { (it.second - meanY) * (it.second - meanY) }
        val ssRes = points.sumOf { (x, y) ->
            val yHat = slope * x + intercept
            (y - yHat) * (y - yHat)
        }
        val confidence = if (ssTot == 0.0) 0f else (1.0 - ssRes / ssTot).toFloat().coerceIn(0f, 1f)

        // Classify trend by slope relative to average price
        val avgPrice = meanY
        val slopePercent = if (avgPrice != 0.0) (slope / avgPrice) * 100.0 else 0.0
        val trend = when {
            slopePercent > 1.5 -> Trend.RISING
            slopePercent < -1.5 -> Trend.FALLING
            else -> Trend.FLAT
        }

        val action = when (trend) {
            Trend.RISING -> "Buy now — price is rising"
            Trend.FALLING -> "Wait — price may drop further"
            Trend.FLAT -> "Price is stable"
        }

        return PricePrediction.Available(
            trend = trend,
            confidence = confidence,
            suggestedAction = action,
            dataPoints = dataPoints
        )
    }
}
