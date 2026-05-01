package com.flightmaximizer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightmaximizer.BuildConfig
import com.flightmaximizer.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    var kiwiKey by remember(settings.kiwiApiKey) { mutableStateOf(settings.kiwiApiKey) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // --- Anti-Blocking ---
            Text(
                "Anti-Blocking",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column {
                    ListItem(
                        headlineContent = { Text("Rotate User-Agents") },
                        supportingContent = { Text("Vary browser identity per request") },
                        trailingContent = {
                            Switch(
                                checked = settings.useRandomUserAgent,
                                onCheckedChange = { viewModel.setUserAgentRotation(it) }
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Min delay: ${settings.minDelayMs} ms") },
                        supportingContent = {
                            Slider(
                                value = settings.minDelayMs.toFloat(),
                                onValueChange = { viewModel.setMinDelay(it.toLong()) },
                                valueRange = 500f..10000f,
                                steps = 19
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Max delay: ${settings.maxDelayMs} ms") },
                        supportingContent = {
                            Slider(
                                value = settings.maxDelayMs.toFloat(),
                                onValueChange = { viewModel.setMaxDelay(it.toLong()) },
                                valueRange = 1000f..30000f,
                                steps = 29
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Max retries: ${settings.maxRetries}") },
                        supportingContent = {
                            Slider(
                                value = settings.maxRetries.toFloat(),
                                onValueChange = { viewModel.setMaxRetries(it.toInt()) },
                                valueRange = 1f..5f,
                                steps = 3
                            )
                        }
                    )
                }
            }

            // --- Data Sources ---
            Text(
                "Data Sources",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.lbl_source_google),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Always enabled · No API key needed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        stringResource(R.string.lbl_source_kiwi),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = kiwiKey,
                        onValueChange = { kiwiKey = it },
                        label = { Text("Kiwi.com API key (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            TextButton(onClick = { viewModel.setKiwiApiKey(kiwiKey) }) {
                                Text("Save")
                            }
                        }
                    )
                }
            }

            // --- Data ---
            Text(
                "Data",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                Column {
                    ListItem(
                        headlineContent = {
                            Text("Keep history for ${settings.maxHistoryDays} days")
                        },
                        supportingContent = {
                            Slider(
                                value = settings.maxHistoryDays.toFloat(),
                                onValueChange = { viewModel.setMaxHistoryDays(it.toInt()) },
                                valueRange = 7f..90f,
                                steps = 10
                            )
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Clear all results") },
                        trailingContent = {
                            OutlinedButton(onClick = { showClearDialog = true }) {
                                Text("Clear")
                            }
                        }
                    )
                }
            }

            // --- About ---
            Text(
                "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Card {
                ListItem(
                    headlineContent = { Text("FlightMaximizer") },
                    supportingContent = { Text("Version ${BuildConfig.VERSION_NAME}") }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all results?") },
            text = { Text("This will delete all flight scan history. This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }
                ) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}
