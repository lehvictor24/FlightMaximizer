package com.flightmaximizer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flightmaximizer.data.local.db.entity.PriceSnapshotEntity

@Dao
interface PriceSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: PriceSnapshotEntity)

    @Query("""
        SELECT * FROM price_snapshots
        WHERE origin = :origin
          AND destination = :dest
          AND departDate = :departDate
        ORDER BY recordedAt DESC
        LIMIT :limit
    """)
    suspend fun getSnapshots(
        origin: String,
        dest: String,
        departDate: Long,
        limit: Int = 30
    ): List<PriceSnapshotEntity>

    @Query("""
        SELECT * FROM price_snapshots
        WHERE origin = :origin
          AND destination = :dest
        ORDER BY recordedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestSnapshot(origin: String, dest: String): PriceSnapshotEntity?

    @Query("DELETE FROM price_snapshots WHERE recordedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
