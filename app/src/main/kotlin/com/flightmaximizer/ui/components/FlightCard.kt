package com.flightmaximizer.ui.components

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.model.PricePrediction
import com.flightmaximizer.domain.model.PriceDirection
import com.flightmaximizer.domain.model.Trend
import com.flightmaximizer.ui.theme.PriceDownGreen
import com.flightmaximizer.ui.theme.PriceStableGray
import com.flightmaximizer.ui.theme.PriceUpRed
import com.flightmaximizer.ui.theme.SourceGoogleBlue
import com.flightmaximizer.ui.theme.SourceKiwiOrange
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun FlightCard(
    result: FlightResult,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "${result.origin} → ${result.destination}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatPrice(result.priceCents, result.currency),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(4.dp))

            val stops = if (result.stops == 0) "Nonstop" else "${result.stops} stop(s)"
            val airline = result.airline ?: "Unknown"
            val departFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a")
            val depart = result.departDateTime.format(departFormatter)
            Text(
                text = "$airline · $stops · $depart",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SourceBadge(result.source)

                result.priceDelta?.let { delta ->
                    val deltaAbs = formatPrice(kotlin.math.abs(delta.deltaCents), result.currency)
                    val (color, text) = when (delta.direction) {
                        PriceDirection.DOWN -> PriceDownGreen to "▼ $deltaAbs cheaper"
                        PriceDirection.UP -> PriceUpRed to "▲ $deltaAbs more"
                        PriceDirection.STABLE -> PriceStableGray to "→ Stable"
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(text, fontSize = 11.sp, color = color) }
                    )
                }

                (result.prediction as? PricePrediction.Available)?.let { pred ->
                    val (icon, color) = when (pred.trend) {
                        Trend.FALLING -> Icons.Filled.TrendingDown to PriceDownGreen
                        Trend.RISING -> Icons.Filled.TrendingUp to PriceUpRed
                        Trend.FLAT -> Icons.Filled.TrendingFlat to PriceStableGray
                    }
                    SuggestionChip(
                        onClick = {},
                        icon = {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(pred.suggestedAction, fontSize = 11.sp) }
                    )
                }
            }

            result.bookingUrl?.let { url ->
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Filled.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("View Deal")
                }
            }
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val color = when {
        source.contains("google", ignoreCase = true) -> SourceGoogleBlue
        source.contains("kiwi", ignoreCase = true) -> SourceKiwiOrange
        else -> MaterialTheme.colorScheme.outline
    }
    val label = when {
        source.contains("google", ignoreCase = true) -> "G"
        source.contains("kiwi", ignoreCase = true) -> "K"
        else -> source.take(1).uppercase()
    }
    SuggestionChip(
        onClick = {},
        label = { Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold) }
    )
}

private fun formatPrice(cents: Int, currency: String): String {
    val major = cents / 100
    val minor = cents % 100
    val symbol = when (currency.uppercase(Locale.ROOT)) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> "$currency "
    }
    return if (minor == 0) "$symbol$major" else "$symbol$major.%02d".format(minor)
}
