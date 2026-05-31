package com.makan.drivelog.domain.model

data class LocationPoint(
    val id: Long,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val timestamp: Long,
    val altitude: Double
)
