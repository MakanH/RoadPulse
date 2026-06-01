package com.makan.drivelog.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.domain.model.Group
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.presentation.components.OnboardingPopup
import com.makan.drivelog.presentation.onboarding.OnboardingViewModel
import com.makan.drivelog.presentation.profile.SettingsViewModel
import com.makan.drivelog.presentation.theme.TealDark
import com.makan.drivelog.presentation.theme.TealPrimary
import com.makan.drivelog.presentation.trips.TripRenameDialog

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel(),
    onTripClick: (Long) -> Unit = {},
    onOpenGroupMap: ((groupId: String, sessionId: String) -> Unit)? = null
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val mapStylePref   by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val onboardingSeen by onboardingVm.mapSeen.collectAsStateWithLifecycle(true)
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<Trip?>(null) }

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
                onClick = {
                    onOpenGroupMap(activeGroup.id, activeGroup.activeSessionId!!)
                }
            )
        }

        // ── Map area (55% of screen height) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            LocationPermissionHandler {
                val state = uiState
                if (state is MapViewModel.UiState.Success) {
                    CompositionLocalProvider(LocalMapStyleUrl provides getMapStyleUrl(mapStylePref)) {
                        MapLibreMapView(
                            modifier        = Modifier.fillMaxSize(),
                            tripsWithPoints = state.monthlyTripsWithPoints,
                            trackingState   = state.trackingState,
                            monthLabel      = state.monthLabel,
                            onTripClick     = onTripClick
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = TealPrimary)
                    }
                }

                // FAB always on top of map area when permission is granted
                val isTracking = (uiState as? MapViewModel.UiState.Success)
                    ?.trackingState?.isTracking ?: false
                TrackingFab(
                    isTracking = isTracking,
                    onToggle = {
                        if (isTracking) viewModel.stopTracking(context)
                        else viewModel.startTracking(context)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
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
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
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
                                trips = state.recentTrips,
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
    } // end Box
}

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
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            "Driving with ${group.name} · ${group.memberUids.size} members",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}
