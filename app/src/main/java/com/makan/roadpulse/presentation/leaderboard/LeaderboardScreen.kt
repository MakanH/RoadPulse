package com.makan.roadpulse.presentation.leaderboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.components.RobotoCondensedBold
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel

// ── Design tokens ─────────────────────────────────────────────────────────────

private val AppBg      = Color(0xFF13162A)
private val CardBg     = Color(0xFF2B2F49)
private val InsetBg    = Color(0xFF1D2238)
private val SegBg      = Color(0xFF1B1F36)
private val Hairline   = Color(0x12FFFFFF)
private val Green      = Color(0xFF2ED391)
private val GreenDeep  = Color(0xFF15B37E)
private val Gold       = Color(0xFFF5C542)
private val GoldDark   = Color(0xFFB8942A)
private val Amber      = Color(0xFFF5A623)
private val Silver     = Color(0xFFC7CCDA)
private val SilverDark = Color(0xFF8F949F)
private val Bronze     = Color(0xFFCD8B53)
private val BronzeDark = Color(0xFF8B5C32)
private val Danger     = Color(0xFFE0606E)
private val TxtPrimary = Color(0xFFF3F5FB)
private val TxtMuted   = Color(0xFF9298B6)
private val TxtDimmer  = Color(0xFF7B81A0)

private val AVATAR_COLORS = listOf(
    Color(0xFF6B4CE0), Color(0xFF4A90E2), Color(0xFF2ED391), Color(0xFFE0606E),
    Color(0xFFF5A623), Color(0xFF9B59B6), Color(0xFF1ABC9C), Color(0xFFE74C3C)
)
private fun avatarColor(name: String) =
    AVATAR_COLORS[name.hashCode().let { if (it < 0) -it else it } % AVATAR_COLORS.size]

// ── Screen ────────────────────────────────────────────────────────────────────

