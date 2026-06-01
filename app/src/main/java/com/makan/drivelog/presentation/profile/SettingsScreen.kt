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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState?.settings

    var showSignOutDialog    by remember { mutableStateOf(false) }
    var showDeleteDialog     by remember { mutableStateOf(false) }
    var showDeleteAcctDialog by remember { mutableStateOf(false) }

    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text("Sign out?") },
            text  = { Text("You will be returned to the login screen.") },
            confirmButton = {
                TextButton(onClick = { viewModel.signOut(); showSignOutDialog = false }) {
                    Text("Sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutDialog = false }) { Text("Cancel") }
            }
        )
    }

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

    if (showDeleteAcctDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAcctDialog = false },
            title = { Text("Delete account?") },
            text  = { Text("This permanently deletes your account and all trip data. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAccountAndAllData(); showDeleteAcctDialog = false }) {
                    Text("Delete account", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAcctDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Top bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
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

        if (settings == null) return@LazyColumn

        // Settings card
        item { Spacer(Modifier.height(8.dp)) }
        item { SectionHeader("Preferences") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    ToggleRow(
                        icon = Icons.Filled.GpsFixed,
                        name = "Auto-tracking",
                        description = "Automatically detect and log trips",
                        checked = settings.autoTracking,
                        onToggle = viewModel::setAutoTracking
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
                        icon = Icons.Filled.Notifications,
                        name = "Background notification",
                        description = "Show notification while tracking",
                        checked = settings.backgroundNotification,
                        onToggle = viewModel::setBackgroundNotification
                    )
                    RowDivider()
                    // Sign out row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showSignOutDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(12.dp))
                        Text("Sign out", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Danger zone
        item { Spacer(Modifier.height(20.dp)) }
        item { SectionHeader("Danger zone") }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null,
                            tint = CoralPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete all data", fontSize = 14.sp, color = CoralPrimary)
                    }
                    RowDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDeleteAcctDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null,
                            tint = CoralPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Delete account", fontSize = 14.sp, color = CoralPrimary)
                    }
                }
            }
        }
    }
}

// ── Row composables ────────────────────────────────────────────────────────────

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
                checkedThumbColor = Color.White,
                checkedTrackColor = TealPrimary
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
