package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "flight_results",
    foreignKeys = [ForeignKey(
        entity = ScanRunEntity::class,
        parentColumns = ["id"],
        childColumns = ["scanRunId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("scanRunId"),
        Index(value = ["routeId", "priceCents"]),
        Index("scrapedAt")
    ]
)
data class FlightResultEntity(
    @PrimaryKey val id: String,
    val scanRunId: String,
    val routeId: String,
    val origin: String,
    val destination: String,
    val priceCents: Int,
    val currency: String,
    val airline: String?,
    val flightNumber: String?,
    val departDateTime: Long,   // epoch millis
    val arriveDateTime: Long,
    val returnDateTime: Long?,
    val durationMinutes: Int?,
    val stops: Int,
    val bookingUrl: String?,
    val tripType: String,
    val source: String,         // "google_flights" or "kiwi"
    val scrapedAt: Long,        // epoch millis
    val priceDeltaCents: Int?,  // vs previous scan
    val priceDirection: String? // PriceDirection.name() or null
)
