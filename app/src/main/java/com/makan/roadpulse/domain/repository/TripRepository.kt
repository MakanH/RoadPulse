package com.makan.roadpulse.domain.repository

import com.makan.roadpulse.domain.model.LocationPoint
import com.makan.roadpulse.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getAllTrips(): Flow<List<Trip>>
    suspend fun getTripById(id: Long): Trip?
    suspend fun startTrip(name: String): Long
    suspend fun startNewTrip(): Long
    suspend fun endTrip(tripId: Long)
    suspend fun deleteTrip(trip: Trip)
    suspend fun addLocationPoint(point: LocationPoint)
    fun getLocationPointsForTrip(tripId: Long): Flow<List<LocationPoint>>
    suspend fun getLatestLocationForTrip(tripId: Long): LocationPoint?
    suspend fun getActiveTrip(): Trip?
    fun getActiveTripFlow(): Flow<Trip?>
    fun getTripWithPoints(tripId: Long): Flow<Pair<Trip, List<LocationPoint>>>
    suspend fun updateTripStats(tripId: Long, distance: Double, avgSpeed: Float, topSpeed: Float)
    fun getTripsForMonth(startOfMonth: Long): Flow<List<Trip>>
    suspend fun getPointsForTrip(tripId: Long): List<LocationPoint>
    suspend fun updateTripNames(tripId: Long, name: String, startName: String?, endName: String?)
    suspend fun renameTrip(tripId: Long, newName: String)
    suspend fun deleteAllData()
}
