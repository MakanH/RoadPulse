package com.makan.drivelog.domain.model

data class FriendRequest(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val fromDisplayName: String,
    val fromPhotoUrl: String?,
    val status: String,
    val createdAt: Long
)
