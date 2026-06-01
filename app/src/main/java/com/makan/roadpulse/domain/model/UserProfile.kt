package com.makan.roadpulse.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val createdAt: Long
)
