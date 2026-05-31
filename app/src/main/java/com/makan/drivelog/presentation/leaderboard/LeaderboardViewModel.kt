package com.makan.drivelog.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    repository: TripRepository
) : ViewModel() {

    data class PersonalRecord(
        val tripId: Long,
        val tripName: String,
        val date: String,
        val value: String
    )

    data class RankedTrip(
        val rank: Int,
        val tripId: Long,
        val name: String,
        val date: String,
        val value: String
    )

    data class LeaderboardData(
        val fastestRecord: PersonalRecord?,
        val topSpeedRecord: PersonalRecord?,
        val longestDistanceRecord: PersonalRecord?,
        val longestDurationRecord: PersonalRecord?,
        val bySpeedTop10: List<RankedTrip>,
        val byDistanceTop10: List<RankedTrip>,
        val byDurationTop10: List<RankedTrip>,
        val totalKm: Double,
        val nextMilestoneName: String,
        val nextMilestoneTargetKm: Double,
        val milestoneProgress: Float,
        val mostActiveDay: String,
        val favoriteTime: String
    )

    sealed class UiState {
        object Loading : UiState()
        object Empty   : UiState()
        data class Success(val data: LeaderboardData) : UiState()
    }

    val uiState: StateFlow<UiState> = repository.getAllTrips()
        .map { trips ->
            if (trips.isEmpty()) UiState.Empty else UiState.Success(compute(trips))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    private fun compute(trips: List<Trip>): LeaderboardData {
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun date(t: Trip) = dateFmt.format(Date(t.startTime))
        fun durationStr(t: Trip): String {
            val ms = (t.endTime ?: t.startTime) - t.startTime
            val h = ms / 3_600_000; val m = (ms % 3_600_000) / 60_000
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
        fun pr(trip: Trip?, value: (Trip) -> String) =
            trip?.let { PersonalRecord(it.id, it.name, date(it), value(it)) }
        fun List<Trip>.ranked(value: (Trip) -> String) =
            mapIndexed { i, t -> RankedTrip(i + 1, t.id, t.name, date(t), value(t)) }

        val completed = trips.filter { it.endTime != null }

        // ── Milestones ────────────────────────────────────────────────────────
        val totalKm = trips.sumOf { it.distance } / 1000.0
        val milestones = listOf(
            "Montréal → Toronto"    to 540.0,
            "Montréal → Vancouver"  to 4_650.0,
            "Across Canada"         to 5_900.0,
            "To the moon"           to 384_400.0
        )
        val nextIdx = milestones.indexOfFirst { it.second > totalKm }
            .takeIf { it >= 0 } ?: (milestones.size - 1)
        val next   = milestones[nextIdx]
        val prevKm = if (nextIdx > 0) milestones[nextIdx - 1].second else 0.0
        val progress = if (next.second > prevKm)
            ((totalKm - prevKm) / (next.second - prevKm)).toFloat().coerceIn(0f, 1f) else 1f

        // ── Most active day ───────────────────────────────────────────────────
        val dayCounts = trips.groupingBy {
            Calendar.getInstance().also { c -> c.timeInMillis = it.startTime }
                .get(Calendar.DAY_OF_WEEK)
        }.eachCount()
        val dayNames = mapOf(
            Calendar.MONDAY    to "Monday",    Calendar.TUESDAY   to "Tuesday",
            Calendar.WEDNESDAY to "Wednesday", Calendar.THURSDAY  to "Thursday",
            Calendar.FRIDAY    to "Friday",    Calendar.SATURDAY  to "Saturday",
            Calendar.SUNDAY    to "Sunday"
        )
        val mostActiveDay = dayNames[dayCounts.maxByOrNull { it.value }?.key] ?: "—"

        // ── Favorite time of day ──────────────────────────────────────────────
        fun timeSlot(ts: Long): String {
            val h = Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
            return when { h in 5..11 -> "Morning"; h in 12..16 -> "Afternoon"; h in 17..20 -> "Evening"; else -> "Night" }
        }
        val favoriteTime = trips.groupingBy { timeSlot(it.startTime) }
            .eachCount().maxByOrNull { it.value }?.key ?: "—"

        return LeaderboardData(
            fastestRecord         = pr(trips.maxByOrNull { it.avgSpeed })  { "${(it.avgSpeed  * 3.6).toInt()} km/h" },
            topSpeedRecord        = pr(trips.maxByOrNull { it.topSpeed })  { "${(it.topSpeed  * 3.6).toInt()} km/h" },
            longestDistanceRecord = pr(trips.maxByOrNull { it.distance })  { "%.1f km".format(it.distance / 1000.0) },
            longestDurationRecord = pr(completed.maxByOrNull { it.endTime!! - it.startTime }) { durationStr(it) },
            bySpeedTop10    = trips.sortedByDescending { it.avgSpeed }.take(10)
                                  .ranked { "${(it.avgSpeed * 3.6).toInt()} km/h" },
            byDistanceTop10 = trips.sortedByDescending { it.distance }.take(10)
                                  .ranked { "%.1f km".format(it.distance / 1000.0) },
            byDurationTop10 = completed.sortedByDescending { it.endTime!! - it.startTime }.take(10)
                                  .ranked { durationStr(it) },
            totalKm              = totalKm,
            nextMilestoneName    = next.first,
            nextMilestoneTargetKm = next.second,
            milestoneProgress    = progress,
            mostActiveDay        = mostActiveDay,
            favoriteTime         = favoriteTime
        )
    }
}
