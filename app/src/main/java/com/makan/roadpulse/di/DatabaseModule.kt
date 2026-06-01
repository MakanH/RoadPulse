package com.makan.roadpulse.di

import android.content.Context
import androidx.room.Room
import com.makan.roadpulse.data.local.RoadPulseDatabase
import com.makan.roadpulse.data.local.dao.LocationPointDao
import com.makan.roadpulse.data.local.dao.TripDao
import com.makan.roadpulse.data.repository.TripRepositoryImpl
import com.makan.roadpulse.domain.repository.TripRepository
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
    fun provideDatabase(@ApplicationContext context: Context): RoadPulseDatabase =
        Room.databaseBuilder(context, RoadPulseDatabase::class.java, "roadpulse_database")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideTripDao(db: RoadPulseDatabase): TripDao = db.tripDao()

    @Provides
    fun provideLocationPointDao(db: RoadPulseDatabase): LocationPointDao = db.locationPointDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTripRepository(impl: TripRepositoryImpl): TripRepository
}
