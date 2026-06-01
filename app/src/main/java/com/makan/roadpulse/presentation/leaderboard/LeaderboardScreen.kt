package com.makan.roadpulse.presentation.leaderboard

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.People
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
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel
import com.makan.roadpulse.presentation.theme.TealPrimary
import com.makan.roadpulse.presentation.util.LocalUnitPrefs

private val AmberAccent = Color(0xFFEF9F27)

@Composable
fun LeaderboardScreen(
    onNavigateToFriends: () -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingSeen by onboardingVm.boardSeen.collectAsStateWithLifecycle(true)

    Box(Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is LeaderboardViewModel.UiState.Loading ->
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TealPrimary)
                }
            is LeaderboardViewModel.UiState.Success ->
                SocialContent(state.social, onNavigateToFriends)
        }

        if (!onboardingSeen) {
            OnboardingPopup(
                icon      = Icons.Filled.EmojiEvents,
                title     = "Leaderboard",
                body      = "See how you stack up against your friends. Compare top speeds, longest trips, and total distance. Add friends from your profile to get started.",
                onDismiss = onboardingVm::markBoardSeen
            )
        }
    }
}

@Composable
private fun SocialContent(
    social: LeaderboardViewModel.SocialStatsData,
    onNavigateToFriends: () -> Unit
) {
    when {
        social.isLoading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TealPrimary)
        }

        !social.hasFriends -> Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(32.dp),
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
                Text("Your rankings with friends will appear here.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(20.dp))
                Button(onClick = onNavigateToFriends,
                    colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape = RoundedCornerShape(10.dp)) {
                    Text("Find friends")
                }
            }
        }

        else -> LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }
            item { RankingsCard(social) }
            item { Spacer(Modifier.height(16.dp)) }
            item { FunStatsCard(social) }
        }
    }
}

// ── Rankings card with inner speed/distance/duration tabs ─────────────────────

@Composable
private fun RankingsCard(social: LeaderboardViewModel.SocialStatsData) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("By speed" to social.bySpeed, "By distance" to social.byDistance, "By duration" to social.byDuration)

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
                        Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomStart)
                            .offset(x = tab.left).width(tab.width).height(2.dp).background(TealPrimary)
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

            val entries = tabs[selectedTab].second
            if (entries.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                    Text("No data yet", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                entries.forEachIndexed { index, entry ->
                    SocialEntryRow(entry)
                    if (index < entries.lastIndex) {
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SocialEntryRow(entry: LeaderboardViewModel.SocialEntry) {
    val leftBorderColor = when {
        entry.rank == 1 -> AmberAccent
        entry.isCurrentUser -> TealPrimary
        else -> Color.Transparent
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(3.dp).fillMaxHeight().background(leftBorderColor))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${entry.rank}", fontSize = 18.sp, fontWeight = FontWeight.W500,
                color = if (entry.rank == 1) AmberAccent else MaterialTheme.colorScheme.secondary,
                modifier = Modifier.width(28.dp))
            Spacer(Modifier.width(8.dp))
            if (entry.photoUrl != null) {
                AsyncImage(model = entry.photoUrl, contentDescription = null,
                    modifier = Modifier.size(32.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.size(32.dp).clip(CircleShape)
                    .background(if (entry.isCurrentUser) TealPrimary else MaterialTheme.colorScheme.outline),
                    contentAlignment = Alignment.Center) {
                    Text(entry.displayName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.displayName, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = if (entry.isCurrentUser) TealPrimary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
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

// ── Fun stats card ────────────────────────────────────────────────────────────

@Composable
private fun FunStatsCard(social: LeaderboardViewModel.SocialStatsData) {
    val units = LocalUnitPrefs.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("You've driven ${units.formatDistanceKm(social.myTotalKm)}",
                fontSize = 14.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Text("Next milestone: ${social.nextMilestoneName} (${units.formatDistanceKm(social.nextMilestoneTargetKm)})",
                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            LinearProgressIndicator(
                progress = { social.milestoneProgress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = TealPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                strokeCap = StrokeCap.Round
            )
            Spacer(Modifier.height(4.dp))
            if (social.mostActiveFriend != null) {
                FunStatRow(Icons.Filled.People, "Most active friend: ${social.mostActiveFriend}")
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
