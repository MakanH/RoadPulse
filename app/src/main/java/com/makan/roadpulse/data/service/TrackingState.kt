package com.makan.roadpulse.data.service

import com.makan.roadpulse.domain.model.LocationPoint
import com.makan.roadpulse.domain.model.Trip

data class TrackingState(
    val isTracking: Boolean = false,
    val currentTrip: Trip? = null,
    val currentSpeed: Float = 0f,
    val currentLocation: LocationPoint? = null,
    val tripDistance: Double = 0.0
)
