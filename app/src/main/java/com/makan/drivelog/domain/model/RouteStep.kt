package com.makan.drivelog.domain.model

data class RouteStep(
    val instruction: String,
    val distanceMeters: Double,
    val durationSeconds: Double,
    val type: Int,
    val startPointIndex: Int,
    val endPointIndex: Int
)
