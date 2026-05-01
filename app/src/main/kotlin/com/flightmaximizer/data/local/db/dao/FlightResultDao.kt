package com.flightmaximizer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.flightmaximizer.data.local.db.entity.FlightResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlightResultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(results: List<FlightResultEntity>)

    @Query("""
        SELECT * FROM flight_results f
        INNER JOIN (
            SELECT routeId, MIN(priceCents) min_price
            FROM flight_results
            WHERE scrapedAt > :cutoffMs
            GROUP BY routeId
        ) m ON f.routeId = m.routeId AND f.priceCents = m.min_price
        ORDER BY priceCents ASC
    """)
    fun getCheapestPerRoute(cutoffMs: Long): Flow<List<FlightResultEntity>>

    @Query("""
        SELECT * FROM flight_results
        WHERE routeId = :routeId
          AND origin = :origin
          AND destination = :dest
        ORDER BY scrapedAt DESC
        LIMIT 1
    """)
    suspend fun getLatestForRoute(routeId: String, origin: String, dest: String): FlightResultEntity?

    @Query("DELETE FROM flight_results WHERE scrapedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("SELECT COUNT(*) FROM flight_results")
    fun getTotalCount(): Flow<Int>
}
