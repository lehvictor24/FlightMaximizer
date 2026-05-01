package com.flightmaximizer.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "scan_runs",
    foreignKeys = [ForeignKey(
        entity = RouteEntity::class,
        parentColumns = ["id"],
        childColumns = ["routeId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("routeId")]
)
data class ScanRunEntity(
    @PrimaryKey val id: String,
    val routeId: String,
    val startedAt: Long,
    val finishedAt: Long,
    val status: String,         // ScanStatus.name()
    val errorMessage: String?,
    val resultsCount: Int
)
