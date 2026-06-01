package com.makan.drivelog.domain.model

data class FriendTripStat(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val tripId: String,
    val topSpeed: Double,
    val avgSpeed: Double,
    val distanceKm: Double,
    val durationSeconds: Long,
    val startName: String?,
    val endName: String?,
    val startTime: Long
)
