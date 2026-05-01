package com.flightmaximizer.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import com.flightmaximizer.R
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.model.PriceDirection
import com.flightmaximizer.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.text.NumberFormat
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_SCAN_RESULTS = "channel_scan_results"
        const val CHANNEL_ERRORS = "channel_errors"
        const val NOTIFICATION_ID_SCAN = 1001
        const val NOTIFICATION_ID_ERROR = 1002
    }

    fun createChannels() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SCAN_RESULTS,
                context.getString(R.string.notification_channel_results),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Posted after every flight scan cycle" }
        )

        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ERRORS,
                context.getString(R.string.notification_channel_errors),
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Posted on persistent scan failures" }
        )
    }

    /**
     * Post a scan-summary notification.
     * [scanRunId] is used for the deep-link and as a PendingIntent request code.
     * Pass null for [scanRunId] when calling from legacy code paths that do not have a run ID.
     */
    fun postScanSummary(
        cheapestResult: FlightResult?,
        totalScanned: Int,
        routeName: String,
        scanRunId: String? = null
    ) {
        try {
            val deepLinkUri = if (scanRunId != null) {
                "flightmaximizer://home?scanRunId=$scanRunId"
            } else {
                "flightmaximizer://home"
            }

            val deepLinkIntent = Intent(
                Intent.ACTION_VIEW,
                deepLinkUri.toUri(),
                context,
                MainActivity::class.java
            ).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val requestCode = scanRunId?.hashCode() ?: 0
            val pendingIntent = PendingIntent.getActivity(
                context, requestCode, deepLinkIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = buildTitle(cheapestResult)
            val body = buildBody(cheapestResult, totalScanned, routeName)

            val builder = NotificationCompat.Builder(context, CHANNEL_SCAN_RESULTS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            cheapestResult?.bookingUrl?.let { url ->
                val bookingPi = PendingIntent.getActivity(
                    context,
                    (requestCode + 1),
                    Intent(Intent.ACTION_VIEW, url.toUri()),
                    PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(0, context.getString(R.string.notification_action_book), bookingPi)
            }
            builder.addAction(0, context.getString(R.string.notification_action_view), pendingIntent)

            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_SCAN, builder.build())
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }

    fun postScanError(routeName: String, errorMessage: String) {
        try {
            val builder = NotificationCompat.Builder(context, CHANNEL_ERRORS)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Scan failed — $routeName")
                .setContentText(errorMessage)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ERROR, builder.build())
        } catch (e: SecurityException) {
            Timber.w(e, "Notification permission not granted")
        }
    }

    private fun buildTitle(cheapestResult: FlightResult?): String {
        if (cheapestResult == null) return "No flights found this scan"

        val price = formatPrice(cheapestResult.priceCents, cheapestResult.currency)
        val deltaStr = cheapestResult.priceDelta?.let { delta ->
            when (delta.direction) {
                PriceDirection.DOWN -> " ▼ ${formatPrice(kotlin.math.abs(delta.deltaCents), cheapestResult.currency)} cheaper"
                PriceDirection.UP -> " ▲ ${formatPrice(kotlin.math.abs(delta.deltaCents), cheapestResult.currency)} more"
                PriceDirection.STABLE -> ""
            }
        } ?: ""
        return "✈ ${cheapestResult.origin} → ${cheapestResult.destination}: $price$deltaStr"
    }

    private fun buildBody(cheapestResult: FlightResult?, totalScanned: Int, routeName: String): String {
        return buildString {
            if (cheapestResult != null) {
                val airline = cheapestResult.airline ?: "Unknown airline"
                val stops = if (cheapestResult.stops == 0) "Nonstop" else "${cheapestResult.stops} stop${if (cheapestResult.stops > 1) "s" else ""}"
                val formatter = DateTimeFormatter.ofPattern("MMM d 'at' h:mm a").withZone(ZoneId.systemDefault())
                val depart = formatter.format(cheapestResult.departDateTime)
                append("$airline · $stops · $depart")
                if (cheapestResult.source.isNotBlank()) append(" · ${cheapestResult.source}")
                append("\n")
            }
            append("Scanned $totalScanned results · $routeName")
        }
    }

    private fun formatPrice(cents: Int, currency: String): String {
        return try {
            val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
                this.currency = Currency.getInstance(currency)
                maximumFractionDigits = 0
            }
            formatter.format(cents / 100.0)
        } catch (e: Exception) {
            "$${cents / 100}"
        }
    }
}
