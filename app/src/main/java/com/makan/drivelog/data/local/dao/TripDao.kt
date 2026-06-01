package com.makan.drivelog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.makan.drivelog.data.local.entity.TripEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Query("SELECT * FROM trips ORDER BY startTime DESC")
    fun getAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getByIdAsFlow(id: Long): Flow<TripEntity?>

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveTrip(): TripEntity?

    @Query("SELECT * FROM trips WHERE isActive = 1 LIMIT 1")
    fun getActiveTripFlow(): Flow<TripEntity?>

    @Query("UPDATE trips SET distance = :distance, avgSpeed = :avgSpeed, topSpeed = :topSpeed WHERE id = :tripId")
    suspend fun updateTripStats(tripId: Long, distance: Double, avgSpeed: Double, topSpeed: Double)

    @Query("SELECT * FROM trips WHERE startTime >= :startOfMonth ORDER BY startTime DESC")
    fun getTripsForMonth(startOfMonth: Long): Flow<List<TripEntity>>

    @Query("UPDATE trips SET name = :name, startName = :startName, endName = :endName WHERE id = :tripId")
    suspend fun updateTripNames(tripId: Long, name: String, startName: String?, endName: String?)

    @Query("UPDATE trips SET name = :name WHERE id = :tripId")
    suspend fun renameTrip(tripId: Long, name: String)

    @Query("DELETE FROM trips")
    suspend fun deleteAll()
}