@Composable
fun LeaderboardScreen(
    onNavigateToFriends: () -> Unit = {},
    viewModel: LeaderboardViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingSeen by onboardingVm.boardSeen.collectAsStateWithLifecycle(true)

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF181C34), AppBg)))
    ) {
        when (val state = uiState) {
            is LeaderboardViewModel.UiState.Loading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green, strokeWidth = 2.dp)
                }
            is LeaderboardViewModel.UiState.Success ->
                BoardContent(state.social, onNavigateToFriends)
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

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun BoardContent(
    social: LeaderboardViewModel.SocialStatsData,
    onNavigateToFriends: () -> Unit
) {
    if (social.isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Green, strokeWidth = 2.dp)
        }
        return
    }

    var selectedMetric by remember { mutableStateOf(0) }

    val entries = when (selectedMetric) {
        1    -> social.byDistance
        2    -> social.byDuration
        else -> social.bySpeed
    }
    val metricLabel = when (selectedMetric) {
        1    -> "THIS SEASON"
        2    -> "TOTAL TIME"
        else -> "TOP SPEED RUN"
    }

    val podium = entries.take(3)
    val ranked = entries.drop(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { BoardHeader() }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            MetricSegmentedControl(
                selected = selectedMetric,
                onSelect = { selectedMetric = it },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
        if (podium.isNotEmpty()) {
            item { PodiumSection(podium = podium) }
        }
        item { Spacer(Modifier.height(14.dp)) }
        item {
            MilestoneCard(
                social  = social,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        if (ranked.isNotEmpty()) {
            item { Spacer(Modifier.height(16.dp)) }
            item {
                RankedListHeader(
                    metricLabel = metricLabel,
                    modifier    = Modifier.padding(horizontal = 16.dp)
                )
            }
            item { Spacer(Modifier.height(6.dp)) }
            items(ranked) { entry ->
                RankedRow(
                    entry    = entry,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 3.dp)
                )
            }
        }

        if (!social.hasFriends) {
            item {
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onNavigateToFriends,
                        colors  = ButtonDefaults.buttonColors(containerColor = Green),
                        shape   = RoundedCornerShape(10.dp)
                    ) {
                        Text("Find real friends", color = Color.Black, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ── Header ────────────────────────────────────────────────────────────────────

@Composable
private fun BoardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Leaderboard",
                fontSize   = 26.sp,
                fontWeight = FontWeight.Black,
                color      = TxtPrimary,
                lineHeight = 28.sp
            )
            Text(
                "Among friends · June 2026",
                fontSize = 12.sp,
                color    = TxtMuted
            )
        }
        // Season chip
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(Gold.copy(alpha = 0.12f))
                .border(1.dp, Gold.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text("⚡ Season 1", fontSize = 12.sp, color = Gold, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Segmented control ─────────────────────────────────────────────────────────

@Composable
private fun MetricSegmentedControl(
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val labels = listOf("speed", "distance", "duration")
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SegBg)
            .padding(4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { i, label ->
                val isActive = selected == i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isActive) Brush.horizontalGradient(listOf(Green, GreenDeep))
                            else          Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        )
                        .clickable { onSelect(i) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = label,
                        fontSize   = 13.sp,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        color      = if (isActive) Color(0xFF0D1A14) else TxtMuted
                    )
                }
            }
        }
    }
}

// ── Podium ────────────────────────────────────────────────────────────────────

@Composable
private fun PodiumSection(podium: List<LeaderboardViewModel.SocialEntry>) {
    // Renders with 1, 2, or 3 distinct drivers. 1st always stays centered; the 2nd/3rd
    // slots collapse to balancing spacers when there aren't enough real people yet.
    val p1 = podium.getOrNull(0); val p2 = podium.getOrNull(1); val p3 = podium.getOrNull(2)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        // Height wraps the tallest (1st-place) column — a fixed height would squeeze the
        // budget and collapse 1st's pedestal, since the Column gives its last child (the
        // pedestal) only the leftover space. Each avatar carries its own glow (no single
        // background halo behind the whole podium).
        Box(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter),
                verticalAlignment = Alignment.Bottom
            ) {
                // 2nd place (left) — spacer keeps 1st centered when absent
                if (p2 != null) {
                    PodiumColumn(
                        entry         = p2,
                        medalColor    = Silver,
                        medalDark     = SilverDark,
                        pedestalH     = 76,
                        avatarSize    = 58,
                        valueSize     = 19,
                        showCrown     = false,
                        modifier      = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                // 1st place (center, wider)
                if (p1 != null) {
                    PodiumColumn(
                        entry         = p1,
                        medalColor    = Gold,
                        medalDark     = GoldDark,
                        pedestalH     = 104,
                        avatarSize    = 72,
                        valueSize     = 24,
                        showCrown     = true,
                        modifier      = Modifier.weight(1.15f)
                    )
                }
                // 3rd place (right) — spacer keeps 1st centered when absent
                if (p3 != null) {
                    PodiumColumn(
                        entry         = p3,
                        medalColor    = Bronze,
                        medalDark     = BronzeDark,
                        pedestalH     = 60,
                        avatarSize    = 58,
                        valueSize     = 19,
                        showCrown     = false,
                        modifier      = Modifier.weight(1f)
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PodiumColumn(
    entry: LeaderboardViewModel.SocialEntry,
    medalColor: Color,
    medalDark: Color,
    pedestalH: Int,
    avatarSize: Int,
    valueSize: Int,
    showCrown: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier            = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (showCrown) {
            // Minimal 3-peak crown (drawn, not an emoji)
            Box(
                Modifier
                    .size(width = 22.dp, height = 14.dp)
                    .drawBehind {
                        val cw = size.width; val ch = size.height
                        val crown = Path().apply {
                            moveTo(0f, ch)
                            lineTo(cw * 0.04f, ch * 0.28f)
                            lineTo(cw * 0.28f, ch * 0.60f)
                            lineTo(cw * 0.50f, ch * 0.04f)
                            lineTo(cw * 0.72f, ch * 0.60f)
                            lineTo(cw * 0.96f, ch * 0.28f)
                            lineTo(cw, ch)
                            close()
                        }
                        drawPath(crown, Gold)
                    }
            )
            Spacer(Modifier.height(3.dp))
        }

        // Avatar with medal ring, per-avatar glow, and rank badge.
        // Container is NOT clipped so the glow and the bottom badge can overflow.
        Box(
            modifier = Modifier
                .size((avatarSize + 6).dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(medalColor.copy(alpha = 0.55f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.75f
                        ),
                        radius = size.minDimension * 0.75f,
                        center = center
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Ring (medal color)
            Box(Modifier.fillMaxSize().clip(CircleShape).background(medalColor))
            // Avatar (centered → the ring shows as a thin band around it)
            InitialsAvatar(name = entry.displayName, size = avatarSize.dp)
            // Rank badge — center anchored exactly on the avatar's bottom edge for every
            // size. The avatar sits 3dp above the container bottom (it's avatarSize inside
            // an avatarSize+6 box), and align(BottomCenter) puts the badge center at
            // badgeSize/2 above the bottom, so offsetting by (badgeSize/2 - 3) lands the
            // badge center on the edge identically for 1st, 2nd and 3rd.
            val badgeSize = avatarSize * 0.32f
            Box(
                modifier = Modifier
                    .size(badgeSize.dp)
                    .align(Alignment.BottomCenter)
                    .offset(y = (badgeSize / 2f - 3f).dp)
                    .clip(CircleShape)
                    .background(medalColor)
                    .border(2.dp, AppBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${entry.rank}",
                    fontSize   = (avatarSize * 0.16f).sp,
                    fontWeight = FontWeight.Black,
                    color      = Color.Black
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Name
        val displayLabel = if (entry.isCurrentUser) "You" else entry.displayName.split(" ").first()
        Text(
            text       = displayLabel,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            color      = if (entry.isCurrentUser) Green else TxtPrimary,
            maxLines   = 1
        )

        // Value (condensed numerals)
        Text(
            text       = entry.value,
            fontFamily = RobotoCondensedBold,
            fontSize   = valueSize.sp,
            lineHeight = valueSize.sp,
            fontWeight = FontWeight.Bold,
            color      = if (entry.rank == 1) Gold else TxtPrimary,
            maxLines   = 1
        )

        Spacer(Modifier.height(4.dp))

        // Pedestal block
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(pedestalH.dp)
                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                .background(Brush.verticalGradient(listOf(medalColor.copy(alpha = 0.9f), medalDark)))
        ) {
            // Faded rank number inside pedestal (condensed)
            Text(
                text       = "${entry.rank}",
                fontFamily = RobotoCondensedBold,
                fontSize   = 26.sp,
                fontWeight = FontWeight.Bold,
                color      = Color.Black.copy(alpha = 0.28f),
                modifier   = Modifier.align(Alignment.Center)
            )
            // Diagonal shine
            Box(
                Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val path = Path().apply {
                            moveTo(size.width * 0.55f, 0f)
                            lineTo(size.width, 0f)
                            lineTo(size.width * 0.45f, size.height)
                            lineTo(0f, size.height)
                            close()
                        }
                        drawPath(path, Color.White.copy(alpha = 0.07f))
                    }
            )
        }
    }
}

// ── Milestone card ────────────────────────────────────────────────────────────

@Composable
private fun MilestoneCard(
    social: LeaderboardViewModel.SocialStatsData,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(social.milestoneProgress, tween(800), label = "mp")
    val goalKm = "%,.0f km".format(social.nextMilestoneTargetKm)
    val drivenKm = "%,.1f km".format(social.myTotalKm)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Road icon tinted box
                Box(
                    modifier            = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Green.copy(alpha = 0.15f)),
                    contentAlignment    = Alignment.Center
                ) {
                    Icon(Icons.Filled.Route, null, Modifier.size(18.dp), tint = Green)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "You've driven $drivenKm",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = TxtPrimary
                    )
                    Text(
                        "Next milestone · ${social.nextMilestoneName}",
                        fontSize = 11.sp,
                        color    = TxtMuted
                    )
                }
                Text(
                    goalKm,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black,
                    color      = TxtMuted
                )
            }

            // Gradient progress bar with glowing knob
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val barW = maxWidth
                Row(
                    modifier            = Modifier.fillMaxWidth().height(6.dp),
                    verticalAlignment   = Alignment.CenterVertically
                ) {
                    if (progress > 0.01f) {
                        Box(
                            Modifier
                                .weight(progress.coerceAtLeast(0.01f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(GreenDeep, Green)))
                        )
                    }
                    // Glowing knob
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(Green)
                            .border(2.dp, Green.copy(alpha = 0.35f), CircleShape)
                    )
                    if (progress < 0.99f) {
                        Box(
                            Modifier
                                .weight((1f - progress).coerceAtLeast(0.01f))
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(InsetBg)
                        )
                    }
                }
            }

            // Footer chips
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (social.mostActiveFriend != null) {
                    MilestoneChip("👥 Most active · ${social.mostActiveFriend}")
                }
                MilestoneChip("🔥 4-day streak")
            }
        }
    }
}

