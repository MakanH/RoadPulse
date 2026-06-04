package com.makan.roadpulse.presentation.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.Trip
import com.makan.roadpulse.data.service.NotificationService
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.notifications.NotificationViewModel
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel
import com.makan.roadpulse.presentation.profile.SettingsViewModel
import com.makan.roadpulse.presentation.theme.TealDark
import com.makan.roadpulse.presentation.theme.TealPrimary
import com.makan.roadpulse.presentation.trips.TripRenameDialog
import com.makan.roadpulse.presentation.util.LocalUnitPrefs

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel(),
    notifViewModel: NotificationViewModel = hiltViewModel(),
    onTripClick: (Long) -> Unit = {},
    onOpenGroupMap: ((groupId: String, sessionId: String) -> Unit)? = null,
    onNavigateToFriends: () -> Unit = {},
    onNavigateToGroup: (groupId: String) -> Unit = {}
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val isHeatmapMode  by viewModel.isHeatmapMode.collectAsStateWithLifecycle()
    val mapStylePref   by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val onboardingSeen by onboardingVm.mapSeen.collectAsStateWithLifecycle(true)
    val notifications  by notifViewModel.notifications.collectAsStateWithLifecycle()
    val unreadCount    by notifViewModel.unreadCount.collectAsStateWithLifecycle()
    var renameTarget by remember { mutableStateOf<Trip?>(null) }
    var recenterTrigger by remember { mutableStateOf(0) }
    var showNotifSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Cache the last successful state so the UI never flashes blank during a brief
    // Loading re-emission (e.g. after ViewModel recreation on a backstack re-entry).
    var lastSuccessState by remember { mutableStateOf<MapViewModel.UiState.Success?>(null) }
    val successState = (uiState as? MapViewModel.UiState.Success)
        ?.also { lastSuccessState = it }
        ?: lastSuccessState

    renameTarget?.let { trip ->
        TripRenameDialog(
            currentName = trip.name,
            onDismiss   = { renameTarget = null },
            onSave      = { newName -> viewModel.renameTrip(trip.id, newName); renameTarget = null }
        )
    }

    Box(Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ── Active group banner ───────────────────────────────────────────────
        val activeGroup = successState?.activeGroup
        if (activeGroup != null && onOpenGroupMap != null) {
            ActiveGroupBanner(
                group = activeGroup,
                onClick = { onOpenGroupMap(activeGroup.id, activeGroup.activeSessionId!!) }
            )
        }

        // ── Map area (55% of screen height) ──────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(0.55f)) {
            LocationPermissionHandler {
                if (successState != null) {
                    CompositionLocalProvider(LocalMapStyleUrl provides getMapStyleUrl(mapStylePref)) {
                        MapLibreMapView(
                            modifier          = Modifier.fillMaxSize(),
                            tripsWithPoints   = successState.monthlyTripsWithPoints,
                            trackingState     = successState.trackingState,
                            monthLabel        = successState.monthLabel,
                            isHeatmapMode     = isHeatmapMode,
                            onTripClick       = onTripClick,
                            recenterTrigger   = recenterTrigger,
                            onRecenterFailed  = {
                                scope.launch { snackbarHostState.showSnackbar("Location unavailable") }
                            }
                        )
                    }

                    // Bottom-left: collapsible speed legend (heatmap mode only)
                    if (isHeatmapMode) {
                        HeatmapLegend(
                            speedUnit = LocalUnitPrefs.current.speedUnit,
                            modifier  = Modifier.align(Alignment.BottomStart).padding(12.dp)
                        )
                    }

                    // Bottom-right: recenter FAB
                    RecenterFab(
                        onClick  = { recenterTrigger++ },
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = TealPrimary) }
                }
            }

            // Top-right: notification bell + heatmap toggle stacked
            // (outside LocationPermissionHandler so bell is always visible)
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 12.dp, top = 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (successState != null) {
                    HeatmapToggleButton(
                        isHeatmapMode = isHeatmapMode,
                        onToggle      = viewModel::toggleHeatmap
                    )
                }
                NotificationBellButton(
                    hasUnread = unreadCount > 0,
                    onClick   = { showNotifSheet = true }
                )
            }
        }

        // ── Scrollable content (45% of screen height) ────────────────────────
        // successState is non-null whenever we have data — current or cached from the
        // last Success emission — so the bottom half never flashes blank on tab return.
        if (successState != null) {
            if (successState.monthlyTripsWithPoints.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No trips yet this month — start your first drive!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    item {
                        StatCardsRow(stats = successState.todayStats)
                        Spacer(Modifier.height(16.dp))
                    }
                    item {
                        RecentTripsList(
                            trips       = successState.recentTrips,
                            onTripClick = onTripClick,
                            onLongPress = { renameTarget = it }
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        } else {
            // Error or first load with no cached data yet
            val errorMessage = (uiState as? MapViewModel.UiState.Error)?.message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.45f)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                if (errorMessage != null) {
                    Text(
                        errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    CircularProgressIndicator(color = TealPrimary)
                }
            }
        }
    } // end Column

    if (showNotifSheet) {
        NotificationSheet(
            notifications     = notifications,
            onDismiss         = { showNotifSheet = false },
            onMarkAllRead     = {
                notifViewModel.markAllAsRead()
                showNotifSheet = false
            },
            onNotificationTap = { notif ->
                notifViewModel.markAsRead(notif.id)
                showNotifSheet = false
                when (notif.type) {
                    NotificationService.TYPE_FRIEND_REQUEST,
                    NotificationService.TYPE_FRIEND_ACCEPTED -> onNavigateToFriends()
                    NotificationService.TYPE_GROUP_INVITE -> {
                        notif.actionData["groupId"]?.let { onNavigateToGroup(it) }
                    }
                    NotificationService.TYPE_GROUP_DRIVE_STARTED -> {
                        val gid = notif.actionData["groupId"]
                        val sid = notif.actionData["sessionId"]
                        if (gid != null && sid != null) onOpenGroupMap?.invoke(gid, sid)
                    }
                }
            }
        )
    }

    if (!onboardingSeen) {
        OnboardingPopup(
            icon      = Icons.Filled.Map,
            title     = "Your driving map",
            body      = "All your trips appear here as colored routes. Tap any route to see details. Hit the play button to start tracking a drive — the app will also detect trips automatically.",
            onDismiss = onboardingVm::markMapSeen
        )
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier  = Modifier.align(Alignment.BottomCenter)
    )
    } // end Box
}

