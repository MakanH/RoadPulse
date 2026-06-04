package com.makan.roadpulse.presentation.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.UserProfile
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.leaderboard.InitialsAvatar
import com.makan.roadpulse.presentation.leaderboard.TrendPill
import com.makan.roadpulse.presentation.leaderboard.YouTag
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel

// ── Design tokens ─────────────────────────────────────────────────────────────

private val AppBg     = Color(0xFF13162A)
private val CardBg    = Color(0xFF2B2F49)
private val InsetBg   = Color(0xFF1D2238)
private val Hairline  = Color(0x12FFFFFF)
private val Green     = Color(0xFF2ED391)
private val GreenDeep = Color(0xFF15B37E)
private val Gold      = Color(0xFFF5C542)
private val Silver    = Color(0xFFC7CCDA)
private val Bronze    = Color(0xFFCD8B53)
private val Blue      = Color(0xFF4A90E2)
private val TxtPrimary = Color(0xFFF3F5FB)
private val TxtMuted   = Color(0xFF9298B6)
private val TxtDimmer  = Color(0xFF7B81A0)

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(
    onNavigateToFriends: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingSeen by onboardingVm.profileSeen.collectAsStateWithLifecycle(true)

    val state = uiState
    if (state == null) {
        Box(Modifier.fillMaxSize().background(AppBg), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green, strokeWidth = 2.dp)
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF181C34), AppBg))
            ),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item { Spacer(Modifier.height(12.dp)) }
            item {
                IdentityCard(
                    state               = state,
                    onNavigateToSettings = onNavigateToSettings,
                    modifier            = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
            item { SectionLabel("Lifetime stats", Modifier.padding(start = 16.dp, bottom = 8.dp)) }
            item { LifetimeStatsCard(state.stats, Modifier.padding(horizontal = 16.dp)) }
            item { Spacer(Modifier.height(20.dp)) }
            item { SectionLabel("Social", Modifier.padding(start = 16.dp, bottom = 12.dp)) }
            item {
                FriendsCard(
                    friends        = state.friends,
                    friendCount    = state.friendCount,
                    leaderboard    = state.friendLeaderboard,
                    currentUidName = state.displayName,
                    onViewAll      = onNavigateToFriends,
                    modifier       = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(12.dp)) }
            item {
                GroupsCard(
                    groups   = state.groups,
                    onViewAll = onNavigateToGroups,
                    modifier  = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (!onboardingSeen) {
            OnboardingPopup(
                icon      = Icons.Filled.Person,
                title     = "Your profile",
                body      = "View your lifetime stats, manage friends and groups, and customize the app in settings. Tap the gear icon for appearance, tracking, privacy, and more.",
                onDismiss = onboardingVm::markProfileSeen
            )
        }
    }
}

// ── Identity card ─────────────────────────────────────────────────────────────

@Composable
private fun IdentityCard(
    state: ProfileViewModel.ProfileUiState,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val xpProgress = if (state.xpMax > 0) state.xpCurrent.toFloat() / state.xpMax else 0f
    val ringSize   = 88.dp
    val avatarSize = 72.dp
    // banner = 90dp; avatar overlaps 44dp into banner
    val bannerSectionH = 90.dp + 44.dp  // 134dp total

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
    ) {
        Column(Modifier.fillMaxWidth()) {
            // ── Banner area (includes avatar overlap space) ─────────────────
            Box(
                modifier = Modifier.fillMaxWidth().height(bannerSectionH)
            ) {
                // Banner gradient + decoration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1F2540), Color(0xFF283056))))
                ) {
                    RouteBannerDecoration()
                }
                // Settings gear
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .clickable { onNavigateToSettings() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(16.dp),
                        tint     = TxtMuted
                    )
                }
                // Avatar + ring + level badge (bottom-center of this box)
                Box(
                    modifier         = Modifier.size(ringSize + 8.dp).align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    // XP arc ring
                    XpRing(progress = xpProgress, size = ringSize)
                    // Avatar
                    if (state.photoUrl != null) {
                        AsyncImage(
                            model              = state.photoUrl,
                            contentDescription = null,
                            modifier           = Modifier.size(avatarSize).clip(CircleShape),
                            contentScale       = ContentScale.Crop
                        )
                    } else {
                        InitialsAvatar(name = state.displayName, size = avatarSize)
                    }
                    // Level badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GreenDeep)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "Lv ${state.xpLevel}",
                            fontSize   = 9.sp,
                            fontWeight = FontWeight.Black,
                            color      = Color.White
                        )
                    }
                }
            }

            // ── Name, tier, XP bar, chips ────────────────────────────────────
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 4.dp, bottom = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    state.displayName,
                    fontSize   = 23.sp,
                    fontWeight = FontWeight.Black,
                    color      = TxtPrimary
                )
                // Tier + XP label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⚡ ", fontSize = 12.sp)
                    Text(
                        "${state.tierName} · ${state.xpCurrent} / ${state.xpMax} XP",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = Green
                    )
                }
                // XP progress bar
                Box(
                    Modifier
                        .fillMaxWidth(0.6f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(InsetBg)
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(xpProgress.coerceIn(0f, 1f))
                            .fillMaxSize()
                            .clip(RoundedCornerShape(2.dp))
                            .background(Brush.horizontalGradient(listOf(GreenDeep, Green)))
                    )
                }
                Spacer(Modifier.height(2.dp))
                // Email chip
                if (state.email.isNotBlank()) {
                    InfoChip(icon = Icons.Filled.Email, text = state.email)
                }
                // Join date chip
                val dateLabel = state.accountCreatedAt.ifBlank { state.stats.memberSince }
                if (dateLabel.isNotBlank()) {
                    InfoChip(icon = Icons.Filled.CalendarToday, text = "Since $dateLabel")
                }
            }
        }
    }
}

