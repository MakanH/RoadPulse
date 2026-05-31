package com.makan.drivelog.data.service

import javax.inject.Inject
import javax.inject.Singleton

enum class SpeedResult { StartCooldown, CancelCooldown, NoAction }

@Singleton
class TripManager @Inject constructor() {

    enum class Phase { IDLE, TRACKING, ENDING }

    companion object {
        const val START_SPEED_MS = 2.2f    // ~8 km/h
        const val STOP_SPEED_MS = 1.4f     // ~5 km/h
        const val STOP_COOLDOWN_MS = 120_000L
    }

    var phase: Phase = Phase.IDLE
        private set
    var currentTripId: Long? = null
        private set

    private val speedSamples = mutableListOf<Float>()
    private var topSpeed = 0f

    fun shouldStartTrip(speed: Float) = phase == Phase.IDLE && speed >= START_SPEED_MS

    fun onTripStarted(tripId: Long) {
        phase = Phase.TRACKING
        currentTripId = tripId
        speedSamples.clear()
        topSpeed = 0f
    }

    fun onSpeedUpdate(speed: Float): SpeedResult {
        if (speed > topSpeed) topSpeed = speed
        speedSamples.add(speed)
        return when (phase) {
            Phase.IDLE -> SpeedResult.NoAction
            Phase.TRACKING -> {
                if (speed < STOP_SPEED_MS) {
                    phase = Phase.ENDING
                    SpeedResult.StartCooldown
                } else {
                    SpeedResult.NoAction
                }
            }
            Phase.ENDING -> {
                if (speed >= START_SPEED_MS) {
                    phase = Phase.TRACKING
                    SpeedResult.CancelCooldown
                } else {
                    SpeedResult.NoAction
                }
            }
        }
    }

    fun onTripEnded() {
        phase = Phase.IDLE
        currentTripId = null
        speedSamples.clear()
        topSpeed = 0f
    }

    fun averageSpeed(): Float = if (speedSamples.isEmpty()) 0f else speedSamples.average().toFloat()
    fun peakSpeed(): Float = topSpeed

    fun reset() = onTripEnded()
}
