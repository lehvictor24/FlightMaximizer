package com.flightmaximizer.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.domain.model.ScanSchedule
import com.flightmaximizer.domain.usecase.UpdateScheduleUseCase
import com.flightmaximizer.worker.WorkManagerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val updateSchedule: UpdateScheduleUseCase,
    private val scheduler: WorkManagerScheduler
) : ViewModel() {

    val schedule: StateFlow<ScanSchedule> = combine(
        prefs.scanIntervalMinutes,
        prefs.scanEnabled,
        prefs.requireWifi,
        prefs.requireCharging
    ) { intervalMinutes, isEnabled, requireWifi, requireCharging ->
        ScanSchedule(
            intervalMinutes = intervalMinutes,
            isEnabled = isEnabled,
            requireWifi = requireWifi,
            requireCharging = requireCharging
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ScanSchedule(
            intervalMinutes = 60,
            isEnabled = false,
            requireWifi = false,
            requireCharging = false
        )
    )

    fun onUpdateSchedule(newSchedule: ScanSchedule) {
        viewModelScope.launch { updateSchedule(newSchedule) }
    }

    fun onScanNow() = scheduler.triggerNow()
}
