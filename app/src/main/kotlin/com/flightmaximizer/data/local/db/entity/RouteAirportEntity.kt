package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_airports",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class RouteAirportEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val iata: String,
    val role: String    // "ORIGIN" or "DESTINATION"
)
