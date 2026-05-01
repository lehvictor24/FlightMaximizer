package com.flightmaximizer.domain.model

import java.time.Instant

data class ScanRun(
    val id: String,
    val routeId: String,
    val startedAt: Instant,
    val finishedAt: Instant,
    val status: ScanStatus,
    val errorMessage: String?,
    val resultsCount: Int
)
