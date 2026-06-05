package com.makan.roadpulse.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.roadpulse.data.local.PreferencesManager
import com.makan.roadpulse.data.service.AuthService
import com.makan.roadpulse.data.service.FriendService
import com.makan.roadpulse.data.service.GroupService
import com.makan.roadpulse.data.service.LeaderboardService
import com.makan.roadpulse.domain.model.FriendTripStat
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.Trip
import com.makan.roadpulse.domain.model.UserProfile
import com.makan.roadpulse.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: TripRepository,
    private val prefs: PreferencesManager,
    private val authService: AuthService,
    private val friendService: FriendService,
    private val groupService: GroupService,
    private val leaderboardService: LeaderboardService,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUid get() = auth.currentUser?.uid ?: ""

    // ── Data classes ───────────────────────────────────────────────────────────

    data class FriendLbEntry(
        val rank: Int,
        val uid: String,
        val displayName: String,
        val photoUrl: String?,
        val valueStr: String,
        val isCurrentUser: Boolean,
        val trend: Int?
    )

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
        val settings: Settings,
        val displayName: String = "Driver",
        val email: String = "",
        val photoUrl: String? = null,
        val friendCount: Int = 0,
        val groupCount: Int = 0,
        // new fields
        val friends: List<UserProfile> = emptyList(),
        val groups: List<Group> = emptyList(),
        val friendLeaderboard: List<FriendLbEntry> = emptyList(),
        val xpLevel: Int = 1,
        val xpCurrent: Int = 0,
        val xpMax: Int = 200,
        val tierName: String = "Rookie",
        val accountCreatedAt: String = ""
    )

    // ── Seed data (mirrors LeaderboardViewModel seeds for consistency) ──────────

    private val SEED_FRIENDS = listOf(
        UserProfile("s1", "Aria Novak",   "", null, 0),
        UserProfile("s2", "Rafael Costa", "", null, 0),
        UserProfile("s3", "Bianca Kim",   "", null, 0),
        UserProfile("s4", "Ivan Kozlov",  "", null, 0),
        UserProfile("s5", "Priya Singh",  "", null, 0),
        UserProfile("s6", "Tariq Benali", "", null, 0),
        UserProfile("s7", "Yuki Tanaka",  "", null, 0),
    )

    private val SEED_LB = listOf(
        // (name, distStr, trend) — "this week" distance
        Triple("You",          "2,410 km",  1),
        Triple("Aria Novak",   "1,820 km",  2),
        Triple("Sofia Reyes",  "1,540 km",  null),
        Triple("Tariq Benali", "1,390 km",  1),
    )

    // ── Flows ──────────────────────────────────────────────────────────────────

    private val friendsFlow: StateFlow<List<UserProfile>> = friendService.getFriends()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val groupsFlow: StateFlow<List<Group>> = groupService.getMyGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val lbRaw: StateFlow<List<FriendTripStat>> = leaderboardService.getFriendsTripStats()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private data class SocialBundle(
        val friends: List<UserProfile>,
        val groups: List<Group>,
        val lb: List<FriendTripStat>
    )

    private val socialBundle: StateFlow<SocialBundle> = combine(
        friendsFlow, groupsFlow, lbRaw
    ) { friends, groups, lb ->
        SocialBundle(friends, groups, lb)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000),
        SocialBundle(emptyList(), emptyList(), emptyList()))

    val uiState: StateFlow<ProfileUiState?> = combine(
        repository.getAllTrips(),
        combine(
            prefs.autoTracking, prefs.speedUnit, prefs.distanceUnit, prefs.backgroundNotification
        ) { auto, speed, dist, bg -> Settings(auto, speed, dist, bg) },
        authService.currentUser,
        socialBundle
    ) { trips, settings, user, social ->
        val uid    = user?.uid ?: ""
        val hasFriends = social.friends.isNotEmpty()

        val friends = if (hasFriends) social.friends else SEED_FRIENDS
        val lbEntries = buildFriendLb(uid, social.lb, hasFriends)
        val (lvl, xpCur, xpMax) = computeXp(computeStats(trips))

        // Account creation date from Firebase user metadata
        val createdAt = user?.metadata?.creationTimestamp
            ?.let { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(it)) }
            ?: ""

        ProfileUiState(
            stats           = computeStats(trips),
            settings        = settings,
            displayName     = user?.displayName?.takeIf { it.isNotBlank() }
                              ?: user?.email?.substringBefore('@') ?: "Driver",
            email           = user?.email ?: "",
            photoUrl        = user?.photoUrl?.toString(),
            friendCount     = friends.size,
            groupCount      = social.groups.size,
            friends         = friends,
            groups          = social.groups,
            friendLeaderboard = lbEntries,
            xpLevel         = lvl,
            xpCurrent       = xpCur,
            xpMax           = xpMax,
            tierName        = tierName(lvl),
            accountCreatedAt = createdAt
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun buildFriendLb(
        uid: String,
        raw: List<FriendTripStat>,
        hasFriends: Boolean
    ): List<FriendLbEntry> {
        if (!hasFriends) {
            return SEED_LB.mapIndexed { i, (name, dist, trend) ->
                FriendLbEntry(
                    rank          = i + 1,
                    uid           = if (i == 0) uid else "s${i}",
                    displayName   = name,
                    photoUrl      = null,
                    valueStr      = dist,
                    isCurrentUser = i == 0,
                    trend         = trend
                )
            }
        }
        // Dedupe by driver: aggregate each person's total distance so they appear once.
        return raw.groupBy { it.uid }
            .map { (_, trips) ->
                trips.first() to trips.sumOf { it.distanceKm }
            }
            .sortedByDescending { it.second }
            .take(4)
            .mapIndexed { i, (s, totalKm) ->
                FriendLbEntry(
                    rank          = i + 1,
                    uid           = s.uid,
                    displayName   = s.displayName,
                    photoUrl      = s.photoUrl,
                    valueStr      = "%,.0f km".format(totalKm),
                    isCurrentUser = s.uid == uid,
                    trend         = null
                )
            }
    }

    private fun computeXp(stats: LifetimeStats): Triple<Int, Int, Int> {
        val xp = (stats.totalTrips * 150 + stats.totalDistanceKm.toInt()).coerceAtLeast(0)
        return when {
            xp < 200  -> Triple(1, xp,       200)
            xp < 500  -> Triple(2, xp - 200, 300)
            xp < 1000 -> Triple(3, xp - 500, 500)
            xp < 2000 -> Triple(4, xp - 1000, 1000)
            else      -> Triple(5, xp - 2000, 2000)
        }
    }

    private fun tierName(level: Int) = when (level) {
        1 -> "Rookie"; 2 -> "Commuter"; 3 -> "Cruiser"; 4 -> "Racer"; else -> "Legend"
    }

    private fun computeStats(trips: List<Trip>): LifetimeStats {
        val fmt   = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val first = trips.minByOrNull { it.startTime }
        val memberSince = first?.let { fmt.format(Date(it.startTime)) } ?: fmt.format(Date())

        val totalMs    = trips.sumOf { (it.endTime ?: it.startTime) - it.startTime }
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

    // ── Mutations (unchanged) ──────────────────────────────────────────────────

    fun setAutoTracking(v: Boolean)           { viewModelScope.launch { prefs.setAutoTracking(v) } }
    fun setBackgroundNotification(v: Boolean)  { viewModelScope.launch { prefs.setBackgroundNotification(v) } }
    fun cycleSpeedUnit()    { viewModelScope.launch { prefs.setSpeedUnit(if (uiState.value?.settings?.speedUnit == "km/h") "mph" else "km/h") } }
    fun setSpeedUnit(u: String)    { viewModelScope.launch { prefs.setSpeedUnit(u) } }
    fun setDistanceUnit(u: String) { viewModelScope.launch { prefs.setDistanceUnit(u) } }
    fun cycleDistanceUnit() { viewModelScope.launch { prefs.setDistanceUnit(if (uiState.value?.settings?.distanceUnit == "km") "miles" else "km") } }
    fun deleteAllData()          { viewModelScope.launch { repository.deleteAllData() } }
    fun signOut()                = authService.signOut()
    fun deleteAccountAndAllData() { viewModelScope.launch { repository.deleteAllData(); authService.deleteAccount() } }

}
