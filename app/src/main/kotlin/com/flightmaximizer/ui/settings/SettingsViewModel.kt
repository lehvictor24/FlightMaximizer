package com.flightmaximizer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: AppPreferences,
    private val flightRepository: FlightRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = flow { emit(prefs.getAppSettings()) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            AppSettings()
        )

    fun setMinDelay(ms: Long) = viewModelScope.launch { prefs.setMinDelayMs(ms) }
    fun setMaxDelay(ms: Long) = viewModelScope.launch { prefs.setMaxDelayMs(ms) }
    fun setMaxRetries(count: Int) = viewModelScope.launch { prefs.setMaxRetries(count) }
    fun setUserAgentRotation(enabled: Boolean) =
        viewModelScope.launch { prefs.setUseRandomUserAgent(enabled) }
    fun setKiwiApiKey(key: String) = viewModelScope.launch { prefs.setKiwiApiKey(key) }
    fun setMaxHistoryDays(days: Int) = viewModelScope.launch { prefs.setMaxHistoryDays(days) }
    fun clearHistory() = viewModelScope.launch { flightRepository.pruneOldData(0) }
}
