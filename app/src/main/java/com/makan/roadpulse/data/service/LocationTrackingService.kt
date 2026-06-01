package com.makan.roadpulse.data.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import com.makan.roadpulse.data.local.PreferencesManager
import com.makan.roadpulse.domain.model.LocationPoint
import com.makan.roadpulse.domain.repository.TripRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject lateinit var repository: TripRepository
    @Inject lateinit var tripManager: TripManager
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var geocodingService: NominatimGeocodingService
    @Inject lateinit var groupService: GroupService
    @Inject lateinit var tripSyncService: TripSyncService
    @Inject lateinit var prefs: PreferencesManager

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val locationMutex = Mutex()

    private var cooldownJob: Job? = null
    private var totalDistance = 0.0
    private var lastLocation: Location? = null

    // Cached preference values — updated via ongoing collection
    @Volatile private var cachedShareLiveLocation = true
    @Volatile private var cachedGpsAccuracy = "high"

    companion object {
        const val ACTION_START = "com.makan.roadpulse.action.START_TRACKING"
        const val ACTION_STOP  = "com.makan.roadpulse.action.STOP_TRACKING"

        private val _state = MutableStateFlow(TrackingState())
        val state: StateFlow<TrackingState> = _state.asStateFlow()

        @Volatile var broadcastGroupId: String? = null
            private set
        @Volatile var broadcastSessionId: String? = null
            private set

        fun startGroupBroadcast(groupId: String, sessionId: String) {
            broadcastGroupId = groupId
            broadcastSessionId = sessionId
        }

        fun stopGroupBroadcast() {
            broadcastGroupId = null
            broadcastSessionId = null
        }

        fun startIntent(context: Context) =
            Intent(context, LocationTrackingService::class.java).apply { action = ACTION_START }

        fun stopIntent(context: Context) =
            Intent(context, LocationTrackingService::class.java).apply { action = ACTION_STOP }
    }

    private fun hasLocationPermission() =
        checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!hasLocationPermission()) { endTracking(); return }
            val location = result.lastLocation ?: return
            val speedKmh = location.speed * 3.6f
            notificationHelper.updateNotification(speedKmh)
            serviceScope.launch {
                locationMutex.withLock { handleLocation(location, location.speed) }
            }
            // Group broadcast (respects share live location preference)
            val gid = broadcastGroupId
            val sid = broadcastSessionId
            if (gid != null && sid != null && cachedShareLiveLocation) {
                serviceScope.launch {
                    val pt = LocationPoint(
                        id = 0L, tripId = 0L,
                        latitude  = location.latitude,
                        longitude = location.longitude,
                        speed     = location.speed.toDouble(),
                        timestamp = System.currentTimeMillis(),
                        altitude  = location.altitude
                    )
                    groupService.pushMyLocation(gid, sid, pt, location.bearing)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationHelper.createNotificationChannel()
        // Keep preference caches live
        serviceScope.launch { prefs.shareLiveLocation.collect { cachedShareLiveLocation = it } }
        serviceScope.launch { prefs.gpsAccuracy.collect { cachedGpsAccuracy = it } }
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
        val priority = when (cachedGpsAccuracy) {
            "balanced" -> Priority.PRIORITY_BALANCED_POWER_ACCURACY
            else       -> Priority.PRIORITY_HIGH_ACCURACY
        }
        val request = LocationRequest.Builder(priority, 3_000L)
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
                id = 0, tripId = tripId,
                latitude = location.latitude, longitude = location.longitude,
                speed = location.speed.toDouble(), timestamp = System.currentTimeMillis(),
                altitude = location.altitude
            )
        )
        repository.updateTripStats(
            tripId   = tripId,
            distance = totalDistance,
            avgSpeed = tripManager.averageSpeed(),
            topSpeed = tripManager.peakSpeed()
        )
    }

    private suspend fun finishTrip(tripId: Long) {
        repository.updateTripStats(tripId, totalDistance, tripManager.averageSpeed(), tripManager.peakSpeed())
        repository.endTrip(tripId)

        // Min trip distance check — discard short trips
        val minDist = parseMinTripDistance(prefs.minTripDistance.first())
        if (totalDistance < minDist) {
            repository.getTripById(tripId)?.let { repository.deleteTrip(it) }
            return
        }

        geocodeAndNameTrip(tripId)
        val trip = repository.getTripById(tripId) ?: return
        tripSyncService.syncTripToFirestore(trip)

        // Trip summary notification
        if (prefs.tripSummaryNotification.first()) {
            val durationMs = (trip.endTime ?: trip.startTime) - trip.startTime
            val avgKmh = (trip.avgSpeed * 3.6).toInt()
            notificationHelper.showTripSummary(
                name        = trip.name,
                distanceKm  = trip.distance / 1_000.0,
                durationMs  = durationMs,
                avgSpeedKmh = avgKmh
            )
        }
    }

    private fun parseMinTripDistance(pref: String): Double = when (pref) {
        "100m" -> 100.0
        "1 km" -> 1_000.0
        else   -> 500.0
    }

    private suspend fun geocodeAndNameTrip(tripId: Long) {
        val points = repository.getPointsForTrip(tripId)
        val startPt = points.firstOrNull() ?: return
        val endPt   = points.lastOrNull()  ?: return
        val startName = geocodingService.getLocationName(startPt.latitude, startPt.longitude)
        val endName   = geocodingService.getLocationName(endPt.latitude, endPt.longitude)
        if (startName == null && endName == null) return
        val tripName = if (startName != null && endName != null) "$startName → $endName"
                       else startName ?: endName ?: return
        repository.updateTripNames(tripId, tripName, startName, endName)
    }

    private suspend fun emitState(speed: Float, tripId: Long) {
        _state.value = TrackingState(
            isTracking   = tripManager.phase != TripManager.Phase.IDLE,
            currentTrip  = repository.getTripById(tripId),
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
