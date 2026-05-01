package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "multi_city_legs",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class MultiCityLegEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val legOrder: Int,
    val origin: String,
    val destination: String,
    val departDate: Long?       // epoch day
)
