package com.makan.drivelog.data.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.makan.drivelog.domain.model.LocationPoint
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var repository: TripRepository
    @Inject lateinit var tripManager: TripManager
    @Inject lateinit var notificationHelper: NotificationHelper

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locationMutex = Mutex()

    private var cooldownJob: Job? = null
    private var totalDistance = 0.0
    private var lastLocation: Location? = null

    companion object {
        const val ACTION_START = "com.makan.drivelog.action.START_TRACKING"
        const val ACTION_STOP  = "com.makan.drivelog.action.STOP_TRACKING"

        private val _state = MutableStateFlow(TrackingState())
        val state: StateFlow<TrackingState> = _state.asStateFlow()

        fun startIntent(context: Context) =
            Intent(context, LocationTrackingService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, LocationTrackingService::class.java).apply { action = ACTION_STOP }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val speedKmh = location.speed * 3.6f
            notificationHelper.updateNotification(speedKmh)
            serviceScope.launch {
                locationMutex.withLock { handleLocation(location, location.speed) }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationHelper.createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> beginTracking()
            ACTION_STOP  -> endTracking()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun beginTracking() {
        val notification = notificationHelper.buildTrackingNotification(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3_000L)
            .setMinUpdateIntervalMillis(1_000L)
            .build()
        try {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            stopSelf()
        }
    }

    private fun endTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.launch {
            locationMutex.withLock {
                tripManager.currentTripId?.let { finishTrip(it) }
                tripManager.reset()
                totalDistance = 0.0
                lastLocation = null
            }
            _state.value = TrackingState()
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private suspend fun handleLocation(location: Location, speed: Float) {
        when {
            tripManager.shouldStartTrip(speed) -> {
                val tripId = repository.startNewTrip()
                tripManager.onTripStarted(tripId)
                totalDistance = 0.0
                lastLocation = location
                persistPoint(location, tripId)
                emitState(speed, tripId)
            }
            tripManager.phase != TripManager.Phase.IDLE -> {
                val tripId = tripManager.currentTripId ?: return
                lastLocation?.let { prev ->
                    val dist = prev.distanceTo(location).toDouble()
                    if (dist < 200.0) totalDistance += dist
                }
                lastLocation = location

                when (tripManager.onSpeedUpdate(speed)) {
                    SpeedResult.StartCooldown  -> scheduleCooldown(tripId)
                    SpeedResult.CancelCooldown -> { cooldownJob?.cancel(); cooldownJob = null }
                    SpeedResult.NoAction       -> Unit
                }
                persistPoint(location, tripId)
                emitState(speed, tripId)
            }
        }
    }

    private fun scheduleCooldown(tripId: Long) {
        cooldownJob?.cancel()
        cooldownJob = serviceScope.launch {
            delay(TripManager.STOP_COOLDOWN_MS)
            locationMutex.withLock {
                if (tripManager.phase == TripManager.Phase.ENDING) {
                    finishTrip(tripId)
                    tripManager.onTripEnded()
                    totalDistance = 0.0
                    lastLocation = null
                    cooldownJob = null
                    _state.value = TrackingState()
                }
            }
        }
    }

    private suspend fun persistPoint(location: Location, tripId: Long) {
        repository.addLocationPoint(
            LocationPoint(
                id = 0,
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                speed = location.speed.toDouble(),
                timestamp = System.currentTimeMillis(),
                altitude = location.altitude
            )
        )
        repository.updateTripStats(
            tripId = tripId,
            distance = totalDistance,
            avgSpeed = tripManager.averageSpeed(),
            topSpeed = tripManager.peakSpeed()
        )
    }

    private suspend fun finishTrip(tripId: Long) {
        repository.updateTripStats(tripId, totalDistance, tripManager.averageSpeed(), tripManager.peakSpeed())
        repository.endTrip(tripId)
    }

    private suspend fun emitState(speed: Float, tripId: Long) {
        _state.value = TrackingState(
            isTracking = tripManager.phase != TripManager.Phase.IDLE,
            currentTrip = repository.getTripById(tripId),
            currentSpeed = speed,
            tripDistance = totalDistance
        )
    }

    override fun onDestroy() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        serviceScope.cancel()
        super.onDestroy()
    }
}
