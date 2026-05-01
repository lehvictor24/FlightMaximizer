package com.flightmaximizer.data.remote.util

import com.flightmaximizer.data.local.datastore.AppPreferences
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.random.Random

@Singleton
class DelayStrategy @Inject constructor(
    private val prefs: AppPreferences
) {
    suspend fun preRequestDelay() {
        val min = prefs.minDelayMs.first()
        val max = prefs.maxDelayMs.first()
        delay(min + Random.nextLong(max - min + 1))
    }

    suspend fun betweenRequestsDelay() {
        val min = prefs.minDelayMs.first()
        delay(min * 2 + Random.nextLong(3000L))
    }

    // Kept for backward-compatibility with call sites that use betweenRoutesDelay()
    suspend fun betweenRoutesDelay() = betweenRequestsDelay()

    suspend fun backoffDelay(attempt: Int) {
        val base = 5_000L
        val backoff = (base * 2.0.pow(attempt.toDouble())).toLong()
        val jitter = Random.nextLong(2000L)
        delay((backoff + jitter).coerceAtMost(120_000L))
    }

    /**
     * Configure min/max values directly (e.g. for testing or legacy callers).
     * Writes persist only in-memory; DataStore values take precedence on next read.
     */
    fun configure(min: Long, max: Long) {
        // no-op: values are read live from DataStore on every call
    }
}