@Composable
private fun MilestoneChip(label: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(InsetBg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, color = TxtMuted)
    }
}

// ── Ranked list ───────────────────────────────────────────────────────────────

@Composable
private fun RankedListHeader(metricLabel: String, modifier: Modifier = Modifier) {
    Row(
        modifier            = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("RANK", fontSize = 10.sp, color = TxtDimmer, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp)
        Text(metricLabel, fontSize = 10.sp, color = TxtDimmer, fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp)
    }
}

@Composable
private fun RankedRow(
    entry: LeaderboardViewModel.SocialEntry,
    modifier: Modifier = Modifier
) {
    val bgModifier = if (entry.isCurrentUser) {
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Green.copy(alpha = 0.12f), CardBg),
                    startX = 0f, endX = 400f
                )
            )
            .border(1.dp, Green.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
    } else {
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(CardBg)
    }

    Row(
        modifier            = modifier.fillMaxWidth().then(bgModifier).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment   = Alignment.CenterVertically
    ) {
        // Rank number (condensed)
        Text(
            "${entry.rank}",
            fontFamily = RobotoCondensedBold,
            fontSize   = 17.sp,
            fontWeight = FontWeight.Bold,
            color      = if (entry.isCurrentUser) Green else TxtDimmer,
            textAlign  = TextAlign.Center,
            modifier   = Modifier.width(22.dp)
        )
        Spacer(Modifier.width(10.dp))

        // Avatar
        InitialsAvatar(name = entry.displayName, size = 36.dp)
        Spacer(Modifier.width(10.dp))

        // Name + route
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    entry.displayName,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = if (entry.isCurrentUser) Green else TxtPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis
                )
                if (entry.isCurrentUser) YouTag()
            }
            Text(
                entry.tripLabel,
                fontSize = 11.sp,
                color    = TxtMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.width(8.dp))

        // Value + trend pill (condensed numerals, stacked)
        Column(
            horizontalAlignment = Alignment.End,
            modifier            = Modifier.widthIn(min = 64.dp)
        ) {
            Text(
                entry.value,
                fontFamily = RobotoCondensedBold,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                color      = TxtPrimary,
                maxLines   = 1
            )
            Spacer(Modifier.height(2.dp))
            TrendPill(entry.trend)
        }
    }
}

