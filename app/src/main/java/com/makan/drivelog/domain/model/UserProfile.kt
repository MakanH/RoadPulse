package com.makan.drivelog.domain.model

data class UserProfile(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
    val createdAt: Long
)
