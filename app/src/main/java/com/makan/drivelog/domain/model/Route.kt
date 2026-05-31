package com.makan.drivelog.domain.model

data class Route(
    val geometry: List<LatLng>,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val steps: List<RouteStep>
)
