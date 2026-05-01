package com.flightmaximizer.di

import android.content.Context
import androidx.room.Room
import com.flightmaximizer.data.local.db.FlightDatabase
import com.flightmaximizer.data.local.db.dao.FlightResultDao
import com.flightmaximizer.data.local.db.dao.PriceSnapshotDao
import com.flightmaximizer.data.local.db.dao.RouteDao
import com.flightmaximizer.data.local.db.dao.ScanRunDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FlightDatabase =
        Room.databaseBuilder(context, FlightDatabase::class.java, "flight_maximizer.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideRouteDao(db: FlightDatabase): RouteDao = db.routeDao()
    @Provides fun provideFlightResultDao(db: FlightDatabase): FlightResultDao = db.flightResultDao()
    @Provides fun provideScanRunDao(db: FlightDatabase): ScanRunDao = db.scanRunDao()
    @Provides fun providePriceSnapshotDao(db: FlightDatabase): PriceSnapshotDao = db.priceSnapshotDao()
}
