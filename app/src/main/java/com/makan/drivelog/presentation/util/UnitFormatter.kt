package com.makan.drivelog.presentation.util

object UnitFormatter {
    fun formatSpeed(speedKmh: Float, unit: String): String = when (unit) {
        "mph" -> "${(speedKmh * 0.621371f).toInt()} mph"
        else  -> "${speedKmh.toInt()} km/h"
    }

    fun formatDistance(distanceKm: Double, unit: String): String = when (unit) {
        "miles" -> "%.1f mi".format(distanceKm * 0.621371)
        else    -> "%.1f km".format(distanceKm)
    }
}
