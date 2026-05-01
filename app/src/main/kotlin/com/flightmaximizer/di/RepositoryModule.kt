package com.flightmaximizer.di

import com.flightmaximizer.data.repository.FlightRepository
import com.flightmaximizer.data.repository.FlightRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFlightRepository(impl: FlightRepositoryImpl): FlightRepository
}
