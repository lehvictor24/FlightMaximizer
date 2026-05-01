package com.flightmaximizer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.flightmaximizer.data.local.db.entity.ScanRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRunDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(run: ScanRunEntity)

    @Update
    suspend fun update(run: ScanRunEntity)

    @Query("SELECT * FROM scan_runs ORDER BY startedAt DESC LIMIT 1")
    fun getLatest(): Flow<ScanRunEntity?>

    @Query("DELETE FROM scan_runs WHERE startedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)
}
