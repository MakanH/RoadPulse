package com.makan.roadpulse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makan.roadpulse.data.local.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert
    suspend fun insert(point: LocationPointEntity): Long

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getByTripId(tripId: Long): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForTrip(tripId: Long): LocationPointEntity?

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getPointsForTrip(tripId: Long): List<LocationPointEntity>

    @Query("DELETE FROM location_points")
    suspend fun deleteAll()
}
