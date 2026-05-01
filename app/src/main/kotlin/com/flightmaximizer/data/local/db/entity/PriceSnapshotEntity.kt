package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "price_snapshots",
    indices = [Index(value = ["origin", "destination", "departDate", "recordedAt"])]
)
data class PriceSnapshotEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val origin: String,
    val destination: String,
    val departDate: Long,       // epoch day
    val returnDate: Long?,
    val priceCents: Int,
    val currency: String,
    val source: String,
    val recordedAt: Long        // epoch millis
)
