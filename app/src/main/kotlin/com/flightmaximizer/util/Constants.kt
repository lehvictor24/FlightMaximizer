package com.flightmaximizer.util

object Constants {
    const val DEEP_LINK_SCHEME = "flightmaximizer"
    const val DEEP_LINK_HOST = "home"
    const val DEEP_LINK_BASE = "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST"
    const val PARAM_SCAN_RUN_ID = "scanRunId"

    const val NOTIFICATION_CHANNEL_RESULTS = "channel_scan_results"
    const val NOTIFICATION_CHANNEL_ERRORS = "channel_errors"
    const val NOTIFICATION_ID_SCAN = 1001
    const val NOTIFICATION_ID_ERROR = 1002

    const val MIN_SCAN_INTERVAL_MINUTES = 15
    const val DEFAULT_SCAN_INTERVAL_MINUTES = 60
    const val DEFAULT_MIN_DELAY_MS = 2_000L
    const val DEFAULT_MAX_DELAY_MS = 8_000L
    const val DEFAULT_MAX_RETRIES = 3
    const val DEFAULT_MAX_HISTORY_DAYS = 30
    const val DEFAULT_WHENEVER_MONTHS = 3
    const val DEFAULT_FLEX_DAYS = 7
}
