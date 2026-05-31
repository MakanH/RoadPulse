package com.makan.drivelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.makan.drivelog.data.local.dao.LocationPointDao
import com.makan.drivelog.data.local.dao.TripDao
import com.makan.drivelog.data.local.entity.LocationPointEntity
import com.makan.drivelog.data.local.entity.TripEntity

@Database(
    entities = [TripEntity::class, LocationPointEntity::class],
    version = 2,
    exportSchema = false
)
abstract class DriveLogDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun locationPointDao(): LocationPointDao
}
