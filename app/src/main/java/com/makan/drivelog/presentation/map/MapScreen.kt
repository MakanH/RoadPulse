package com.makan.drivelog.presentation.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.TealPrimary

@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
    onTripClick: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // ── Map area (55% of screen height) ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f)
        ) {
            LocationPermissionHandler {
                val state = uiState
                if (state is MapViewModel.UiState.Success) {
                    MapLibreMapView(
                        modifier = Modifier.fillMaxSize(),
                        tripsWithPoints = state.monthlyTripsWithPoints,
                        trackingState   = state.trackingState,
                        monthLabel      = state.monthLabel,
                        onTripClick     = onTripClick
                    )
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
                            onTripClick = onTripClick
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
