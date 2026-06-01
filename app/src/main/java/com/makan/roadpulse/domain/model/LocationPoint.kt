package com.makan.roadpulse.domain.model

data class LocationPoint(
    val id: Long,
    val tripId: Long,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val timestamp: Long,
    val altitude: Double
)
