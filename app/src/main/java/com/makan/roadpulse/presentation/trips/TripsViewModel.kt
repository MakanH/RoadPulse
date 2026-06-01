package com.makan.roadpulse.presentation.trips

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.roadpulse.data.service.TripSyncService
import com.makan.roadpulse.domain.model.Trip
import com.makan.roadpulse.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TripsViewModel @Inject constructor(
    private val repository: TripRepository,
    private val tripSyncService: TripSyncService
) : ViewModel() {

    enum class TripFilter { ALL_TIME, THIS_MONTH, THIS_WEEK }

    data class YearMonth(val year: Int, val month: Int) // month: 1–12

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
            val stats: SummaryStats,
            val availableYears: List<Int>,
            val tripsPerYearMonth: Map<Int, Set<Int>>
        ) : UiState()
    }

    private val _filter          = MutableStateFlow(TripFilter.THIS_MONTH)
    private val _yearMonthFilter  = MutableStateFlow<YearMonth?>(null)

    val filter:          StateFlow<TripFilter>  = _filter.asStateFlow()
    val yearMonthFilter: StateFlow<YearMonth?>  = _yearMonthFilter.asStateFlow()

    val uiState: StateFlow<UiState> = combine(
        repository.getAllTrips(),
        _filter,
        _yearMonthFilter
    ) { trips, chipFilter, ymFilter ->
        val filtered = if (ymFilter != null) applyYearMonthFilter(trips, ymFilter)
                       else applyFilter(trips, chipFilter)
        UiState.Success(
            grouped            = groupByDate(filtered),
            stats              = computeStats(filtered),
            availableYears     = computeAvailableYears(trips),
            tripsPerYearMonth  = computeTripsPerYearMonth(trips)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    fun renameTrip(tripId: Long, newName: String) {
        viewModelScope.launch {
            repository.renameTrip(tripId, newName)
            tripSyncService.updateTripNameInFirestore(tripId, newName)
        }
    }

    fun setFilter(filter: TripFilter) {
        _filter.value = filter
        _yearMonthFilter.value = null   // clear year/month when switching chip filters
    }

    fun setYearMonthFilter(year: Int, month: Int) { _yearMonthFilter.value = YearMonth(year, month) }
    fun clearYearMonthFilter()                    { _yearMonthFilter.value = null }

    // ── Filtering ─────────────────────────────────────────────────────────────

    private fun applyFilter(trips: List<Trip>, filter: TripFilter): List<Trip> {
        val cutoff = when (filter) {
            TripFilter.ALL_TIME   -> 0L
            TripFilter.THIS_MONTH -> startOfMonth()
            TripFilter.THIS_WEEK  -> startOfWeek()
        }
        return trips.filter { it.startTime >= cutoff }
    }

    private fun applyYearMonthFilter(trips: List<Trip>, ym: YearMonth): List<Trip> =
        trips.filter { tripYear(it.startTime) == ym.year && tripMonth(it.startTime) == ym.month }

    // ── Grouping ──────────────────────────────────────────────────────────────

    private fun groupByDate(trips: List<Trip>): Map<String, List<Trip>> {
        val todayMs     = startOfDay()
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

    // ── Stats ─────────────────────────────────────────────────────────────────

    private fun computeStats(trips: List<Trip>): SummaryStats {
        val totalMs   = trips.sumOf { (it.endTime ?: it.startTime) - it.startTime }
        val hours     = totalMs / 3_600_000
        val mins      = (totalMs % 3_600_000) / 60_000
        val driveTime = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        return SummaryStats(
            totalTrips      = trips.size,
            totalDistanceKm = trips.sumOf { it.distance } / 1000.0,
            totalDriveTime  = driveTime,
            topSpeedKmh     = ((trips.maxOfOrNull { it.topSpeed } ?: 0.0) * 3.6).toInt()
        )
    }

    // ── Picker data ───────────────────────────────────────────────────────────

    private fun computeAvailableYears(trips: List<Trip>): List<Int> =
        trips.map { tripYear(it.startTime) }.distinct().sortedDescending()

    private fun computeTripsPerYearMonth(trips: List<Trip>): Map<Int, Set<Int>> =
        trips.groupBy { tripYear(it.startTime) }
             .mapValues { (_, ts) -> ts.map { tripMonth(it.startTime) }.toSet() }

    // ── Calendar helpers ──────────────────────────────────────────────────────

    private fun tripYear(ts: Long)  = Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.YEAR)
    private fun tripMonth(ts: Long) = Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.MONTH) + 1

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
