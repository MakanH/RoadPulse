package com.makan.drivelog.data.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        const val CHANNEL_ID = "drivelog_tracking"
        const val NOTIFICATION_ID = 1001
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "DriveLog Tracking",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Shown while DriveLog is tracking your drive" }
        manager.createNotificationChannel(channel)
    }

    fun buildTrackingNotification(speedKmh: Float): Notification {
        val body = if (speedKmh > 1f) "%.0f km/h".format(speedKmh) else "Waiting for movement…"
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("DriveLog is tracking your drive")
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    fun updateNotification(speedKmh: Float) =
        manager.notify(NOTIFICATION_ID, buildTrackingNotification(speedKmh))
}
