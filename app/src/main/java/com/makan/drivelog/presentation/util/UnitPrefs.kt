package com.makan.drivelog.presentation.util

import androidx.compose.runtime.compositionLocalOf

data class UnitPrefs(val speedUnit: String = "km/h", val distanceUnit: String = "km") {

    // m/s → full formatted string with unit
    fun formatSpeed(ms: Double): String = when (speedUnit) {
        "mph" -> "${(ms * 2.237).toInt()} mph"
        else  -> "${(ms * 3.6).toInt()} km/h"
    }

    // km/h integer → full formatted string
    fun formatSpeedKmh(kmh: Int): String = when (speedUnit) {
        "mph" -> "${(kmh * 0.621).toInt()} mph"
        else  -> "$kmh km/h"
    }

    // m/s → numeric value string only (no unit suffix), for DetailStatCard
    fun speedValueMs(ms: Double): String = when (speedUnit) {
        "mph" -> "${(ms * 2.237).toInt()}"
        else  -> "${(ms * 3.6).toInt()}"
    }

    // km/h → numeric value string only
    fun speedValueKmh(kmh: Int): String = when (speedUnit) {
        "mph" -> "${(kmh * 0.621).toInt()}"
        else  -> "$kmh"
    }

    fun speedLabel(): String = speedUnit

    // meters → full formatted string
    fun formatDistanceM(m: Double): String = when (distanceUnit) {
        "miles" -> "%.1f mi".format(m / 1_609.34)
        else    -> "%.1f km".format(m / 1_000.0)
    }

    // km → full formatted string
    fun formatDistanceKm(km: Double): String = formatDistanceM(km * 1_000.0)

    // meters → numeric value only (no unit suffix)
    fun distanceValueM(m: Double): String = when (distanceUnit) {
        "miles" -> "%.1f".format(m / 1_609.34)
        else    -> "%.1f".format(m / 1_000.0)
    }

    // km → numeric value only
    fun distanceValueKm(km: Double): String = when (distanceUnit) {
        "miles" -> "%.1f".format(km * 0.621)
        else    -> "%.1f".format(km)
    }

    fun distanceLabel(): String = if (distanceUnit == "miles") "mi" else "km"

    // Multiplier for converting m/s to the display unit (used in SpeedGraph)
    fun speedFactor(): Float = if (speedUnit == "mph") 2.237f else 3.6f
}

val LocalUnitPrefs = compositionLocalOf { UnitPrefs() }
