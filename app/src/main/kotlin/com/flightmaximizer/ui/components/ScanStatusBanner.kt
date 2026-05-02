package com.flightmaximizer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ScanStatusBanner(
    workInfo: WorkInfo?,
    lastScanMs: Long?,
    lastScanResultCount: Int?,
    nextScanMs: Long?,
    modifier: Modifier = Modifier
) {
    val isRunning = workInfo?.state == WorkInfo.State.RUNNING

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (isRunning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning flights…", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val noResults = lastScanMs != null && lastScanResultCount == 0
                    Icon(
                        if (noResults) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (noResults) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    val label = when {
                        lastScanMs == null -> "No scan yet"
                        lastScanResultCount == 0 ->
                            "Last scan ${formatTime(lastScanMs)} — no prices found"
                        lastScanResultCount != null ->
                            "Last scan ${formatTime(lastScanMs)} · $lastScanResultCount result${if (lastScanResultCount == 1) "" else "s"}"
                        else -> "Last scan: ${formatTime(lastScanMs)}"
                    }
                    Text(label, style = MaterialTheme.typography.bodyMedium)
                }
                if (lastScanMs != null && lastScanResultCount == 0) {
                    Text(
                        "Google Flights requires JavaScript — add a Kiwi.com API key in Settings to get real prices.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
            nextScanMs?.let {
                Text(
                    "Next scan: ${formatTime(it)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(epochMs: Long): String =
    DateTimeFormatter.ofPattern("h:mm a").withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochMilli(epochMs))
