package com.flightmaximizer.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.US)
private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)
private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)

fun Long.toDisplayTime(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(timeFormatter)

fun Long.toDisplayDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(dateFormatter)

fun Long.toShortDisplayDate(): String =
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(shortDateFormatter)

fun Int.centsToDisplayPrice(currency: String = "USD"): String {
    val symbol = when (currency.uppercase()) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "CAD" -> "CA$"
        "AUD" -> "A$"
        else -> currency
    }
    val dollars = this / 100
    val cents = this % 100
    return if (cents == 0) "$symbol$dollars" else "$symbol$dollars.${cents.toString().padStart(2, '0')}"
}

fun Long.toRelativeTime(): String {
    val now = System.currentTimeMillis()
    val diffMs = now - this
    return when {
        diffMs < 60_000 -> "just now"
        diffMs < 3_600_000 -> "${diffMs / 60_000}m ago"
        diffMs < 86_400_000 -> "${diffMs / 3_600_000}h ago"
        else -> "${diffMs / 86_400_000}d ago"
    }
}
