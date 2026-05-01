package com.flightmaximizer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flightmaximizer.data.local.db.entity.MultiCityLegEntity
import com.flightmaximizer.data.local.db.entity.RouteAirportEntity
import com.flightmaximizer.data.local.db.entity.RouteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoute(route: RouteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAirports(airports: List<RouteAirportEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLegs(legs: List<MultiCityLegEntity>)

    @Query("SELECT * FROM routes WHERE isActive = 1")
    fun getActiveRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE isActive = 1")
    suspend fun getActiveRoutesList(): List<RouteEntity>

    @Query("SELECT * FROM routes")
    fun getAllRoutes(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM route_airports WHERE routeId = :routeId")
    suspend fun getAirportsForRoute(routeId: String): List<RouteAirportEntity>

    @Query("SELECT * FROM multi_city_legs WHERE routeId = :routeId ORDER BY legOrder")
    suspend fun getLegsForRoute(routeId: String): List<MultiCityLegEntity>

    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: String)

    @Query("UPDATE routes SET isActive = :active WHERE id = :routeId")
    suspend fun setActive(routeId: String, active: Int)

    @Transaction
    suspend fun getFullRoute(routeId: String): RouteEntity? {
        return getRouteById(routeId)
    }

    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: String): RouteEntity?
}
