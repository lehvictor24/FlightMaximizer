package com.flightmaximizer.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightmaximizer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val schedule by viewModel.schedule.collectAsStateWithLifecycle()
    val intervalOptions = listOf(15, 30, 60, 360, 720, 1440)
    var customMinutes by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_schedule)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enable scanning toggle
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable scanning", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Run in background automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = schedule.isEnabled,
                        onCheckedChange = { viewModel.onUpdateSchedule(schedule.copy(isEnabled = it)) }
                    )
                }
            }

            // Interval selection
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        stringResource(R.string.lbl_interval),
                        style = MaterialTheme.typography.titleMedium
                    )
                    val labels = mapOf(
                        15 to "15 min",
                        30 to "30 min",
                        60 to "1 hour",
                        360 to "6 hours",
                        720 to "12 hours",
                        1440 to "24 hours"
                    )
                    intervalOptions.forEach { option ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = schedule.intervalMinutes == option,
                                onClick = {
                                    viewModel.onUpdateSchedule(schedule.copy(intervalMinutes = option))
                                }
                            )
                            Text(
                                labels[option] ?: "$option min",
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                    // Custom interval row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RadioButton(
                            selected = schedule.intervalMinutes !in intervalOptions,
                            onClick = {
                                customMinutes.toIntOrNull()?.let { m ->
                                    viewModel.onUpdateSchedule(
                                        schedule.copy(intervalMinutes = m.coerceAtLeast(15))
                                    )
                                }
                            }
                        )
                        OutlinedTextField(
                            value = customMinutes,
                            onValueChange = { customMinutes = it },
                            label = { Text("Custom (min, ≥15)") },
                            modifier = Modifier.width(200.dp),
                            singleLine = true
                        )
                    }
                }
            }

            // Network constraints
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Network constraints", style = MaterialTheme.typography.titleMedium)
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("Wi-Fi only")
                        Switch(
                            checked = schedule.requireWifi,
                            onCheckedChange = {
                                viewModel.onUpdateSchedule(schedule.copy(requireWifi = it))
                            }
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        Arrangement.SpaceBetween,
                        Alignment.CenterVertically
                    ) {
                        Text("Require charging")
                        Switch(
                            checked = schedule.requireCharging,
                            onCheckedChange = {
                                viewModel.onUpdateSchedule(schedule.copy(requireCharging = it))
                            }
                        )
                    }
                }
            }

            // Run now button
            Button(
                onClick = viewModel::onScanNow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_scan_now))
            }
        }
    }
}
