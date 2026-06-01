package com.makan.drivelog.presentation.tripdetail

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.domain.model.LocationPoint
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.domain.model.TripStats
import com.makan.drivelog.presentation.map.getMapStyleUrl
import com.makan.drivelog.presentation.profile.SettingsViewModel
import com.makan.drivelog.presentation.util.LocalUnitPrefs
import com.makan.drivelog.presentation.theme.CoralPrimary
import com.makan.drivelog.presentation.theme.TealPrimary
import com.makan.drivelog.presentation.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun TripDetailScreen(
    tripId: Long,
    viewModel: TripDetailViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val uiState      by viewModel.uiState.collectAsStateWithLifecycle()
    val scrubIndex   by viewModel.scrubIndex.collectAsStateWithLifecycle()
    val mapStylePref by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")

    when (val state = uiState) {
        is TripDetailViewModel.UiState.Loading -> {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TealPrimary)
            }
        }
        is TripDetailViewModel.UiState.Error -> {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
                .padding(16.dp), contentAlignment = Alignment.Center) {
                Text(state.message, color = MaterialTheme.colorScheme.error)
            }
        }
        is TripDetailViewModel.UiState.Success -> {
            TripDetailContent(
                trip        = state.trip,
                points      = state.points,
                stats       = state.stats,
                segments    = state.segments,
                scrubIndex  = scrubIndex,
                mapStyleUrl = getMapStyleUrl(mapStylePref),
                onScrub     = viewModel::onScrub,
                onRename    = viewModel::renameTrip,
                onBack      = onBack
            )
        }
    }
}

@Composable
private fun TripDetailContent(
    trip: Trip,
    points: List<LocationPoint>,
    stats: TripStats,
    segments: List<TripSegment>,
    scrubIndex: Int,
    mapStyleUrl: String,
    onScrub: (Int) -> Unit,
    onRename: (String) -> Unit,
    onBack: () -> Unit
) {
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        RenameDialog(
            currentName = trip.name,
            onDismiss = { showRenameDialog = false },
            onSave = { newName -> onRename(newName); showRenameDialog = false }
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // ── Map section (35% of screen) ──────────────────────────────────────
        Box(modifier = Modifier.fillMaxWidth().weight(0.35f)) {
            TripDetailMapView(
                modifier   = Modifier.fillMaxSize(),
                points     = points,
                scrubIndex = scrubIndex,
                styleUrl   = mapStyleUrl
            )
            TopBar(trip = trip, onBack = onBack, onRenameClick = { showRenameDialog = true })
        }

        // ── Scrollable content (65%) ──────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(0.65f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            item { SpeedScrubberSection(points, scrubIndex, onScrub, trip) }
            item { Spacer(Modifier.height(16.dp)) }
            item { DetailStatCards(stats) }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                Text("Speed over route", fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp))
                SpeedGraph(points = points, scrubIndex = scrubIndex)
            }
            if (segments.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)) }
                item { SegmentsBreakdown(segments) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

// ── Top bar overlay ───────────────────────────────────────────────────────────

@Composable
private fun TopBar(trip: Trip, onBack: () -> Unit, onRenameClick: () -> Unit) {
    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(trip.startTime))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Text(
            text = trip.name,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 56.dp)
                .clickable { onRenameClick() },
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = dateStr,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 12.dp),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun RenameDialog(currentName: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename trip") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onSave(text) }, enabled = text.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Speed scrubber ────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedScrubberSection(
    points: List<LocationPoint>,
    scrubIndex: Int,
    onScrub: (Int) -> Unit,
    trip: Trip
) {
    val units = LocalUnitPrefs.current
    val currentSpeedDisplay = if (points.isNotEmpty() && scrubIndex < points.size)
        units.formatSpeed(points[scrubIndex].speed) else "0 ${units.speedLabel()}"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DragHandle, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Drag to scrub speed along route", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(Modifier.height(4.dp))
            Text(currentSpeedDisplay, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                color = TealPrimary)

            Slider(
                value = if (points.size > 1) scrubIndex.toFloat() / (points.size - 1) else 0f,
                onValueChange = { frac ->
                    if (points.size > 1) onScrub((frac * (points.size - 1)).roundToInt())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor       = TealPrimary,
                    activeTrackColor = TealPrimary,
                    inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Départ ${timeOf(trip.startTime)}", fontSize = 10.sp, color = TextTertiary)
                Text("Arrivée ${timeOf(trip.endTime ?: trip.startTime)}", fontSize = 10.sp, color = TextTertiary)
            }
        }
    }
}

// ── Stat cards ────────────────────────────────────────────────────────────────

@Composable
private fun DetailStatCards(stats: TripStats) {
    val units = LocalUnitPrefs.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DetailStatCard("Distance",  units.distanceValueM(stats.distance), units.distanceLabel(), Color(0xFF5DCAA5), Modifier.weight(1f))
        DetailStatCard("Avg speed", units.speedValueMs(stats.avgSpeed),   units.speedLabel(),    Color(0xFF378ADD), Modifier.weight(1f))
        DetailStatCard("Top speed", units.speedValueMs(stats.topSpeed),   units.speedLabel(),    Color(0xFFA32D2D), Modifier.weight(1f))
    }
}

@Composable
private fun DetailStatCard(
    label: String, value: String, unit: String,
    valueColor: Color, modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.W500, color = valueColor,
                textAlign = TextAlign.Center)
            Text(text = unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center)
        }
    }
}

// ── Segments breakdown ────────────────────────────────────────────────────────

private val segmentColors = listOf(Color(0xFF5DCAA5), Color(0xFF378ADD), Color(0xFFD85A30))

@Composable
private fun SegmentsBreakdown(segments: List<TripSegment>) {
    val units = LocalUnitPrefs.current
    Column {
        Text("Route segments", fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                segments.forEach { seg ->
                    val color = segmentColors.getOrElse(seg.accentIndex) { CoralPrimary }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                        Spacer(Modifier.width(10.dp))
                        Text(seg.label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f))
                        Text(units.formatDistanceKm(seg.distanceKm), fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 12.dp))
                        Text(units.formatSpeedKmh(seg.avgSpeedKmh), fontSize = 12.sp, color = color)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun timeOf(epochMs: Long) =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMs))
