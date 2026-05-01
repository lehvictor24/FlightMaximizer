package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(
    @PrimaryKey val id: String,
    val name: String,
    val tripType: String,       // TripType.name()
    val dateMode: String,       // DateMode.name()
    val departDate: Long?,      // epoch day (LocalDate.toEpochDay())
    val returnDate: Long?,
    val flexDays: Int,
    val wheneverMonths: Int,
    val isActive: Int,          // 1 = active, 0 = paused
    val createdAt: Long         // epoch millis
)