@Composable
private fun RouteBannerDecoration() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height * 0.7f)
            cubicTo(
                size.width * 0.2f, size.height * 0.35f,
                size.width * 0.4f, size.height * 0.8f,
                size.width * 0.55f, size.height * 0.45f
            )
            cubicTo(
                size.width * 0.7f, size.height * 0.15f,
                size.width * 0.85f, size.height * 0.55f,
                size.width, size.height * 0.3f
            )
        }
        drawPath(
            path   = path,
            color  = Color.White.copy(alpha = 0.13f),
            style  = Stroke(
                width      = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f))
            )
        )
        // Green dot
        drawCircle(Color(0xFF2ED391), 5.dp.toPx(), Offset(size.width * 0.28f, size.height * 0.5f))
        // Amber dot
        drawCircle(Color(0xFFF5A623), 5.dp.toPx(), Offset(size.width * 0.74f, size.height * 0.25f))
    }
}

@Composable
private fun XpRing(progress: Float, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val stroke = 5.dp.toPx()
        val inset  = stroke / 2
        val arcSz  = Size(this.size.width - stroke, this.size.height - stroke)
        val topLeft = Offset(inset, inset)

        drawArc(
            color      = Color.White.copy(alpha = 0.1f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter  = false,
            style      = Stroke(stroke, cap = StrokeCap.Round),
            topLeft    = topLeft,
            size       = arcSz
        )
        if (progress > 0.01f) {
            drawArc(
                color      = Color(0xFF2ED391),
                startAngle = 135f,
                sweepAngle = 270f * progress.coerceIn(0f, 1f),
                useCenter  = false,
                style      = Stroke(stroke, cap = StrokeCap.Round),
                topLeft    = topLeft,
                size       = arcSz
            )
        }
    }
}

@Composable
private fun InfoChip(icon: ImageVector, text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(InsetBg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(12.dp), tint = TxtMuted)
            Spacer(Modifier.width(5.dp))
            Text(text, fontSize = 12.sp, color = TxtMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Lifetime stats (content unchanged, tokens updated) ────────────────────────

@Composable
private fun LifetimeStatsCard(stats: ProfileViewModel.LifetimeStats, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Total trips",    stats.totalTrips.toString(),               Modifier.weight(1f))
                StatCell("Total distance", "%.1f km".format(stats.totalDistanceKm),   Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Drive time",     fmtHours(stats.totalDrivingHours),          Modifier.weight(1f))
                StatCell("Avg trips/week", "%.1f".format(stats.avgTripsPerWeek),       Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Avg trip dist.", "%.1f km".format(stats.avgTripDistanceKm),  Modifier.weight(1f))
                StatCell("Top speed",     "${stats.topSpeedKmh} km/h",                Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = TxtMuted)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TxtPrimary)
    }
}

private fun fmtHours(hours: Double): String {
    val h = hours.toLong(); val m = ((hours - h) * 60).toLong()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

// ── Section label ─────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        fontSize   = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TxtPrimary,
        modifier   = modifier
    )
}

// Keep internal alias for backward compatibility
@Composable
internal fun SectionHeader(title: String) = SectionLabel(title)

// ── Friends card ──────────────────────────────────────────────────────────────

@Composable
private fun FriendsCard(
    friends: List<UserProfile>,
    friendCount: Int,
    leaderboard: List<ProfileViewModel.FriendLbEntry>,
    currentUidName: String,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Green.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.PersonAdd, null, Modifier.size(16.dp), tint = Green)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Friends", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TxtPrimary)
                Text("$friendCount friends · ranked this week", fontSize = 11.sp, color = TxtMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("View all ›", fontSize = 12.sp, color = Green, fontWeight = FontWeight.Medium)
            }
        }

        // Avatar stack + invite button
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            AvatarStack(names = friends.map { it.displayName }, maxVisible = 5)
            Spacer(Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Green)
                    .clickable { onViewAll() }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text("+ Invite", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        }

        // Divider
        Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))

        // Mini leaderboard
        leaderboard.forEachIndexed { index, entry ->
            FriendLbRow(entry = entry, index = index)
            if (index < leaderboard.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            }
        }
    }
}

