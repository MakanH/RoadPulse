package com.makan.drivelog.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.data.local.PreferencesManager
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: TripRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    data class LifetimeStats(
        val totalTrips: Int,
        val totalDistanceKm: Double,
        val totalDrivingHours: Double,
        val memberSince: String,
        val avgTripsPerWeek: Double,
        val avgTripDistanceKm: Double,
        val topSpeedKmh: Int
    )

    data class Settings(
        val autoTracking: Boolean,
        val speedUnit: String,
        val distanceUnit: String,
        val backgroundNotification: Boolean
    )

    data class ProfileUiState(
        val stats: LifetimeStats,
        val settings: Settings
    )

    val uiState: StateFlow<ProfileUiState?> = combine(
        repository.getAllTrips(),
        combine(
            prefs.autoTracking,
            prefs.speedUnit,
            prefs.distanceUnit,
            prefs.backgroundNotification
        ) { auto, speed, dist, bg -> Settings(auto, speed, dist, bg) }
    ) { trips, settings ->
        ProfileUiState(stats = computeStats(trips), settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setAutoTracking(v: Boolean)          { viewModelScope.launch { prefs.setAutoTracking(v) } }
    fun setBackgroundNotification(v: Boolean) { viewModelScope.launch { prefs.setBackgroundNotification(v) } }

    fun cycleSpeedUnit() {
        viewModelScope.launch {
            val cur = uiState.value?.settings?.speedUnit ?: "km/h"
            prefs.setSpeedUnit(if (cur == "km/h") "mph" else "km/h")
        }
    }

    fun cycleDistanceUnit() {
        viewModelScope.launch {
            val cur = uiState.value?.settings?.distanceUnit ?: "km"
            prefs.setDistanceUnit(if (cur == "km") "miles" else "km")
        }
    }

    fun deleteAllData() { viewModelScope.launch { repository.deleteAllData() } }

    private fun computeStats(trips: List<Trip>): LifetimeStats {
        val fmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val first = trips.minByOrNull { it.startTime }
        val memberSince = first?.let { fmt.format(Date(it.startTime)) } ?: fmt.format(Date())

        val totalMs = trips.sumOf { (it.endTime ?: it.startTime) - it.startTime }
        val weeksActive = if (first != null)
            maxOf(1.0, (System.currentTimeMillis() - first.startTime) / (7.0 * 24 * 3_600_000))
        else 1.0

        return LifetimeStats(
            totalTrips        = trips.size,
            totalDistanceKm   = trips.sumOf { it.distance } / 1000.0,
            totalDrivingHours = totalMs / 3_600_000.0,
            memberSince       = memberSince,
            avgTripsPerWeek   = trips.size / weeksActive,
            avgTripDistanceKm = if (trips.isEmpty()) 0.0 else trips.sumOf { it.distance } / (1000.0 * trips.size),
            topSpeedKmh       = ((trips.maxOfOrNull { it.topSpeed } ?: 0.0) * 3.6).toInt()
        )
    }
}
