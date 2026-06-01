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
        const val CHANNEL_ID           = "drivelog_tracking"
        const val SUMMARY_CHANNEL_ID   = "drivelog_summary"
        const val NOTIFICATION_ID      = 1001
        const val SUMMARY_NOTIFICATION_ID = 1002
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "DriveLog Tracking", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Shown while DriveLog is tracking your drive" }
        )
        manager.createNotificationChannel(
            NotificationChannel(SUMMARY_CHANNEL_ID, "Trip Summary", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Shown after a trip ends" }
        )
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

    fun showTripSummary(name: String, distanceKm: Double, durationMs: Long, avgSpeedKmh: Int) {
        val h = durationMs / 3_600_000; val m = (durationMs % 3_600_000) / 60_000
        val dur = if (h > 0) "${h}h ${m}m" else "${m}m"
        val body = "%.1f km · %s · %d km/h avg".format(distanceKm, dur, avgSpeedKmh)
        val notif = NotificationCompat.Builder(context, SUMMARY_CHANNEL_ID)
            .setContentTitle(name)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setAutoCancel(true)
            .build()
        manager.notify(SUMMARY_NOTIFICATION_ID, notif)
    }
}
