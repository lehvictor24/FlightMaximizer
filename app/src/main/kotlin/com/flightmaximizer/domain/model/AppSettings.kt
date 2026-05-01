package com.flightmaximizer.domain.model

data class AppSettings(
    val minDelayMs: Long = 2000L,
    val maxDelayMs: Long = 8000L,
    val maxRetries: Int = 3,
    val useRandomUserAgent: Boolean = true,
    val currency: String = "USD",
    val maxHistoryDays: Int = 30,
    val kiwiApiKey: String = "",
    val scanIntervalMinutes: Int = 60,
    val scanEnabled: Boolean = false,
    val requireWifi: Boolean = false,
    val requireCharging: Boolean = false
)
