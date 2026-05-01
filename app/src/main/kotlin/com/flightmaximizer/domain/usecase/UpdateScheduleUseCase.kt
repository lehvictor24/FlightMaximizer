package com.flightmaximizer.domain.usecase

import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.domain.model.ScanSchedule
import com.flightmaximizer.worker.WorkManagerScheduler
import javax.inject.Inject

class UpdateScheduleUseCase @Inject constructor(
    private val prefs: AppPreferences,
    private val scheduler: WorkManagerScheduler
) {
    suspend operator fun invoke(schedule: ScanSchedule) {
        prefs.setScanIntervalMinutes(schedule.intervalMinutes)
        prefs.setScanEnabled(schedule.isEnabled)
        prefs.setRequireWifi(schedule.requireWifi)
        prefs.setRequireCharging(schedule.requireCharging)

        if (schedule.isEnabled) {
            scheduler.reschedule(schedule.intervalMinutes, schedule.requireWifi, schedule.requireCharging)
        } else {
            scheduler.cancel()
        }
    }
}
