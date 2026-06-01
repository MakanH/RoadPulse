package com.makan.roadpulse.presentation.navigate

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.roadpulse.data.service.SearchResult
import com.makan.roadpulse.domain.model.Route
import com.makan.roadpulse.domain.model.RouteStep
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.map.LocationPermissionHandler
import com.makan.roadpulse.presentation.map.getMapStyleUrl
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel
import com.makan.roadpulse.presentation.profile.SettingsViewModel
import com.makan.roadpulse.presentation.theme.CoralPrimary
import com.makan.roadpulse.presentation.theme.TealPrimary
import com.makan.roadpulse.presentation.util.LocalUnitPrefs
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NavigateScreen(
    viewModel: NavigateViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mapStylePref   by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val onboardingSeen by onboardingVm.navigateSeen.collectAsStateWithLifecycle(true)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearError()
    }

    LocationPermissionHandler {
        Box(Modifier.fillMaxSize()) {

            // Always-visible map
            NavigateMapView(
                modifier     = Modifier.fillMaxSize(),
                route        = state.route,
                isNavigating = state.navState == NavState.NAVIGATING,
                styleUrl     = getMapStyleUrl(mapStylePref)
            )

            // Top overlays
            when (state.navState) {
                NavState.NAVIGATING -> {
                    state.route?.steps?.getOrNull(state.currentStepIndex)?.let { step ->
                        InstructionBanner(
                            step            = step,
                            distanceToTurn  = state.distanceToNextTurn,
                            modifier        = Modifier.align(Alignment.TopCenter)
                        )
                    }
                }
                NavState.ERROR -> {
                    ErrorBanner(
                        message  = state.errorMessage ?: "Route error",
                        onDismiss = viewModel::onClearDestination,
                        modifier  = Modifier.align(Alignment.TopCenter)
                    )
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        SearchBar(
                            query           = state.query,
                            isLoading       = state.navState == NavState.ROUTING,
                            showClear       = state.navState == NavState.ROUTING ||
                                              state.navState == NavState.ROUTE_PREVIEW,
                            onQueryChange   = viewModel::onQueryChange,
                            onClear         = viewModel::onClearDestination
                        )
                        if (state.navState == NavState.SEARCHING && state.searchResults.isNotEmpty()) {
                            SearchResultsList(
                                results  = state.searchResults,
                                onSelect = viewModel::onSelectDestination
                            )
                        }
                    }
                }
            }

            // Bottom overlays
            when (state.navState) {
                NavState.ROUTE_PREVIEW -> state.route?.let { route ->
                    RoutePreviewCard(
                        destName  = state.selectedShortName,
                        route     = route,
                        onStart   = viewModel::onStartNavigation,
                        onCancel  = viewModel::onClearDestination,
                        modifier  = Modifier.align(Alignment.BottomCenter)
                    )
                }
                NavState.NAVIGATING -> state.route?.let { route ->
                    NavigationBottomBar(
                        route            = route,
                        currentStepIndex = state.currentStepIndex,
                        onEnd            = viewModel::onEndNavigation,
                        modifier         = Modifier.align(Alignment.BottomCenter)
                    )
                }
                else -> {}
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
            if (!onboardingSeen) {
                OnboardingPopup(
                    icon      = Icons.AutoMirrored.Filled.ArrowForward,
                    title     = "Turn-by-turn navigation",
                    body      = "Search for a destination and get driving directions. Your trip is automatically tracked while navigating, so it shows up in your history too.",
                    onDismiss = onboardingVm::markNavigateSeen
                )
            }
        }
    }
}

// ── Search bar ────────────────────────────────────────────────────────────────

@Composable
private fun SearchBar(
    query: String,
    isLoading: Boolean,
    showClear: Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape    = RoundedCornerShape(28.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Where to?", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color    = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            when {
                isLoading -> CircularProgressIndicator(
                    modifier   = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color      = TealPrimary
                )
                showClear -> IconButton(onClick = onClear, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

// ── Search results ────────────────────────────────────────────────────────────

@Composable
private fun SearchResultsList(results: List<SearchResult>, onSelect: (SearchResult) -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column {
            results.forEachIndexed { index, result ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(result) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null,
                        tint    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(result.shortName, fontSize = 14.sp,
                            color   = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(result.displayName, fontSize = 12.sp,
                            color   = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (index < results.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                }
            }
        }
    }
}

// ── Route preview card ────────────────────────────────────────────────────────

@Composable
private fun RoutePreviewCard(
    destName: String,
    route: Route,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val units = LocalUnitPrefs.current
    Card(
        modifier  = modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(destName.ifBlank { "Destination" }, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(
                "${units.formatDistanceM(route.distanceMeters)} · ${formatDuration(route.durationSeconds)}",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                colors   = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape    = RoundedCornerShape(12.dp)
            ) {
                Text("Start navigation", color = Color.White, fontSize = 15.sp)
            }
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Instruction banner ────────────────────────────────────────────────────────

@Composable
private fun InstructionBanner(
    step: RouteStep,
    distanceToTurn: Double,
    modifier: Modifier = Modifier
) {
    val units = LocalUnitPrefs.current
    Card(
        modifier  = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.78f)),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector  = maneuverIcon(step.type),
                contentDescription = null,
                tint         = Color.White,
                modifier     = Modifier.size(34.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(step.instruction, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(units.formatDistanceM(distanceToTurn), fontSize = 14.sp, color = TealPrimary)
            }
        }
    }
}

// ── Navigation bottom bar ─────────────────────────────────────────────────────

@Composable
private fun NavigationBottomBar(
    route: Route,
    currentStepIndex: Int,
    onEnd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val units     = LocalUnitPrefs.current
    val remaining = route.steps.drop(currentStepIndex)
    val remDist   = remaining.sumOf { it.distanceMeters }
    val remSecs   = remaining.sumOf { it.durationSeconds }
    val eta       = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(System.currentTimeMillis() + (remSecs * 1000).toLong()))

    Card(
        modifier  = modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("ETA $eta", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${units.formatDistanceM(remDist)} · ${formatDuration(remSecs)}",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onEnd,
                colors  = ButtonDefaults.buttonColors(containerColor = CoralPrimary),
                shape   = RoundedCornerShape(8.dp)
            ) {
                Text("End", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ── Error banner ──────────────────────────────────────────────────────────────

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.toInt()} m" else "%.1f km".format(meters / 1000)

private fun formatDuration(seconds: Double): String {
    val h = (seconds / 3600).toInt()
    val m = ((seconds % 3600) / 60).toInt()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

/** Maps ORS step type integers to directional icons. */
private fun maneuverIcon(type: Int): ImageVector = when (type) {
    0, 2, 4, 12 -> Icons.AutoMirrored.Filled.ArrowBack   // left family
    1, 3, 5, 13 -> Icons.AutoMirrored.Filled.ArrowForward // right family
    7           -> Icons.Filled.RotateRight               // roundabout enter
    8           -> Icons.Filled.RotateLeft                // roundabout exit
    9           -> Icons.Filled.RotateLeft                // u-turn
    10          -> Icons.Filled.Flag                      // arrive
    else        -> Icons.Filled.ArrowUpward               // 6=straight, 11=depart, default
}
