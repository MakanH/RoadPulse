package com.makan.roadpulse

import android.app.Application
import com.makan.roadpulse.data.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class RoadPulseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        NotificationHelper(this).createNotificationChannel()
    }
}