// ── Shared small components ───────────────────────────────────────────────────

@Composable
fun TrendPill(trend: Int?) {
    val (text, bg, fg) = when {
        trend == null || trend == 0 -> Triple("—", Color.White.copy(alpha = 0.06f), TxtDimmer)
        trend > 0 -> Triple("▲ $trend", Green.copy(alpha = 0.15f), Green)
        else      -> Triple("▼ ${-trend}", Danger.copy(alpha = 0.15f), Danger)
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 10.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun YouTag() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Green.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text("YOU", fontSize = 9.sp, color = Green, fontWeight = FontWeight.Black, letterSpacing = 0.5.sp)
    }
}

@Composable
fun InitialsAvatar(
    name: String,
    size: androidx.compose.ui.unit.Dp,
    photoUrl: String? = null,
    modifier: Modifier = Modifier
) {
    val initial  = name.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
    val bgColor  = avatarColor(name)
    val fontSize = (size.value * 0.38f).sp

    Box(
        modifier            = modifier.size(size).clip(CircleShape).background(bgColor),
        contentAlignment    = Alignment.Center
    ) {
        if (photoUrl != null) {
            coil.compose.AsyncImage(
                model              = photoUrl,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = androidx.compose.ui.layout.ContentScale.Crop
            )
        } else {
            Text(initial, fontSize = fontSize, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
