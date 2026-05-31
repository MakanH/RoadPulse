package com.makan.drivelog.di

import android.content.Context
import androidx.room.Room
import com.makan.drivelog.data.local.DriveLogDatabase
import com.makan.drivelog.data.local.dao.LocationPointDao
import com.makan.drivelog.data.local.dao.TripDao
import com.makan.drivelog.data.repository.TripRepositoryImpl
import com.makan.drivelog.domain.repository.TripRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): DriveLogDatabase =
        Room.databaseBuilder(context, DriveLogDatabase::class.java, "drivelog.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTripDao(db: DriveLogDatabase): TripDao = db.tripDao()

    @Provides
    fun provideLocationPointDao(db: DriveLogDatabase): LocationPointDao = db.locationPointDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository
}
