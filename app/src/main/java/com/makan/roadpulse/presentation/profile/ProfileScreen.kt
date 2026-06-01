package com.makan.roadpulse.presentation.profile

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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.makan.roadpulse.presentation.components.OnboardingPopup
import com.makan.roadpulse.presentation.onboarding.OnboardingViewModel
import com.makan.roadpulse.presentation.theme.TealPrimary

@Composable
fun ProfileScreen(
    onNavigateToFriends: () -> Unit = {},
    onNavigateToGroups: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel(),
    onboardingVm: OnboardingViewModel = hiltViewModel()
) {
    val uiState        by viewModel.uiState.collectAsStateWithLifecycle()
    val onboardingSeen by onboardingVm.profileSeen.collectAsStateWithLifecycle(true)

    val state = uiState
    if (state == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = TealPrimary) }
        return
    }

    Box(Modifier.fillMaxSize()) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item { Spacer(Modifier.height(12.dp)) }
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                AvatarSection(state.displayName, state.email, state.photoUrl, state.stats.memberSince)
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 8.dp)
                ) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
        item { SectionHeader("Lifetime stats") }
        item { LifetimeStatsCard(state.stats) }
        item { Spacer(Modifier.height(20.dp)) }
        item { SectionHeader("Social") }
        item {
            SocialCard(
                friendCount = state.friendCount,
                groupCount  = state.groupCount,
                onFriends   = onNavigateToFriends,
                onGroups    = onNavigateToGroups
            )
        }
    } // end LazyColumn

    if (!onboardingSeen) {
        OnboardingPopup(
            icon      = Icons.Filled.Person,
            title     = "Your profile",
            body      = "View your lifetime stats, manage friends and groups, and customize the app in settings. Tap the gear icon for appearance, tracking, privacy, and more.",
            onDismiss = onboardingVm::markProfileSeen
        )
    }
    } // end Box
}

// ── Avatar ────────────────────────────────────────────────────────────────────

@Composable
private fun AvatarSection(displayName: String, email: String, photoUrl: String?, memberSince: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "Avatar",
                modifier = Modifier.size(72.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(72.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "D",
                    fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TealPrimary
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(displayName, fontSize = 16.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
        if (email.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
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
                StatCell("Total trips",    stats.totalTrips.toString(),               Modifier.weight(1f))
                StatCell("Total distance", "%.1f km".format(stats.totalDistanceKm),   Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Drive time",     formatHours(stats.totalDrivingHours),      Modifier.weight(1f))
                StatCell("Avg trips/week", "%.1f".format(stats.avgTripsPerWeek),      Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatCell("Avg trip dist.", "%.1f km".format(stats.avgTripDistanceKm), Modifier.weight(1f))
                StatCell("Top speed",     "${stats.topSpeedKmh} km/h",               Modifier.weight(1f))
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

// ── Social ────────────────────────────────────────────────────────────────────

@Composable
private fun SocialCard(
    friendCount: Int,
    groupCount: Int,
    onFriends: () -> Unit,
    onGroups: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            SocialRow(
                icon    = Icons.Filled.People,
                label   = "Friends",
                count   = "$friendCount friend${if (friendCount != 1) "s" else ""}",
                onClick = onFriends
            )
            HorizontalDivider(
                modifier  = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            SocialRow(
                icon    = Icons.Filled.DirectionsCar,
                label   = "Groups",
                count   = "$groupCount group${if (groupCount != 1) "s" else ""}",
                onClick = onGroups
            )
        }
    }
}

@Composable
private fun SocialRow(icon: ImageVector, label: String, count: String, onClick: () -> Unit) {
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
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f))
        Text(count, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
internal fun SectionHeader(title: String) {
    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
}
