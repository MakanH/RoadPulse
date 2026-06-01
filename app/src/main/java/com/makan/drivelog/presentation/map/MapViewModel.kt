package com.makan.drivelog.presentation.map

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.data.service.GroupService
import com.makan.drivelog.data.service.LocationTrackingService
import com.makan.drivelog.data.service.TrackingState
import com.makan.drivelog.data.service.TripSyncService
import com.makan.drivelog.domain.model.Group
import com.makan.drivelog.domain.model.LocationPoint
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class TripWithPoints(val trip: Trip, val points: List<LocationPoint>)

data class TodayStats(
    val distanceKm: Double = 0.0,
    val topSpeedKmh: Int = 0,
    val avgSpeedKmh: Int = 0
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val repository: TripRepository,
    private val groupService: GroupService,
    private val tripSyncService: TripSyncService
) : ViewModel() {

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val monthlyTripsWithPoints: List<TripWithPoints>,
            val recentTrips: List<Trip>,
            val todayStats: TodayStats,
            val trackingState: TrackingState,
            val monthLabel: String,
            val activeGroup: Group? = null
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _isHeatmapMode = MutableStateFlow(false)
    val isHeatmapMode: StateFlow<Boolean> = _isHeatmapMode.asStateFlow()

    fun toggleHeatmap() { _isHeatmapMode.value = !_isHeatmapMode.value }

    private val startOfMonth = getStartOfMonth()
    private val startOfDay = getStartOfDay()
    val monthLabel = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date())

    init {
        viewModelScope.launch {
            combine(
                repository.getTripsForMonth(startOfMonth),
                repository.getAllTrips(),
                LocationTrackingService.state,
                groupService.getMyGroups()
            ) { monthly, all, tracking, groups ->
                listOf<Any>(monthly, all, tracking, groups)
            }.collect { combined ->
                @Suppress("UNCHECKED_CAST")
                val monthly  = combined[0] as List<Trip>
                @Suppress("UNCHECKED_CAST")
                val all      = combined[1] as List<Trip>
                val tracking = combined[2] as TrackingState
                @Suppress("UNCHECKED_CAST")
                val groups   = combined[3] as List<Group>
                try {
                    val tripsWithPoints = coroutineScope {
                        monthly.map { trip ->
                            async { TripWithPoints(trip, repository.getPointsForTrip(trip.id)) }
                        }.awaitAll()
                    }
                    val todayTrips = monthly.filter { it.startTime >= startOfDay }
                    val activeGroup = groups.firstOrNull { it.activeSessionId != null }
                    _uiState.value = UiState.Success(
                        monthlyTripsWithPoints = tripsWithPoints,
                        recentTrips = all.take(10),
                        todayStats = computeTodayStats(todayTrips),
                        trackingState = tracking,
                        monthLabel = monthLabel,
                        activeGroup = activeGroup
                    )
                } catch (e: Exception) {
                    _uiState.value = UiState.Error(e.message ?: "Failed to load map data")
                }
            }
        }
    }

    fun startTracking(context: Context) {
        val intent = LocationTrackingService.startIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopTracking(context: Context) {
        context.startService(LocationTrackingService.stopIntent(context))
    }

    fun renameTrip(tripId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameTrip(tripId, newName)
            tripSyncService.updateTripNameInFirestore(tripId, newName)
        }
    }

    private fun computeTodayStats(trips: List<Trip>): TodayStats {
        if (trips.isEmpty()) return TodayStats()
        val totalDistanceKm = trips.sumOf { it.distance } / 1000.0
        val topSpeedKmh = (trips.maxOfOrNull { it.topSpeed } ?: 0.0) * 3.6
        val validSpeeds = trips.filter { it.avgSpeed > 0 }.map { it.avgSpeed * 3.6 }
        val avgSpeedKmh = if (validSpeeds.isEmpty()) 0.0 else validSpeeds.average()
        return TodayStats(
            distanceKm = totalDistanceKm,
            topSpeedKmh = topSpeedKmh.toInt(),
            avgSpeedKmh = avgSpeedKmh.toInt()
        )
    }

    private fun getStartOfMonth(): Long = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun getStartOfDay(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
