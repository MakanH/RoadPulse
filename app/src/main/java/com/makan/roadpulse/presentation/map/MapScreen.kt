package com.makan.roadpulse.presentation.map

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.roadpulse.domain.model.Group
import com.makan.roadpulse.domain.model.Trip
import com.makan.roadpulse.presentation.components.OnboardingPopup
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
    onTripClick: (Long) -> Unit = {},
    onOpenGroupMap: ((groupId: String, sessionId: String) -> Unit)? = null
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val isHeatmapMode  by viewModel.isHeatmapMode.collectAsStateWithLifecycle()
    val mapStylePref   by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val onboardingSeen by onboardingVm.mapSeen.collectAsStateWithLifecycle(true)
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<Trip?>(null) }
    var recenterTrigger by remember { mutableStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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
        val activeGroup = (uiState as? MapViewModel.UiState.Success)?.activeGroup
        if (activeGroup != null && onOpenGroupMap != null) {
            ActiveGroupBanner(
                group = activeGroup,
                onClick = { onOpenGroupMap(activeGroup.id, activeGroup.activeSessionId!!) }
            )
        }

        // ── Map area (55% of screen height) ──────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(0.55f)) {
            LocationPermissionHandler {
                val state = uiState
                if (state is MapViewModel.UiState.Success) {
                    CompositionLocalProvider(LocalMapStyleUrl provides getMapStyleUrl(mapStylePref)) {
                        MapLibreMapView(
                            modifier          = Modifier.fillMaxSize(),
                            tripsWithPoints   = state.monthlyTripsWithPoints,
                            trackingState     = state.trackingState,
                            monthLabel        = state.monthLabel,
                            isHeatmapMode     = isHeatmapMode,
                            onTripClick       = onTripClick,
                            recenterTrigger   = recenterTrigger,
                            onRecenterFailed  = {
                                scope.launch { snackbarHostState.showSnackbar("Location unavailable") }
                            }
                        )
                    }

                    // Heatmap toggle pill — top-right
                    HeatmapToggleButton(
                        isHeatmapMode = isHeatmapMode,
                        onToggle      = viewModel::toggleHeatmap,
                        modifier      = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    )

                    // Bottom-left: speed legend (heatmap mode) stacked above recenter FAB
                    Column(
                        modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        if (isHeatmapMode) {
                            HeatmapLegend(speedUnit = LocalUnitPrefs.current.speedUnit)
                        }
                        RecenterFab(onClick = { recenterTrigger++ })
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = TealPrimary) }
                }

                // FAB — always visible when permission granted
                val isTracking = (uiState as? MapViewModel.UiState.Success)
                    ?.trackingState?.isTracking ?: false
                TrackingFab(
                    isTracking = isTracking,
                    onToggle = {
                        if (isTracking) viewModel.stopTracking(context)
                        else viewModel.startTracking(context)
                    },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                )
            }
        }

        // ── Scrollable content (45% of screen height) ────────────────────────
        when (val state = uiState) {
            is MapViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = TealPrimary) }
            }

            is MapViewModel.UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.45f)
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) { Text(state.message, color = MaterialTheme.colorScheme.error) }
            }

            is MapViewModel.UiState.Success -> {
                if (state.monthlyTripsWithPoints.isEmpty()) {
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
                            StatCardsRow(stats = state.todayStats)
                            Spacer(Modifier.height(16.dp))
                        }
                        item {
                            RecentTripsList(
                                trips       = state.recentTrips,
                                onTripClick = onTripClick,
                                onLongPress = { renameTarget = it }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    } // end Column

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

    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(8.dp),
        color    = Color.Black.copy(alpha = 0.58f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                    Text(
                        text     = label,
                        fontSize = 10.sp,
                        color    = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text     = unitLabel,
                fontSize = 9.sp,
                color    = Color.White.copy(alpha = 0.45f)
            )
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
