package com.flightmaximizer.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.domain.model.DateMode
import com.flightmaximizer.domain.model.Route
import com.flightmaximizer.domain.model.TripType
import com.flightmaximizer.domain.usecase.DeleteRouteUseCase
import com.flightmaximizer.domain.usecase.SaveRouteUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

data class RoutesUiState(
    val routes: List<Route> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val repository: FlightRepository,
    private val saveRoute: SaveRouteUseCase,
    private val deleteRoute: DeleteRouteUseCase
) : ViewModel() {

    val uiState: StateFlow<RoutesUiState> = repository.getAllRoutes()
        .map { RoutesUiState(routes = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutesUiState())

    fun onSaveRoute(
        id: String?,
        name: String,
        origins: List<String>,
        destinations: List<String>,
        tripType: TripType,
        dateMode: DateMode,
        departDate: LocalDate?,
        returnDate: LocalDate?,
        flexDays: Int,
        wheneverMonths: Int
    ) {
        viewModelScope.launch {
            val route = Route(
                id = id ?: UUID.randomUUID().toString(),
                name = name.ifBlank {
                    "${origins.joinToString(",")} → ${destinations.joinToString(",")}"
                },
                origins = origins,
                destinations = destinations,
                tripType = tripType,
                dateMode = dateMode,
                departDate = departDate,
                returnDate = returnDate,
                flexDays = flexDays,
                wheneverMonths = wheneverMonths,
                isActive = true,
                multiCityLegs = emptyList()
            )
            saveRoute(route)
        }
    }

    fun onDeleteRoute(routeId: String) = viewModelScope.launch { deleteRoute(routeId) }

    fun onToggleActive(routeId: String, active: Boolean) =
        viewModelScope.launch { repository.toggleRouteActive(routeId, active) }
}
