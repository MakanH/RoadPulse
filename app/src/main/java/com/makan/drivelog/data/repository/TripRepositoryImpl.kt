package com.makan.drivelog.data.repository

import com.makan.drivelog.data.local.dao.LocationPointDao
import com.makan.drivelog.data.local.dao.TripDao
import com.makan.drivelog.data.local.entity.LocationPointEntity
import com.makan.drivelog.data.local.entity.TripEntity
import com.makan.drivelog.domain.model.LocationPoint
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class TripRepositoryImpl @Inject constructor(
    private val tripDao: TripDao,
    private val locationPointDao: LocationPointDao
) : TripRepository {

    override fun getAllTrips(): Flow<List<Trip>> =
        tripDao.getAll().map { it.map(TripEntity::toDomain) }

    override suspend fun getTripById(id: Long): Trip? =
        tripDao.getById(id)?.toDomain()

    override suspend fun startTrip(name: String): Long =
        tripDao.insert(
            TripEntity(name = name, startTime = System.currentTimeMillis(),
                endTime = null, distance = 0.0, avgSpeed = 0.0, topSpeed = 0.0, isActive = true)
        )

    override suspend fun startNewTrip(): Long {
        val name = "Trip — ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date())}"
        return startTrip(name)
    }

    override suspend fun endTrip(tripId: Long) {
        val trip = tripDao.getById(tripId) ?: return
        tripDao.update(trip.copy(endTime = System.currentTimeMillis(), isActive = false))
    }

    override suspend fun deleteTrip(trip: Trip) = tripDao.delete(trip.toEntity())

    override suspend fun addLocationPoint(point: LocationPoint) {
        locationPointDao.insert(point.toEntity())
    }

    override fun getLocationPointsForTrip(tripId: Long): Flow<List<LocationPoint>> =
        locationPointDao.getByTripId(tripId).map { it.map(LocationPointEntity::toDomain) }

    override suspend fun getLatestLocationForTrip(tripId: Long): LocationPoint? =
        locationPointDao.getLatestForTrip(tripId)?.toDomain()

    override suspend fun getActiveTrip(): Trip? = tripDao.getActiveTrip()?.toDomain()

    override fun getActiveTripFlow(): Flow<Trip?> =
        tripDao.getActiveTripFlow().map { it?.toDomain() }

    override fun getTripWithPoints(tripId: Long): Flow<Pair<Trip, List<LocationPoint>>> =
        tripDao.getByIdAsFlow(tripId)
            .filterNotNull()
            .combine(locationPointDao.getByTripId(tripId)) { trip, points ->
                Pair(trip.toDomain(), points.map(LocationPointEntity::toDomain))
            }

    override suspend fun updateTripStats(tripId: Long, distance: Double, avgSpeed: Float, topSpeed: Float) =
        tripDao.updateTripStats(tripId, distance, avgSpeed.toDouble(), topSpeed.toDouble())

    override fun getTripsForMonth(startOfMonth: Long): Flow<List<Trip>> =
        tripDao.getTripsForMonth(startOfMonth).map { it.map(TripEntity::toDomain) }

    override suspend fun getPointsForTrip(tripId: Long): List<LocationPoint> =
        locationPointDao.getPointsForTrip(tripId).map(LocationPointEntity::toDomain)

    override suspend fun updateTripNames(tripId: Long, name: String, startName: String?, endName: String?) =
        tripDao.updateTripNames(tripId, name, startName, endName)

    override suspend fun deleteAllData() {
        locationPointDao.deleteAll()
        tripDao.deleteAll()
    }
}

private fun TripEntity.toDomain() = Trip(
    id = id, name = name, startTime = startTime, endTime = endTime,
    distance = distance, avgSpeed = avgSpeed, topSpeed = topSpeed, isActive = isActive,
    startName = startName, endName = endName
)

private fun Trip.toEntity() = TripEntity(
    id = id, name = name, startTime = startTime, endTime = endTime,
    distance = distance, avgSpeed = avgSpeed, topSpeed = topSpeed, isActive = isActive,
    startName = startName, endName = endName
)

private fun LocationPointEntity.toDomain() = LocationPoint(
    id = id, tripId = tripId, latitude = latitude, longitude = longitude,
    speed = speed, timestamp = timestamp, altitude = altitude
)

private fun LocationPoint.toEntity() = LocationPointEntity(
    id = id, tripId = tripId, latitude = latitude, longitude = longitude,
    speed = speed, timestamp = timestamp, altitude = altitude
)
