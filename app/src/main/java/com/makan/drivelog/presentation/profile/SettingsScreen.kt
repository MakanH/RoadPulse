package com.makan.drivelog.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.presentation.theme.CoralPrimary
import com.makan.drivelog.presentation.theme.TealPrimary
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth as fmw

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToLicenses: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme              by viewModel.prefs.theme.collectAsStateWithLifecycle("dark")
    val mapStyle           by viewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val autoTracking       by viewModel.prefs.autoTracking.collectAsStateWithLifecycle(true)
    val gpsAccuracy        by viewModel.prefs.gpsAccuracy.collectAsStateWithLifecycle("high")
    val minTripDist        by viewModel.prefs.minTripDistance.collectAsStateWithLifecycle("100m")
    val speedUnit          by viewModel.prefs.speedUnit.collectAsStateWithLifecycle("km/h")
    val distanceUnit       by viewModel.prefs.distanceUnit.collectAsStateWithLifecycle("km")
    val bgNotif            by viewModel.prefs.backgroundNotification.collectAsStateWithLifecycle(true)
    val tripSummaryNotif   by viewModel.prefs.tripSummaryNotification.collectAsStateWithLifecycle(true)
    val friendActNotif     by viewModel.prefs.friendActivityNotification.collectAsStateWithLifecycle(true)
    val shareLoc           by viewModel.prefs.shareLiveLocation.collectAsStateWithLifecycle(true)
    val profileVis         by viewModel.prefs.profileVisibility.collectAsStateWithLifecycle("everyone")

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.snackbar.collect { snackbarHostState.showSnackbar(it) } }

    var showSignOutDialog    by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var showDeleteAcctDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) ConfirmDialog("Sign out?", "You will be returned to the login screen.",
        "Sign out", { viewModel.signOut(); showSignOutDialog = false }, { showSignOutDialog = false })
    if (showDeleteDataDialog) ConfirmDialog("Delete all trips?", "This cannot be undone.",
        "Delete", { viewModel.deleteAllData(); showDeleteDataDialog = false }, { showDeleteDataDialog = false })
    if (showDeleteAcctDialog) ConfirmDialog("Delete account?",
        "Permanently deletes your account and all trip data. This cannot be undone.",
        "Delete account", { viewModel.deleteAccountAndAllData(); showDeleteAcctDialog = false }, { showDeleteAcctDialog = false })

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Top bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Text("Settings", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // ── Appearance ─────────────────────────────────────────────────────
            item { CategoryHeader("Appearance") }
            item {
                SettingsCard {
                    ChevronRow(Icons.Filled.AutoAwesome, "Theme", "App color scheme",
                        theme.replaceFirstChar { it.uppercase() },
                        onClick = viewModel::cycleTheme)
                    Div()
                    ChevronRow(Icons.Filled.Map, "Map style", "Current map theme",
                        if (mapStyle == "dark") "Dark" else "Light",
                        onClick = viewModel::cycleMapStyle)
                }
            }

            // ── Tracking ───────────────────────────────────────────────────────
            item { CategoryHeader("Tracking") }
            item {
                SettingsCard {
                    ToggleRow(Icons.Filled.GpsFixed, "Auto-tracking",
                        "Automatically detect and record trips", autoTracking,
                        viewModel::setAutoTracking)
                    Div()
                    ChevronRow(Icons.Filled.Tune, "GPS accuracy", "Location precision vs battery",
                        if (gpsAccuracy == "high") "High" else "Balanced",
                        onClick = viewModel::cycleGpsAccuracy)
                    Div()
                    ChevronRow(Icons.Filled.Route, "Min trip distance",
                        "Discard trips shorter than", minTripDist,
                        onClick = viewModel::cycleMinTripDistance)
                }
            }

            // ── Units ──────────────────────────────────────────────────────────
            item { CategoryHeader("Units") }
            item {
                SettingsCard {
                    ChevronRow(Icons.Filled.Speed, "Speed unit", "Display speed in",
                        speedUnit, onClick = { viewModel.setSpeedUnit(if (speedUnit == "km/h") "mph" else "km/h") })
                    Div()
                    ChevronRow(Icons.Filled.Straighten, "Distance unit", "Display distance in",
                        distanceUnit, onClick = { viewModel.setDistanceUnit(if (distanceUnit == "km") "miles" else "km") })
                }
            }

            // ── Notifications ──────────────────────────────────────────────────
            item { CategoryHeader("Notifications") }
            item {
                SettingsCard {
                    ToggleRow(Icons.Filled.Notifications, "Tracking notification",
                        "Show persistent notification while tracking", bgNotif,
                        viewModel::setBackgroundNotification)
                    Div()
                    ToggleRow(Icons.Filled.Notifications, "Trip summary",
                        "Show notification with stats when a trip ends", tripSummaryNotif,
                        viewModel::setTripSummaryNotification)
                    Div()
                    ToggleRowComingSoon(Icons.Filled.People, "Friend activity",
                        "Notify when friends start a group drive", friendActNotif,
                        viewModel::setFriendActivityNotification)
                }
            }

            // ── Privacy ────────────────────────────────────────────────────────
            item { CategoryHeader("Privacy") }
            item {
                SettingsCard {
                    ToggleRow(Icons.Filled.LocationOn, "Share live location",
                        "Allow friends to see your location in group drives", shareLoc,
                        viewModel::setShareLiveLocation)
                    Div()
                    ChevronRow(Icons.Filled.Person, "Profile visibility", "Who can see your profile",
                        if (profileVis == "everyone") "Everyone" else "Friends only",
                        onClick = viewModel::cycleProfileVisibility)
                }
            }

            // ── Data ───────────────────────────────────────────────────────────
            item { CategoryHeader("Data") }
            item {
                SettingsCard {
                    ActionRow(Icons.Filled.SaveAlt, "Export trips",
                        "Save all trips as a CSV to Downloads",
                        onClick = viewModel::exportTrips)
                    Div()
                    ActionRow(Icons.Filled.Delete, "Clear location cache",
                        "Remove cached geocoding results",
                        onClick = viewModel::clearGeocodingCache)
                    Div()
                    ActionRow(Icons.Filled.DeleteForever, "Delete all trips",
                        "Wipe all locally recorded trip data",
                        onClick = { showDeleteDataDialog = true },
                        tint = CoralPrimary)
                }
            }

            // ── Account ────────────────────────────────────────────────────────
            item { CategoryHeader("Account") }
            item {
                SettingsCard {
                    ActionRow(Icons.AutoMirrored.Filled.Logout, "Sign out", "",
                        onClick = { showSignOutDialog = true })
                    Div()
                    ActionRow(Icons.Filled.DeleteForever, "Delete account",
                        "Permanently delete your account and all data",
                        onClick = { showDeleteAcctDialog = true },
                        tint = CoralPrimary)
                }
            }

            // ── About ──────────────────────────────────────────────────────────
            item { CategoryHeader("About") }
            item {
                SettingsCard {
                    StaticRow(Icons.Filled.Info, "Version", "1.0.0")
                    Div()
                    ActionRow(Icons.Filled.Info, "Open source licenses", "",
                        onClick = onNavigateToLicenses)
                    Div()
                    ActionRow(Icons.Filled.Security, "Privacy policy", "",
                        onClick = viewModel::openPrivacyPolicy)
                    Div()
                    ActionRow(Icons.Filled.Description, "Terms of service", "",
                        onClick = viewModel::openTermsOfService)
                    Div()
                    ActionRow(Icons.Filled.Info, "Send feedback",
                        "drivelogofficial1@gmail.com",
                        onClick = viewModel::sendFeedback)
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

// ── Category layout helpers ───────────────────────────────────────────────────

@Composable
private fun CategoryHeader(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) { Column { content() } }
}

@Composable
private fun Div() {
    HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp,
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

// ── Row types ─────────────────────────────────────────────────────────────────

@Composable
private fun ToggleRow(icon: ImageVector, name: String, desc: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (desc.isNotBlank()) Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked, onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary))
    }
}

@Composable
private fun ToggleRowComingSoon(icon: ImageVector, name: String, desc: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text("(coming soon)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (desc.isNotBlank()) Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked, onToggle, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = TealPrimary))
    }
}

@Composable
private fun ChevronRow(icon: ImageVector, name: String, desc: String, currentValue: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            if (desc.isNotBlank()) Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(currentValue, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TealPrimary)
    }
}

@Composable
private fun ActionRow(icon: ImageVector, name: String, desc: String, onClick: () -> Unit,
                      tint: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontSize = 14.sp, color = tint)
            if (desc.isNotBlank()) Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StaticRow(icon: ImageVector, name: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
        Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text  = { Text(body) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
