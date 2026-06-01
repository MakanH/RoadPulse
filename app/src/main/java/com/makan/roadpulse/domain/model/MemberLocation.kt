package com.makan.roadpulse.domain.model

data class MemberLocation(
    val uid: String,
    val displayName: String,
    val photoUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val bearing: Float,
    val timestamp: Long,
    val isActive: Boolean
)
