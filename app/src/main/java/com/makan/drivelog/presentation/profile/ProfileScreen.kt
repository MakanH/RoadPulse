package com.makan.drivelog.presentation.profile

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.CoralPrimary
import com.makan.drivelog.presentation.theme.TealPrimary

@Composable
fun ProfileScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete all trips?") },
            text  = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAllData(); showDeleteDialog = false }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    val state = uiState
    if (state == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = TealPrimary) }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { Spacer(Modifier.height(28.dp)) }
        item { AvatarSection(state.stats.memberSince) }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionHeader("Lifetime stats") }
        item { LifetimeStatsCard(state.stats) }
        item { Spacer(Modifier.height(20.dp)) }
        item { SectionHeader("Settings") }
        item { SettingsCard(state.settings, viewModel) }
        item { Spacer(Modifier.height(20.dp)) }
        item { DangerZone(onDelete = { showDeleteDialog = true }) }
    }
}

// ── Avatar ────────────────────────────────────────────────────────────────────

@Composable
private fun AvatarSection(memberSince: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Person, contentDescription = null,
                modifier = Modifier.size(40.dp), tint = TealPrimary)
        }
        Spacer(Modifier.height(12.dp))
        Text("Driver", fontSize = 16.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(4.dp))
        Text("Member since $memberSince", fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Lifetime stats ────────────────────────────────────────────────────────────

@Composable
private fun LifetimeStatsCard(stats: ProfileViewModel.LifetimeStats) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Total trips",    stats.totalTrips.toString(),                        Modifier.weight(1f))
                StatCell("Total distance", "%.1f km".format(stats.totalDistanceKm),            Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Drive time",     formatHours(stats.totalDrivingHours),               Modifier.weight(1f))
                StatCell("Avg trips/week", "%.1f".format(stats.avgTripsPerWeek),               Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Avg trip dist.", "%.1f km".format(stats.avgTripDistanceKm),          Modifier.weight(1f))
                StatCell("Top speed",     "${stats.topSpeedKmh} km/h",                        Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatHours(hours: Double): String {
    val h = hours.toLong()
    val m = ((hours - h) * 60).toLong()
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

// ── Settings ──────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
}

@Composable
private fun SettingsCard(settings: ProfileViewModel.Settings, viewModel: ProfileViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            ToggleRow(
                icon        = Icons.Filled.GpsFixed,
                name        = "Auto-tracking",
                description = "Automatically detect and log trips",
                checked     = settings.autoTracking,
                onToggle    = viewModel::setAutoTracking
            )
            RowDivider()
            ChevronRow(
                icon         = Icons.Filled.Speed,
                name         = "Speed unit",
                description  = "Display speed in",
                currentValue = settings.speedUnit,
                onClick      = viewModel::cycleSpeedUnit
            )
            RowDivider()
            ChevronRow(
                icon         = Icons.Filled.Straighten,
                name         = "Distance unit",
                description  = "Display distance in",
                currentValue = settings.distanceUnit,
                onClick      = viewModel::cycleDistanceUnit
            )
            RowDivider()
            ToggleRow(
                icon        = Icons.Filled.Notifications,
                name        = "Background notification",
                description = "Show notification while tracking",
                checked     = settings.backgroundNotification,
                onToggle    = viewModel::setBackgroundNotification
            )
        }
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    name: String,
    description: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor  = Color.White,
                checkedTrackColor  = TealPrimary
            )
        )
    }
}

@Composable
private fun ChevronRow(
    icon: ImageVector,
    name: String,
    description: String,
    currentValue: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(currentValue, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TealPrimary)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        modifier  = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    )
}

// ── Danger zone ───────────────────────────────────────────────────────────────

@Composable
private fun DangerZone(onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDelete() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null,
                tint = CoralPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Delete all data", fontSize = 14.sp, color = CoralPrimary)
        }
    }
}
