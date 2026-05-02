package com.flightmaximizer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import com.flightmaximizer.domain.model.FlightResult
import com.flightmaximizer.domain.usecase.GetCheapestFlightsUseCase
import com.flightmaximizer.worker.WorkManagerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val cheapestPerRoute: List<FlightResult> = emptyList(),
    val workInfo: WorkInfo? = null,
    val lastScanMs: Long? = null,
    val lastScanResultCount: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getCheapestFlights: GetCheapestFlightsUseCase,
    private val scheduler: WorkManagerScheduler
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        getCheapestFlights(),
        scheduler.getWorkInfo()
    ) { results, workInfo ->
        val output = workInfo?.outputData
        HomeUiState(
            cheapestPerRoute = results,
            workInfo = workInfo,
            lastScanMs = output?.getLong("last_scan_ms", 0L)?.takeIf { it > 0L },
            lastScanResultCount = output?.getInt("results_count", -1)?.takeIf { it >= 0 }
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUiState()
    )

    fun onScanNowClicked() {
        scheduler.triggerNow()
    }
}
