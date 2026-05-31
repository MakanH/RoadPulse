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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.BlueDark
import com.makan.drivelog.presentation.theme.CoralDark
import com.makan.drivelog.presentation.theme.TealDark
import com.makan.drivelog.presentation.theme.TealPrimary

private val PurpleIcon   = Color(0xFF7F77DD)
private val PurpleIconBg = Color(0xFFEEEDFE)
private val TealIconBg   = Color(0xFFE1F5EE)
private val CoralIconBg  = Color(0xFFFAECE7)
private val BlueIconBg   = Color(0xFFE6F1FB)
private val AmberAccent  = Color(0xFFEF9F27)

@Composable
fun LeaderboardScreen(
    onTripClick: (Long) -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is LeaderboardViewModel.UiState.Loading ->
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        is LeaderboardViewModel.UiState.Empty   -> FullEmptyState()
        is LeaderboardViewModel.UiState.Success -> LeaderboardContent(state.data, onTripClick)
    }
}

// ── Full screen empty state ───────────────────────────────────────────────────

@Composable
private fun FullEmptyState() {
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
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun LeaderboardContent(
    data: LeaderboardViewModel.LeaderboardData,
    onTripClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            SectionHeader("Your records",
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp))
        }
        item { PersonalRecordsCard(data, onTripClick) }
        item { Spacer(Modifier.height(16.dp)) }
        item { TopTenSection(data, onTripClick) }
        item { Spacer(Modifier.height(16.dp)) }
        item {
            SectionHeader("Fun stats",
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp))
        }
        item { FunStatsCard(data) }
    }
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(title, fontSize = 16.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface, modifier = modifier)
}

// ── Section 1: Personal Records ───────────────────────────────────────────────

@Composable
private fun PersonalRecordsCard(
    data: LeaderboardViewModel.LeaderboardData,
    onTripClick: (Long) -> Unit
) {
    data class RecordDef(
        val icon: ImageVector, val iconColor: Color, val iconBg: Color,
        val label: String, val record: LeaderboardViewModel.PersonalRecord?
    )
    val records = listOf(
        RecordDef(Icons.Filled.Star,   TealDark,  TealIconBg,  "Fastest trip",      data.fastestRecord),
        RecordDef(Icons.Filled.Bolt,   CoralDark, CoralIconBg, "Top speed",          data.topSpeedRecord),
        RecordDef(Icons.Filled.Route,  BlueDark,  BlueIconBg,  "Longest distance",   data.longestDistanceRecord),
        RecordDef(Icons.Filled.Timer,  PurpleIcon, PurpleIconBg, "Longest duration", data.longestDurationRecord)
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            records.forEachIndexed { index, def ->
                RecordRow(
                    icon      = def.icon,
                    iconColor = def.iconColor,
                    iconBg    = def.iconBg,
                    label     = def.label,
                    record    = def.record,
                    onClick   = { def.record?.let { onTripClick(it.tripId) } }
                )
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
    icon: ImageVector,
    iconColor: Color,
    iconBg: Color,
    label: String,
    record: LeaderboardViewModel.PersonalRecord?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = record != null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
            contentAlignment = Alignment.Center
        ) {
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

// ── Section 2: Top 10 with tabs ───────────────────────────────────────────────

@Composable
private fun TopTenSection(
    data: LeaderboardViewModel.LeaderboardData,
    onTripClick: (Long) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(
        "By speed"    to data.bySpeedTop10,
        "By distance" to data.byDistanceTop10,
        "By duration" to data.byDurationTop10
    )
    val rankedTrips = tabs[selectedTab].second

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

            if (rankedTrips.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data yet", fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                rankedTrips.forEachIndexed { index, ranked ->
                    RankedTripRow(ranked = ranked, onClick = { onTripClick(ranked.tripId) })
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
private fun RankedTripRow(ranked: LeaderboardViewModel.RankedTrip, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable { onClick() }
    ) {
        // Amber left border for rank 1
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(if (ranked.rank == 1) AmberAccent else Color.Transparent)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${ranked.rank}",
                fontSize = 20.sp,
                fontWeight = FontWeight.W500,
                color = if (ranked.rank == 1) TealPrimary else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(36.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(ranked.name, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ranked.date, fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Text(ranked.value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

// ── Section 3: Fun stats ──────────────────────────────────────────────────────

@Composable
private fun FunStatsCard(data: LeaderboardViewModel.LeaderboardData) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "You've driven %.0f km".format(data.totalKm),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Next milestone: ${data.nextMilestoneName} (%.0f km)".format(data.nextMilestoneTargetKm),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = { data.milestoneProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TealPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(4.dp))
            FunStatRow(icon = Icons.Filled.CalendarToday, label = "Most active day: ${data.mostActiveDay}")
            FunStatRow(icon = Icons.Filled.AccessTime,    label = "Favorite time: ${data.favoriteTime}")
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
