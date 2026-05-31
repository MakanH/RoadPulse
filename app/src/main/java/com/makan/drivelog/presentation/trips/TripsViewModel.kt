package com.makan.drivelog.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    repository: TripRepository
) : ViewModel() {

    enum class TripFilter { ALL_TIME, THIS_MONTH, THIS_WEEK, TODAY }

    data class SummaryStats(
        val totalTrips: Int,
        val totalDistanceKm: Double,
        val totalDriveTime: String,
        val topSpeedKmh: Int
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val grouped: Map<String, List<Trip>>,
            val stats: SummaryStats
        ) : UiState()
    }

    private val _filter = MutableStateFlow(TripFilter.THIS_MONTH)
    val filter: StateFlow<TripFilter> = _filter.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        repository.getAllTrips(),
        _filter
    ) { trips, filter ->
        val filtered = applyFilter(trips, filter)
        UiState.Success(
            grouped = groupByDate(filtered),
            stats = computeStats(filtered)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun setFilter(filter: TripFilter) { _filter.value = filter }

    private fun applyFilter(trips: List<Trip>, filter: TripFilter): List<Trip> {
        val cutoff = when (filter) {
            TripFilter.ALL_TIME   -> 0L
            TripFilter.THIS_MONTH -> startOfMonth()
            TripFilter.THIS_WEEK  -> startOfWeek()
            TripFilter.TODAY      -> startOfDay()
        }
        return trips.filter { it.startTime >= cutoff }
    }

    private fun groupByDate(trips: List<Trip>): Map<String, List<Trip>> {
        val todayMs = startOfDay()
        val yesterdayMs = todayMs - 86_400_000L
        val fmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        return trips.groupByTo(LinkedHashMap()) { trip ->
            when {
                trip.startTime >= todayMs     -> "Today"
                trip.startTime >= yesterdayMs -> "Yesterday"
                else                          -> fmt.format(Date(trip.startTime))
            }
        }
    }

    private fun computeStats(trips: List<Trip>): SummaryStats {
        val totalMs = trips.sumOf { (it.endTime ?: it.startTime) - it.startTime }
        val hours = totalMs / 3_600_000
        val mins = (totalMs % 3_600_000) / 60_000
        val driveTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        return SummaryStats(
            totalTrips = trips.size,
            totalDistanceKm = trips.sumOf { it.distance } / 1000.0,
            totalDriveTime = driveTime,
            topSpeedKmh = ((trips.maxOfOrNull { it.topSpeed } ?: 0.0) * 3.6).toInt()
        )
    }

    private fun startOfDay() = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfWeek() = Calendar.getInstance().apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun startOfMonth() = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
