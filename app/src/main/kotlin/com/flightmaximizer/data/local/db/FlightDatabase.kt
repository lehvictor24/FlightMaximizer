package com.flightmaximizer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.flightmaximizer.data.local.db.converter.DateConverter
import com.flightmaximizer.data.local.db.converter.EnumConverters
import com.flightmaximizer.data.local.db.dao.FlightResultDao
import com.flightmaximizer.data.local.db.dao.PriceSnapshotDao
import com.flightmaximizer.data.local.db.dao.RouteDao
import com.flightmaximizer.data.local.db.dao.ScanRunDao
import com.flightmaximizer.data.local.db.entity.FlightResultEntity
import com.flightmaximizer.data.local.db.entity.MultiCityLegEntity
import com.flightmaximizer.data.local.db.entity.PriceSnapshotEntity
import com.flightmaximizer.data.local.db.entity.RouteAirportEntity
import com.flightmaximizer.data.local.db.entity.RouteEntity
import com.flightmaximizer.data.local.db.entity.ScanRunEntity

@Database(
    entities = [
        RouteEntity::class,
        RouteAirportEntity::class,
        MultiCityLegEntity::class,
        ScanRunEntity::class,
        FlightResultEntity::class,
        PriceSnapshotEntity::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(DateConverter::class, EnumConverters::class)
abstract class FlightDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun flightResultDao(): FlightResultDao
    abstract fun scanRunDao(): ScanRunDao
    abstract fun priceSnapshotDao(): PriceSnapshotDao
}
