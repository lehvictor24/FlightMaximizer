package com.flightmaximizer.domain.model

data class ScanSchedule(
    val intervalMinutes: Int,    // minimum 15
    val isEnabled: Boolean,
    val requireWifi: Boolean,
    val requireCharging: Boolean
)
