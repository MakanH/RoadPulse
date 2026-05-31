package com.makan.drivelog.presentation.tripdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.domain.model.LocationPoint
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.model.TripStats
import com.makan.drivelog.domain.repository.TripRepository
import com.makan.drivelog.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sqrt

data class TripSegment(
    val label: String,
    val distanceKm: Double,
    val avgSpeedKmh: Int,
    val accentIndex: Int  // 0=teal, 1=blue, 2=coral
)

@HiltViewModel
class TripDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: TripRepository
) : ViewModel() {

    private val tripId: Long = savedStateHandle.get<Long>(Screen.TripDetail.ARG_TRIP_ID) ?: -1L

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val trip: Trip,
            val points: List<LocationPoint>,
            val stats: TripStats,
            val segments: List<TripSegment>
        ) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _scrubIndex = MutableStateFlow(0)
    val scrubIndex: StateFlow<Int> = _scrubIndex.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.getTripWithPoints(tripId).collect { (trip, points) ->
                    val stats = TripStats(
                        avgSpeed = trip.avgSpeed,
                        topSpeed = trip.topSpeed,
                        distance = trip.distance,
                        duration = (trip.endTime ?: System.currentTimeMillis()) - trip.startTime
                    )
                    _uiState.value = UiState.Success(
                        trip     = trip,
                        points   = points,
                        stats    = stats,
                        segments = computeSegments(points)
                    )
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to load trip")
            }
        }
    }

    fun onScrub(index: Int) {
        _scrubIndex.value = index
    }

    private fun computeSegments(points: List<LocationPoint>): List<TripSegment> {
        if (points.size < 6) return emptyList()
        val chunkSize = points.size / 3
        return (0 until 3).map { i ->
            val start = i * chunkSize
            val end   = if (i == 2) points.size else start + chunkSize
            val chunk = points.subList(start, end)
            val dist  = chunk.zipWithNext().sumOf { (a, b) ->
                val dLat = (b.latitude - a.latitude) * 111_000.0
                val dLon = (b.longitude - a.longitude) * 111_000.0 * cos(PI / 180.0 * a.latitude)
                sqrt(dLat * dLat + dLon * dLon)
            } / 1000.0
            val avg = if (chunk.isEmpty()) 0 else (chunk.map { it.speed * 3.6 }.average()).toInt()
            TripSegment("Segment ${i + 1}", dist, avg, i)
        }
    }
}