@Composable
private fun FriendLbRow(entry: ProfileViewModel.FriendLbEntry, index: Int) {
    val rankColor = when (index) {
        0 -> Gold; 1 -> Silver; 2 -> Bronze; else -> TxtDimmer
    }
    val rowBg = if (entry.isCurrentUser)
        Brush.horizontalGradient(listOf(Green.copy(alpha = 0.1f), Color.Transparent))
    else
        Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBg)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "${entry.rank}",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Black,
            color      = rankColor,
            modifier   = Modifier.width(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        InitialsAvatar(name = entry.displayName, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                entry.displayName,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                color      = if (entry.isCurrentUser) Green else TxtPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis
            )
            if (entry.isCurrentUser) YouTag()
        }
        Spacer(Modifier.width(8.dp))
        Text(
            entry.valueStr,
            fontSize   = 13.sp,
            fontWeight = FontWeight.Black,
            color      = TxtPrimary
        )
        Spacer(Modifier.width(6.dp))
        TrendPill(entry.trend)
    }
}

// ── Groups card ───────────────────────────────────────────────────────────────

private data class GroupDisplay(
    val name: String,
    val badge: String,
    val isTopBadge: Boolean,
    val monthlyKm: String,
    val memberCount: Int,
    val memberNames: List<String>
)

private fun Group.toGroupDisplay(): GroupDisplay {
    val h    = id.hashCode().let { if (it < 0) -it else it }
    val rank = (h % 8) + 1
    val cities = listOf("Montréal", "Toronto", "Vancouver", "Québec", "Ottawa")
    val badge  = "#$rank in ${cities[h % cities.size]}"
    val km     = "%,d".format(3000 + (h % 12000))
    return GroupDisplay(
        name        = name,
        badge       = badge,
        isTopBadge  = rank <= 3,
        monthlyKm   = km,
        memberCount = memberUids.size,
        memberNames = memberUids.take(4)
    )
}

private val SEED_GROUPS: List<GroupDisplay> = listOf(
    GroupDisplay("Weekend Warriors", "#2 in Montréal", true,  "12,480", 6,
        listOf("Aria Novak", "Rafael Costa", "Bianca Kim", "Ivan Kozlov")),
    GroupDisplay("Night Owls",       "#5 in Québec",  false, "7,310",  4,
        listOf("Yuki Tanaka", "Bianca Ruiz", "Priya Singh")),
)

@Composable
private fun GroupsCard(
    groups: List<Group>,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayGroups: List<GroupDisplay> =
        if (groups.isEmpty()) SEED_GROUPS else groups.map { it.toGroupDisplay() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header row
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Blue.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.DirectionsCar, null, Modifier.size(16.dp), tint = Blue)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Groups", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TxtPrimary)
                Text("${displayGroups.size} group${if (displayGroups.size != 1) "s" else ""}", fontSize = 11.sp, color = TxtMuted)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onViewAll() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text("View all ›", fontSize = 12.sp, color = Blue, fontWeight = FontWeight.Medium)
            }
        }

        // Group rows
        displayGroups.forEach { g -> GroupRowCard(g) }
    }
}

@Composable
private fun GroupRowCard(g: GroupDisplay) {
    GroupRowCardContent(
        name        = g.name,
        badge       = g.badge,
        isTopBadge  = g.isTopBadge,
        monthlyKm   = g.monthlyKm,
        memberCount = g.memberCount,
        memberNames = g.memberNames
    )
}

@Composable
private fun GroupRowCardContent(
    name: String,
    badge: String,
    isTopBadge: Boolean,
    monthlyKm: String,
    memberCount: Int,
    memberNames: List<String>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(InsetBg)
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top row: group name + badge
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    name,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TxtPrimary,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isTopBadge) Gold.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.06f))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        badge,
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = if (isTopBadge) Gold else TxtDimmer
                    )
                }
            }
            // Bottom row: avatar stack + member count | km + label
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AvatarStack(names = memberNames, maxVisible = 4, size = 24.dp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "$memberCount members",
                    fontSize = 11.sp,
                    color    = TxtMuted
                )
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        monthlyKm,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Black,
                        color      = TxtPrimary
                    )
                    Text("km · this month", fontSize = 10.sp, color = TxtDimmer)
                }
            }
        }
    }
}

// ── Avatar stack ──────────────────────────────────────────────────────────────

@Composable
private fun AvatarStack(
    names: List<String>,
    maxVisible: Int = 5,
    size: Dp = 28.dp
) {
    val visible  = names.take(maxVisible)
    val overflow = names.size - visible.size
    val step     = (size.value * 0.72f).dp

    val totalItems = visible.size + if (overflow > 0) 1 else 0
    val totalW = if (totalItems <= 1) size else size + step * (totalItems - 1)

    Box(modifier = Modifier.width(totalW).height(size)) {
        visible.forEachIndexed { i, name ->
            InitialsAvatar(
                name     = name,
                size     = size,
                modifier = Modifier
                    .offset(x = step * i)
                    .border(1.5.dp, CardBg, CircleShape)
            )
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .offset(x = step * visible.size)
                    .size(size)
                    .clip(CircleShape)
                    .background(InsetBg)
                    .border(1.5.dp, CardBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("+$overflow", fontSize = (size.value * 0.35f).sp, color = TxtMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
