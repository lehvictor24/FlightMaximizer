package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.local.db.dao.FlightResultDao
import com.flightmaximizer.domain.model.PriceDelta
import com.flightmaximizer.domain.model.PriceDirection
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PriceAnalyzer @Inject constructor(
    private val flightResultDao: FlightResultDao
) {
    private val stableThresholdPercent = 2f

    /**
     * Compute a price delta by comparing [currentPriceCents] against the most recent
     * stored result for the same route/origin/destination triplet.
     */
    suspend fun computeDelta(
        routeId: String,
        origin: String,
        destination: String,
        currentPriceCents: Int
    ): PriceDelta? {
        val previous = flightResultDao.getLatestForRoute(routeId, origin, destination)
            ?: return null
        return computeDelta(currentPriceCents, previous.priceCents)
    }

    /**
     * Compute delta directly from two price values (for callers that already
     * have the previous price from a DAO query).
     */
    fun computeDelta(currentCents: Int, previousCents: Int?): PriceDelta? {
        if (previousCents == null || previousCents <= 0) return null
        val deltaCents = currentCents - previousCents
        val percentage = (deltaCents.toFloat() / previousCents) * 100f
        val absPct = kotlin.math.abs(percentage)
        val direction = when {
            absPct < stableThresholdPercent -> PriceDirection.STABLE
            deltaCents > 0 -> PriceDirection.UP
            else -> PriceDirection.DOWN
        }
        return PriceDelta(deltaCents, absPct, direction)
    }
}
