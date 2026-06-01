package com.makan.drivelog.presentation.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.makan.drivelog.presentation.theme.BlueDark
import com.makan.drivelog.presentation.theme.CoralDark
import com.makan.drivelog.presentation.theme.TealDark
import com.makan.drivelog.presentation.theme.TealPrimary
import com.makan.drivelog.presentation.util.LocalUnitPrefs

private val PurpleIcon   = Color(0xFF7F77DD)
private val PurpleIconBg = Color(0xFFEEEDFE)
private val TealIconBg   = Color(0xFFE1F5EE)
private val CoralIconBg  = Color(0xFFFAECE7)
private val BlueIconBg   = Color(0xFFE6F1FB)
private val AmberAccent  = Color(0xFFEF9F27)

@Composable
fun LeaderboardScreen(
    onTripClick: (Long) -> Unit = {},
    onNavigateToFriends: () -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is LeaderboardViewModel.UiState.Loading ->
            Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = TealPrimary) }

        is LeaderboardViewModel.UiState.Success ->
            TabbedLeaderboardContent(state, onTripClick, onNavigateToFriends)
    }
}

// ── Top-level tab structure ───────────────────────────────────────────────────

@Composable
private fun TabbedLeaderboardContent(
    state: LeaderboardViewModel.UiState.Success,
    onTripClick: (Long) -> Unit,
    onNavigateToFriends: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor   = MaterialTheme.colorScheme.surfaceVariant,
            contentColor     = MaterialTheme.colorScheme.onSurface,
            indicator        = { tabPositions ->
                val tab = tabPositions[selectedTab]
                Box(
                    Modifier
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.BottomStart)
                        .offset(x = tab.left)
                        .width(tab.width)
                        .height(2.dp)
                        .background(TealPrimary)
                )
            }
        ) {
            listOf("My stats", "Friends").forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick  = { selectedTab = i },
                    selectedContentColor   = TealPrimary,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(title, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
        }

        when (selectedTab) {
            0 -> MyStatsTab(state, onTripClick)
            1 -> FriendsTab(state, onNavigateToFriends)
        }
    }
}

// ── My Stats tab ──────────────────────────────────────────────────────────────

@Composable
private fun MyStatsTab(
    state: LeaderboardViewModel.UiState.Success,
    onTripClick: (Long) -> Unit
) {
    val personal = state.personal
    if (personal == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Spacer(Modifier.height(16.dp))
                Text("No records yet", fontWeight = FontWeight.Medium, fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(4.dp))
                Text("Your driving records will appear here", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { SectionHeader("Your records", Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)) }
        item { PersonalRecordsCard(personal, onTripClick) }
        item { Spacer(Modifier.height(16.dp)) }
        item { LocalTopTenSection(personal, onTripClick) }
        item { Spacer(Modifier.height(16.dp)) }
        item { SectionHeader("Fun stats", Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)) }
        item { FunStatsCard(personal, state.social.mostActiveFriend) }
    }
}

// ── Friends tab ───────────────────────────────────────────────────────────────

@Composable
private fun FriendsTab(
    state: LeaderboardViewModel.UiState.Success,
    onNavigateToFriends: () -> Unit
) {
    val social = state.social

    when {
        social.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }

        !social.hasFriends -> Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Filled.People, contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                Spacer(Modifier.height(16.dp))
                Text("Add friends to compare stats!", fontWeight = FontWeight.Medium,
                    fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Once you have friends your rankings will appear here.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onNavigateToFriends,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Find friends") }
            }
        }

        else -> SocialRankingsContent(social)
    }
}

