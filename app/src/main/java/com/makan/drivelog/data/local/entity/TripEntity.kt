package com.makan.drivelog.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val startTime: Long,
    val endTime: Long?,
    val distance: Double,
    val avgSpeed: Double,
    val topSpeed: Double,
    val isActive: Boolean
)
