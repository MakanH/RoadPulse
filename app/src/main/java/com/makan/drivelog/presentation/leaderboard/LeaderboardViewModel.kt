package com.makan.drivelog.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.drivelog.data.local.PreferencesManager
import com.makan.drivelog.data.service.LeaderboardService
import com.makan.drivelog.domain.model.FriendTripStat
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val repository: TripRepository,
    private val prefs: PreferencesManager,
    private val leaderboardService: LeaderboardService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Data classes ───────────────────────────────────────────────────────────

    data class PersonalRecord(val tripId: Long, val tripName: String, val date: String, val value: String)
    data class RankedTrip(val rank: Int, val tripId: Long, val name: String, val date: String, val value: String)

    data class PersonalStatsData(
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

    data class SocialEntry(
        val rank: Int,
        val uid: String,
        val displayName: String,
        val photoUrl: String?,
        val tripLabel: String,
        val date: String,
        val value: String,
        val isCurrentUser: Boolean
    )

    data class SocialStatsData(
        val isLoading: Boolean = true,
        val hasFriends: Boolean = false,
        val bySpeed: List<SocialEntry> = emptyList(),
        val byDistance: List<SocialEntry> = emptyList(),
        val byDuration: List<SocialEntry> = emptyList(),
        val mostActiveFriend: String? = null
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(
            val personal: PersonalStatsData?,
            val social: SocialStatsData
        ) : UiState()
    }

    // ── Streams ────────────────────────────────────────────────────────────────

    private val socialRaw: StateFlow<List<FriendTripStat>?> =
        leaderboardService.getFriendsTripStats()
            .map { it as List<FriendTripStat>? }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<UiState> = combine(
        combine(repository.getAllTrips(), prefs.speedUnit, prefs.distanceUnit) { trips, su, du ->
            Triple(trips, su, du)
        },
        socialRaw
    ) { (trips, speedUnit, distUnit), social ->
        val personal = if (trips.isEmpty()) null else computePersonal(trips, speedUnit, distUnit)
        val socialData = if (social == null) SocialStatsData(isLoading = true)
                         else computeSocial(social, speedUnit, distUnit)
        UiState.Success(personal, socialData)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // ── Personal computation (local Room) ─────────────────────────────────────

    private fun computePersonal(trips: List<Trip>, speedUnit: String, distUnit: String): PersonalStatsData {
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun date(t: Trip) = dateFmt.format(Date(t.startTime))
        fun durationStr(t: Trip): String {
            val ms = (t.endTime ?: t.startTime) - t.startTime
            val h = ms / 3_600_000; val m = (ms % 3_600_000) / 60_000
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
        fun fmtSpeed(ms: Double) = when (speedUnit) {
            "mph" -> "${(ms * 2.237).toInt()} mph"
            else  -> "${(ms * 3.6).toInt()} km/h"
        }
        fun fmtDist(m: Double) = when (distUnit) {
            "miles" -> "%.1f mi".format(m / 1_609.34)
            else    -> "%.1f km".format(m / 1_000.0)
        }
        fun pr(trip: Trip?, value: (Trip) -> String) =
            trip?.let { PersonalRecord(it.id, it.name, date(it), value(it)) }
        fun List<Trip>.ranked(value: (Trip) -> String) =
            mapIndexed { i, t -> RankedTrip(i + 1, t.id, t.name, date(t), value(t)) }

        val completed = trips.filter { it.endTime != null }
        val totalKm   = trips.sumOf { it.distance } / 1_000.0

        val milestones = listOf(
            "Montréal → Toronto"   to 540.0,
            "Montréal → Vancouver" to 4_650.0,
            "Across Canada"        to 5_900.0,
            "To the moon"          to 384_400.0
        )
        val nextIdx  = milestones.indexOfFirst { it.second > totalKm }.takeIf { it >= 0 } ?: (milestones.size - 1)
        val next     = milestones[nextIdx]
        val prevKm   = if (nextIdx > 0) milestones[nextIdx - 1].second else 0.0
        val progress = if (next.second > prevKm)
            ((totalKm - prevKm) / (next.second - prevKm)).toFloat().coerceIn(0f, 1f) else 1f

        val dayCounts = trips.groupingBy {
            Calendar.getInstance().also { c -> c.timeInMillis = it.startTime }.get(Calendar.DAY_OF_WEEK)
        }.eachCount()
        val dayNames = mapOf(
            Calendar.MONDAY to "Monday", Calendar.TUESDAY to "Tuesday",
            Calendar.WEDNESDAY to "Wednesday", Calendar.THURSDAY to "Thursday",
            Calendar.FRIDAY to "Friday", Calendar.SATURDAY to "Saturday",
            Calendar.SUNDAY to "Sunday"
        )
        val mostActiveDay = dayNames[dayCounts.maxByOrNull { it.value }?.key] ?: "—"

        fun timeSlot(ts: Long): String {
            val h = Calendar.getInstance().also { it.timeInMillis = ts }.get(Calendar.HOUR_OF_DAY)
            return when { h in 5..11 -> "Morning"; h in 12..16 -> "Afternoon"; h in 17..20 -> "Evening"; else -> "Night" }
        }
        val favoriteTime = trips.groupingBy { timeSlot(it.startTime) }
            .eachCount().maxByOrNull { it.value }?.key ?: "—"

        return PersonalStatsData(
            fastestRecord         = pr(trips.maxByOrNull { it.avgSpeed })  { fmtSpeed(it.avgSpeed) },
            topSpeedRecord        = pr(trips.maxByOrNull { it.topSpeed })  { fmtSpeed(it.topSpeed) },
            longestDistanceRecord = pr(trips.maxByOrNull { it.distance })  { fmtDist(it.distance) },
            longestDurationRecord = pr(completed.maxByOrNull { it.endTime!! - it.startTime }) { durationStr(it) },
            bySpeedTop10    = trips.sortedByDescending { it.avgSpeed }.take(10).ranked { fmtSpeed(it.avgSpeed) },
            byDistanceTop10 = trips.sortedByDescending { it.distance }.take(10).ranked { fmtDist(it.distance) },
            byDurationTop10 = completed.sortedByDescending { it.endTime!! - it.startTime }.take(10).ranked { durationStr(it) },
            totalKm              = totalKm,
            nextMilestoneName    = next.first,
            nextMilestoneTargetKm = next.second,
            milestoneProgress    = progress,
            mostActiveDay        = mostActiveDay,
            favoriteTime         = favoriteTime
        )
    }

    // ── Social computation (Firestore) ─────────────────────────────────────────

    private fun computeSocial(stats: List<FriendTripStat>, speedUnit: String, distUnit: String): SocialStatsData {
        val uid  = currentUid
        val dateFmt = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        fun fmtSpeed(ms: Double) = when (speedUnit) {
            "mph" -> "${(ms * 2.237).toInt()} mph"
            else  -> "${(ms * 3.6).toInt()} km/h"
        }
        fun fmtDist(km: Double) = when (distUnit) {
            "miles" -> "%.1f mi".format(km * 0.621371)
            else    -> "%.1f km".format(km)
        }
        fun fmtDuration(secs: Long): String {
            val h = secs / 3_600; val m = (secs % 3_600) / 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
        fun tripLabel(s: FriendTripStat) = when {
            s.startName != null && s.endName != null -> "${s.startName} → ${s.endName}"
            s.startName != null -> s.startName
            s.endName   != null -> s.endName
            else                -> "Trip"
        }
        fun date(s: FriendTripStat) = dateFmt.format(Date(s.startTime))

        fun List<FriendTripStat>.toSocialEntries(value: (FriendTripStat) -> String) =
            mapIndexed { i, s ->
                SocialEntry(
                    rank          = i + 1,
                    uid           = s.uid,
                    displayName   = s.displayName,
                    photoUrl      = s.photoUrl,
                    tripLabel     = tripLabel(s),
                    date          = date(s),
                    value         = value(s),
                    isCurrentUser = s.uid == uid
                )
            }

        val hasFriends = stats.any { it.uid != uid }

        val mostActiveFriend = stats
            .filter { it.uid != uid }
            .groupBy { it.uid }
            .mapValues { it.value.size }
            .maxByOrNull { it.value }
            ?.let { (friendUid, _) -> stats.firstOrNull { it.uid == friendUid }?.displayName }

        return SocialStatsData(
            isLoading        = false,
            hasFriends       = hasFriends,
            bySpeed          = stats.sortedByDescending { it.avgSpeed }.take(10)
                                    .toSocialEntries { fmtSpeed(it.avgSpeed) },
            byDistance       = stats.sortedByDescending { it.distanceKm }.take(10)
                                    .toSocialEntries { fmtDist(it.distanceKm) },
            byDuration       = stats.sortedByDescending { it.durationSeconds }.take(10)
                                    .toSocialEntries { fmtDuration(it.durationSeconds) },
            mostActiveFriend = mostActiveFriend
        )
    }
}
