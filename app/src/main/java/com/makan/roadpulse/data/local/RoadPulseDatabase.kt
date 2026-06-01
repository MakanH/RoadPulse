package com.makan.roadpulse.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.makan.roadpulse.data.local.dao.LocationPointDao
import com.makan.roadpulse.data.local.dao.TripDao
import com.makan.roadpulse.data.local.entity.LocationPointEntity
import com.makan.roadpulse.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class RoadPulseDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationPointDao(): LocationPointDao
}
