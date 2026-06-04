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
        val isCurrentUser: Boolean,
        val trend: Int? = null
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

    // ── Seed data (shown when user has no friends yet) ─────────────────────────

    internal data class SeedPerson(
        val uid: String, val name: String,
        val avgMs: Double, val topMs: Double,
        val distKm: Double, val durSecs: Long,
        val start: String, val end: String,
        val trend: Int?
    )

    internal val SEED_OTHERS = listOf(
        SeedPerson("s1", "Aria Novak",   39.44, 48.61, 1820.0, 158400L, "Blvd René-Lévesque", "Centre-ville", null),
        SeedPerson("s2", "Leo Petrov",   37.22, 44.72, 1540.0, 140400L, "Rue Saint-Denis",    "Mont-Royal",   null),
        SeedPerson("s3", "Dmitri Kovac", 35.00, 41.67, 1390.0, 134400L, "Boulevard René",     "Centre-v",      3),
        SeedPerson("s4", "Sofia Reyes",  33.06, 38.89, 1200.0, 120000L, "Rue Saint-Denis",    "Mont-R",       null),
        SeedPerson("s5", "Tariq Benali", 30.83, 36.11,  980.0, 105600L, "Avenue du Parc",     "Outremont",     1),
        SeedPerson("s6", "Yuki Tanaka",  28.61, 33.33,  840.0,  96000L, "Rue Wellington",     "Verdun",       null),
        SeedPerson("s7", "Bianca Ruiz",  26.67, 30.56,  720.0,  84000L, "Rue Renaissance",    "Avenue C.",    -2),
    )

    private fun buildSeedStats(uid: String, userStat: FriendTripStat?): List<FriendTripStat> {
        val t = System.currentTimeMillis() - 86_400_000L
        val userName = auth.currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: auth.currentUser?.email?.substringBefore('@') ?: "You"
        val me = userStat ?: FriendTripStat(
            uid = uid, displayName = userName, photoUrl = null, tripId = "seed_me",
            topSpeed = 42.5, avgSpeed = 35.56, distanceKm = 1450.0, durationSeconds = 136800L,
            startName = "Rue Sainte-Catherine", endName = "Old Port", startTime = t
        )
        return SEED_OTHERS.map { s ->
            FriendTripStat(
                uid = s.uid, displayName = s.name, photoUrl = null, tripId = "seed_${s.uid}",
                topSpeed = s.topMs, avgSpeed = s.avgMs, distanceKm = s.distKm,
                durationSeconds = s.durSecs, startName = s.start, endName = s.end, startTime = t
            )
        } + me.copy(uid = uid)
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

    // ── Computation ────────────────────────────────────────────────────────────

    private fun computeSocial(
        stats: List<FriendTripStat>,
        speedUnit: String,
        distUnit: String
    ): SocialStatsData {
        val uid       = currentUid
        val dateFmt   = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val hasFriends = stats.any { it.uid != uid }

        val workingStats = if (!hasFriends) {
            buildSeedStats(uid, stats.firstOrNull { it.uid == uid })
        } else stats

        val trendMap: Map<String, Int?> = if (!hasFriends) {
            SEED_OTHERS.associate { it.uid to it.trend } + mapOf(uid to null)
        } else emptyMap()

        fun fmtSpeed(ms: Double) = when (speedUnit) {
            "mph" -> "${(ms * 2.237).toInt()} mph"
            else  -> "${(ms * 3.6).toInt()} km/h"
        }
        fun fmtDist(km: Double) = when (distUnit) {
            "miles" -> "${"%,.0f".format(km * 0.621371)} mi"
            else    -> "${"%,.0f".format(km)} km"
        }
        fun fmtDur(secs: Long): String {
            val h = secs / 3_600; val m = (secs % 3_600) / 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
        fun tripLabel(s: FriendTripStat) = when {
            s.startName != null && s.endName != null -> "${s.startName} → ${s.endName}"
            s.startName != null -> s.startName!!
            s.endName   != null -> s.endName!!
            else -> "Trip"
        }

        fun List<FriendTripStat>.toEntries(valFn: (FriendTripStat) -> String) =
            mapIndexed { i, s ->
                SocialEntry(
                    rank          = i + 1,
                    uid           = s.uid,
                    displayName   = s.displayName,
                    photoUrl      = s.photoUrl,
                    tripLabel     = tripLabel(s),
                    date          = dateFmt.format(Date(s.startTime)),
                    value         = valFn(s),
                    isCurrentUser = s.uid == uid,
                    trend         = trendMap[s.uid]
                )
            }

        val mostActiveFriend = workingStats
            .filter { it.uid != uid }
            .groupBy { it.uid }.mapValues { it.value.size }
            .maxByOrNull { it.value }
            ?.let { (fuid, _) -> workingStats.firstOrNull { it.uid == fuid }?.displayName }

        val myTotalKm = workingStats.filter { it.uid == uid }.sumOf { it.distanceKm }
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
            isLoading             = false,
            hasFriends            = hasFriends,
            bySpeed               = workingStats.sortedByDescending { it.avgSpeed }.take(10).toEntries { fmtSpeed(it.avgSpeed) },
            byDistance            = workingStats.sortedByDescending { it.distanceKm }.take(10).toEntries { fmtDist(it.distanceKm) },
            byDuration            = workingStats.sortedByDescending { it.durationSeconds }.take(10).toEntries { fmtDur(it.durationSeconds) },
            mostActiveFriend      = mostActiveFriend,
            myTotalKm             = myTotalKm,
            nextMilestoneName     = next.first,
            nextMilestoneTargetKm = next.second,
            milestoneProgress     = progress
        )
    }
}
