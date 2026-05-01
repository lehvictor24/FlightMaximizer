package com.flightmaximizer.di

import com.flightmaximizer.data.local.datastore.AppPreferences
import com.flightmaximizer.data.remote.AggregatingFlightDataSource
import com.flightmaximizer.data.remote.FlightDataSource
import com.flightmaximizer.data.remote.google.GoogleFlightsDataSource
import com.flightmaximizer.data.remote.kiwi.KiwiDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataSourceModule {

    @Provides
    @Singleton
    fun provideFlightDataSource(
        googleSource: GoogleFlightsDataSource,
        kiwiSource: KiwiDataSource,
        prefs: AppPreferences
    ): FlightDataSource {
        // Read the Kiwi API key at startup to decide whether to include Kiwi in the aggregation.
        // AppPreferences emits defaults on first read so this is safe.
        val kiwiKey = runBlocking { prefs.kiwiApiKey.first() }

        val sources = buildList {
            add(googleSource)
            if (kiwiKey.isNotBlank()) add(kiwiSource)
        }
        return AggregatingFlightDataSource(sources)
    }
}
