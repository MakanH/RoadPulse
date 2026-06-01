package com.makan.roadpulse.presentation.leaderboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.roadpulse.data.local.PreferencesManager
import com.makan.roadpulse.data.service.LeaderboardService
import com.makan.roadpulse.domain.model.FriendTripStat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LeaderboardViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val leaderboardService: LeaderboardService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Data classes ───────────────────────────────────────────────────────────

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
        val mostActiveFriend: String? = null,
        val myTotalKm: Double = 0.0,
        val nextMilestoneName: String = "Montréal → Toronto",
        val nextMilestoneTargetKm: Double = 540.0,
        val milestoneProgress: Float = 0f
    )

    sealed class UiState {
        object Loading : UiState()
        data class Success(val social: SocialStatsData) : UiState()
    }

    // ── Streams ────────────────────────────────────────────────────────────────

    private val socialRaw: StateFlow<List<FriendTripStat>?> =
        leaderboardService.getFriendsTripStats()
            .map { it as List<FriendTripStat>? }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val uiState: StateFlow<UiState> = combine(
        socialRaw,
        prefs.speedUnit,
        prefs.distanceUnit
    ) { social, speedUnit, distUnit ->
        val socialData = if (social == null) SocialStatsData(isLoading = true)
                         else computeSocial(social, speedUnit, distUnit)
        UiState.Success(socialData)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)

    // ── Social computation (Firestore) ─────────────────────────────────────────

    private fun computeSocial(stats: List<FriendTripStat>, speedUnit: String, distUnit: String): SocialStatsData {
        val uid     = currentUid
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

        fun List<FriendTripStat>.toEntries(value: (FriendTripStat) -> String) =
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
            ?.let { (fuid, _) -> stats.firstOrNull { it.uid == fuid }?.displayName }

        // Milestone from current user's own Firestore-synced trips
        val myTotalKm = stats.filter { it.uid == uid }.sumOf { it.distanceKm }
        val milestones = listOf(
            "Montréal → Toronto"   to 540.0,
            "Montréal → Vancouver" to 4_650.0,
            "Across Canada"        to 5_900.0,
            "To the moon"          to 384_400.0
        )
        val nextIdx  = milestones.indexOfFirst { it.second > myTotalKm }.takeIf { it >= 0 } ?: (milestones.size - 1)
        val next     = milestones[nextIdx]
        val prevKm   = if (nextIdx > 0) milestones[nextIdx - 1].second else 0.0
        val progress = if (next.second > prevKm)
            ((myTotalKm - prevKm) / (next.second - prevKm)).toFloat().coerceIn(0f, 1f) else 1f

        return SocialStatsData(
            isLoading            = false,
            hasFriends           = hasFriends,
            bySpeed              = stats.sortedByDescending { it.avgSpeed }.take(10).toEntries { fmtSpeed(it.avgSpeed) },
            byDistance           = stats.sortedByDescending { it.distanceKm }.take(10).toEntries { fmtDist(it.distanceKm) },
            byDuration           = stats.sortedByDescending { it.durationSeconds }.take(10).toEntries { fmtDuration(it.durationSeconds) },
            mostActiveFriend     = mostActiveFriend,
            myTotalKm            = myTotalKm,
            nextMilestoneName    = next.first,
            nextMilestoneTargetKm = next.second,
            milestoneProgress    = progress
        )
    }
}
