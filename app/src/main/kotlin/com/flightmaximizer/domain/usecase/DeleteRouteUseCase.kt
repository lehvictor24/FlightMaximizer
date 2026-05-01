package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.repository.FlightRepository
import javax.inject.Inject

class DeleteRouteUseCase @Inject constructor(private val repository: FlightRepository) {
    suspend operator fun invoke(routeId: String) = repository.deleteRoute(routeId)
}
