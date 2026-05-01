package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.domain.model.Route
import javax.inject.Inject

class SaveRouteUseCase @Inject constructor(private val repository: FlightRepository) {
    suspend operator fun invoke(route: Route) = repository.saveRoute(route)
}
