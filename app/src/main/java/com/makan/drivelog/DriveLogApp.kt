package com.makan.drivelog

import android.app.Application
import com.makan.drivelog.data.service.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import org.maplibre.android.MapLibre

@HiltAndroidApp
class DriveLogApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MapLibre.getInstance(this)
        NotificationHelper(this).createNotificationChannel()
    }
}