@Composable
private fun SocialRankingsContent(social: LeaderboardViewModel.SocialStatsData) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "By speed"    to social.bySpeed,
        "By distance" to social.byDistance,
        "By duration" to social.byDuration
    )

    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item { Spacer(Modifier.height(16.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor     = MaterialTheme.colorScheme.onSurface,
                        indicator        = { tabPositions ->
                            val tab = tabPositions[selectedTab]
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .wrapContentSize(Alignment.BottomStart)
                                    .offset(x = tab.left)
                                    .width(tab.width)
                                    .height(2.dp)
                                    .background(TealPrimary)
                            )
                        }
                    ) {
                        tabs.forEachIndexed { i, (title, _) ->
                            Tab(
                                selected = selectedTab == i,
                                onClick  = { selectedTab = i },
                                selectedContentColor   = TealPrimary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ) {
                                Text(title, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                            }
                        }
                    }

                    val entries = tabs[selectedTab].second
                    if (entries.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                            Text("No data yet", fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        entries.forEachIndexed { index, entry ->
                            SocialEntryRow(entry = entry)
                            if (index < entries.lastIndex) {
                                HorizontalDivider(
                                    modifier  = Modifier.padding(horizontal = 16.dp),
                                    thickness = 0.5.dp,
                                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialEntryRow(entry: LeaderboardViewModel.SocialEntry) {
    val leftBorderColor = when {
        entry.rank == 1 && entry.isCurrentUser -> AmberAccent
        entry.rank == 1  -> AmberAccent
        entry.isCurrentUser -> TealPrimary
        else -> Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(leftBorderColor)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "${entry.rank}",
                fontSize = 18.sp,
                fontWeight = FontWeight.W500,
                color = if (entry.rank == 1) AmberAccent else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(28.dp)
            )
            Spacer(Modifier.width(8.dp))
            // Avatar
            if (entry.photoUrl != null) {
                AsyncImage(
                    model = entry.photoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape)
                        .background(if (entry.isCurrentUser) TealPrimary else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        entry.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            // Name + trip
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    entry.displayName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (entry.isCurrentUser) TealPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(entry.tripLabel, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(entry.value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Personal Records card ─────────────────────────────────────────────────────

@Composable
private fun PersonalRecordsCard(
    data: LeaderboardViewModel.PersonalStatsData,
    onTripClick: (Long) -> Unit
) {
    data class RecordDef(
        val icon: ImageVector, val iconColor: Color, val iconBg: Color,
        val label: String, val record: LeaderboardViewModel.PersonalRecord?
    )
    val records = listOf(
        RecordDef(Icons.Filled.Star,   TealDark,   TealIconBg,   "Fastest trip",     data.fastestRecord),
        RecordDef(Icons.Filled.Bolt,   CoralDark,  CoralIconBg,  "Top speed",        data.topSpeedRecord),
        RecordDef(Icons.Filled.Route,  BlueDark,   BlueIconBg,   "Longest distance", data.longestDistanceRecord),
        RecordDef(Icons.Filled.Timer,  PurpleIcon, PurpleIconBg, "Longest duration", data.longestDurationRecord)
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            records.forEachIndexed { index, def ->
                RecordRow(def.icon, def.iconColor, def.iconBg, def.label, def.record) {
                    def.record?.let { onTripClick(it.tripId) }
                }
                if (index < records.lastIndex) {
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordRow(
    icon: ImageVector, iconColor: Color, iconBg: Color,
    label: String, record: LeaderboardViewModel.PersonalRecord?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = record != null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(record?.tripName ?: "—", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(record?.value ?: "—", fontSize = 16.sp, fontWeight = FontWeight.W500,
                color = MaterialTheme.colorScheme.onSurface)
            Text(record?.date ?: "—", fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Local Top 10 (My stats tab only) ─────────────────────────────────────────

@Composable
private fun LocalTopTenSection(
    data: LeaderboardViewModel.PersonalStatsData,
    onTripClick: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "By speed"    to data.bySpeedTop10,
        "By distance" to data.byDistanceTop10,
        "By duration" to data.byDurationTop10
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor   = MaterialTheme.colorScheme.surfaceVariant,
                contentColor     = MaterialTheme.colorScheme.onSurface,
                indicator        = { tabPositions ->
                    val tab = tabPositions[selectedTab]
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .wrapContentSize(Alignment.BottomStart)
                            .offset(x = tab.left)
                            .width(tab.width)
                            .height(2.dp)
                            .background(TealPrimary)
                    )
                }
            ) {
                tabs.forEachIndexed { i, (title, _) ->
                    Tab(selected = selectedTab == i, onClick = { selectedTab = i },
                        selectedContentColor   = TealPrimary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant) {
                        Text(title, fontSize = 13.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }

            val rankedTrips = tabs[selectedTab].second
            if (rankedTrips.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No data yet", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                rankedTrips.forEachIndexed { index, ranked ->
                    LocalRankedTripRow(ranked = ranked, onClick = { onTripClick(ranked.tripId) })
                    if (index < rankedTrips.lastIndex) {
                        HorizontalDivider(
                            modifier  = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalRankedTripRow(ranked: LeaderboardViewModel.RankedTrip, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.width(3.dp).fillMaxHeight()
            .background(if (ranked.rank == 1) AmberAccent else Color.Transparent))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${ranked.rank}", fontSize = 20.sp, fontWeight = FontWeight.W500,
                color = if (ranked.rank == 1) TealPrimary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(36.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(ranked.name, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ranked.date, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(ranked.value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Fun stats card ────────────────────────────────────────────────────────────

@Composable
private fun FunStatsCard(data: LeaderboardViewModel.PersonalStatsData, mostActiveFriend: String?) {
    val units = LocalUnitPrefs.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("You've driven ${units.formatDistanceKm(data.totalKm)}",
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Next milestone: ${data.nextMilestoneName} (${units.formatDistanceKm(data.nextMilestoneTargetKm)})",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { data.milestoneProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TealPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(4.dp))
            FunStatRow(Icons.Filled.CalendarToday, "Most active day: ${data.mostActiveDay}")
            FunStatRow(Icons.Filled.AccessTime, "Favorite time: ${data.favoriteTime}")
            if (mostActiveFriend != null) {
                FunStatRow(Icons.Filled.People, "Most active friend: $mostActiveFriend")
            }
        }
    }
}

@Composable
private fun FunStatRow(icon: ImageVector, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}
