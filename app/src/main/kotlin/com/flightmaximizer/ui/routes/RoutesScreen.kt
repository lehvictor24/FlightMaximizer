package com.flightmaximizer.ui.routes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.flightmaximizer.R
import com.flightmaximizer.domain.model.DateMode
import com.flightmaximizer.domain.model.Route
import com.flightmaximizer.domain.model.TripType
import com.flightmaximizer.ui.components.AirportChip
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutesScreen(viewModel: RoutesViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }
    var editingRoute by remember { mutableStateOf<Route?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_routes)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { editingRoute = null; showAddSheet = true }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.btn_add_route))
            }
        }
    ) { innerPadding ->
        if (state.routes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "No routes yet.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { editingRoute = null; showAddSheet = true }) {
                        Text(stringResource(R.string.btn_add_route))
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(innerPadding)
            ) {
                items(state.routes, key = { it.id }) { route ->
                    RouteCard(
                        route = route,
                        onEdit = { editingRoute = route; showAddSheet = true },
                        onDelete = { viewModel.onDeleteRoute(route.id) },
                        onToggle = { active -> viewModel.onToggleActive(route.id, active) }
                    )
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showAddSheet) {
        RouteEditSheet(
            existingRoute = editingRoute,
            onSave = { name, origins, dests, tripType, dateMode, depart, ret, flexDays, wheneverMonths ->
                viewModel.onSaveRoute(
                    id = editingRoute?.id,
                    name = name,
                    origins = origins,
                    destinations = dests,
                    tripType = tripType,
                    dateMode = dateMode,
                    departDate = depart,
                    returnDate = ret,
                    flexDays = flexDays,
                    wheneverMonths = wheneverMonths
                )
                showAddSheet = false
            },
            onDismiss = { showAddSheet = false }
        )
    }
}

@Composable
private fun RouteCard(
    route: Route,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    route.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = route.isActive, onCheckedChange = onToggle)
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.btn_delete),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "From: ${route.origins.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                "To: ${route.destinations.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(route.tripType.name.replace("_", " ")) }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(route.dateMode.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RouteEditSheet(
    existingRoute: Route?,
    onSave: (String, List<String>, List<String>, TripType, DateMode, LocalDate?, LocalDate?, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existingRoute?.name ?: "") }
    var originsText by remember { mutableStateOf(existingRoute?.origins?.joinToString(", ") ?: "") }
    var destsText by remember { mutableStateOf(existingRoute?.destinations?.joinToString(", ") ?: "") }
    var tripType by remember { mutableStateOf(existingRoute?.tripType ?: TripType.ROUND_TRIP) }
    var dateMode by remember { mutableStateOf(existingRoute?.dateMode ?: DateMode.WHENEVER) }
    var flexDays by remember { mutableStateOf(existingRoute?.flexDays ?: 7) }
    var wheneverMonths by remember { mutableStateOf(existingRoute?.wheneverMonths ?: 3) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (existingRoute == null) "Add Route" else "Edit Route",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Route name (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = originsText,
                onValueChange = { originsText = it },
                label = { Text("Origins — comma-separated IATA codes (e.g. JFK, EWR)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = destsText,
                onValueChange = { destsText = it },
                label = { Text("Destinations — comma-separated IATA codes (e.g. LHR, CDG)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Trip type
            Text(stringResource(R.string.lbl_trip_type), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TripType.values().forEach { type ->
                    FilterChip(
                        selected = tripType == type,
                        onClick = { tripType = type },
                        label = { Text(type.name.replace("_", " ")) }
                    )
                }
            }

            // Date mode
            Text(stringResource(R.string.lbl_date_mode), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DateMode.values().forEach { mode ->
                    FilterChip(
                        selected = dateMode == mode,
                        onClick = { dateMode = mode },
                        label = { Text(mode.name) }
                    )
                }
            }

            when (dateMode) {
                DateMode.FLEXIBLE -> {
                    Text(
                        "Search window: ±$flexDays days",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = flexDays.toFloat(),
                        onValueChange = { flexDays = it.toInt() },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }
                DateMode.WHENEVER -> {
                    Text(
                        "Scan next $wheneverMonths months",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = wheneverMonths.toFloat(),
                        onValueChange = { wheneverMonths = it.toInt() },
                        valueRange = 1f..6f,
                        steps = 4
                    )
                }
                DateMode.FIXED -> { /* No extra controls needed for fixed */ }
            }

            // Preview
            val origins = originsText.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() }
            val dests = destsText.split(",")
                .map { it.trim().uppercase() }
                .filter { it.isNotBlank() }
            if (origins.isNotEmpty() && dests.isNotEmpty()) {
                val combos = origins.flatMap { o -> dests.map { d -> "$o→$d" } }
                Text(
                    text = "${combos.size} route(s): ${combos.take(6).joinToString(", ")}${if (combos.size > 6) "…" else ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_cancel)) }
                Button(
                    onClick = {
                        val parsedOrigins = originsText.split(",")
                            .map { it.trim().uppercase() }
                            .filter { it.isNotBlank() }
                        val parsedDests = destsText.split(",")
                            .map { it.trim().uppercase() }
                            .filter { it.isNotBlank() }
                        if (parsedOrigins.isEmpty() || parsedDests.isEmpty()) return@Button
                        onSave(
                            name, parsedOrigins, parsedDests,
                            tripType, dateMode,
                            null, null,
                            flexDays, wheneverMonths
                        )
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.btn_save)) }
            }
        }
    }
}
