package com.makan.drivelog.domain.model

data class Trip(
    val id: Long,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val distance: Double,
    val avgSpeed: Double,
    val topSpeed: Double,
    val isActive: Boolean,
    val startName: String? = null,
    val endName: String? = null
)
