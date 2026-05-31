package com.makan.drivelog.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.makan.drivelog.data.local.entity.LocationPointEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationPointDao {
    @Insert
    suspend fun insert(point: LocationPointEntity): Long

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getByTripId(tripId: Long): Flow<List<LocationPointEntity>>

    @Query("SELECT * FROM location_points WHERE tripId = :tripId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForTrip(tripId: Long): LocationPointEntity?
}