// ── Heatmap toggle pill ───────────────────────────────────────────────────────

@Composable
private fun HeatmapToggleButton(
    isHeatmapMode: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isHeatmapMode) TealPrimary
                  else MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    val fgColor = if (isHeatmapMode) Color.White
                  else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

    Surface(
        modifier      = modifier.clickable { onToggle() },
        shape         = RoundedCornerShape(20.dp),
        color         = bgColor,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Layers,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = fgColor
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text       = if (isHeatmapMode) "Heatmap" else "Routes",
                fontSize   = 12.sp,
                color      = fgColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ── Speed legend ──────────────────────────────────────────────────────────────

private val LEGEND_COLORS = listOf(
    Color(0xFF97C459), // green     0–30
    Color(0xFFEF9F27), // amber    30–60
    Color(0xFFD85A30), // orange   60–90
    Color(0xFFE24B4A), // red      90–120
    Color(0xFFA32D2D), // deep red 120–150
    Color(0xFF7F77DD), // purple   150–180
    Color(0xFFD4537E)  // pink     180+
)

@Composable
private fun HeatmapLegend(speedUnit: String, modifier: Modifier = Modifier) {
    val isMph = speedUnit == "mph"
    val labels = if (isMph)
        listOf("< 19", "19–37", "37–56", "56–75", "75–93", "93–112", "112+")
    else
        listOf("< 30", "30–60", "60–90", "90–120", "120–150", "150–180", "180+")
    val unitLabel = if (isMph) "mph" else "km/h"
    var expanded by remember { mutableStateOf(true) }

    Surface(
        modifier = modifier
            .width(108.dp)
            .clickable { expanded = !expanded },
        shape    = RoundedCornerShape(8.dp),
        color    = Color(0xFF1A1A2E).copy(alpha = 0.85f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            // Header: "Legend" label + collapse/expand arrow
            Row(
                verticalAlignment      = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Legend", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(12.dp))
                Text(
                    text     = if (expanded) "▼" else "▲",
                    fontSize = 9.sp,
                    color    = Color.White.copy(alpha = 0.5f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier            = Modifier.padding(top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    labels.forEachIndexed { i, label ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(LEGEND_COLORS[i])
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(label, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                        }
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(unitLabel, fontSize = 9.sp, color = Color.White.copy(alpha = 0.45f))
                }
            }
        }
    }
}

// ── Active group banner ───────────────────────────────────────────────────────

@Composable
private fun ActiveGroupBanner(group: Group, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TealDark)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Group,
            contentDescription = null,
            tint     = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Driving with ${group.name} · ${group.memberUids.size} members",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = Color.White
        )
    }
}
