package com.makan.roadpulse.presentation.map

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makan.roadpulse.domain.model.Trip
import com.makan.roadpulse.presentation.components.TripRow
import com.makan.roadpulse.presentation.theme.CoralPrimary
import com.makan.roadpulse.presentation.theme.TealPrimary
import com.makan.roadpulse.presentation.util.LocalUnitPrefs

// ── Stat cards ────────────────────────────────────────────────────────────────

@Composable
fun StatCardsRow(stats: TodayStats, modifier: Modifier = Modifier) {
    val units = LocalUnitPrefs.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Today's distance",
            value = units.distanceValueKm(stats.distanceKm),
            unit  = units.distanceLabel(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Top speed",
            value = units.speedValueKmh(stats.topSpeedKmh),
            unit  = units.speedLabel(),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Avg speed",
            value = units.speedValueKmh(stats.avgSpeedKmh),
            unit  = units.speedLabel(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.W500,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ── Recent trips list ─────────────────────────────────────────────────────────

@Composable
fun RecentTripsList(
    trips: List<Trip>,
    onTripClick: (Long) -> Unit,
    onLongPress: ((Trip) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Recent trips",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (trips.isEmpty()) {
            Text(
                text = "No trips yet — start your first drive!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            return
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
        ) {
            trips.forEachIndexed { index, trip ->
                TripRow(
                    trip = trip,
                    index = index,
                    onClick = { onTripClick(trip.id) },
                    onLongPress = onLongPress
                )
                if (index < trips.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}


// ── Recenter FAB ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecenterFab(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 4.dp,
        tonalElevation = 2.dp
    ) {
        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.GpsFixed,
                contentDescription = "Recenter on my location",
                tint = TealPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// ── Tracking FAB ──────────────────────────────────────────────────────────────

@Composable
fun TrackingFab(
    isTracking: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fab_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (isTracking) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(CoralPrimary.copy(alpha = pulseAlpha))
            )
        }
        FloatingActionButton(
            onClick = onToggle,
            containerColor = if (isTracking) CoralPrimary else TealPrimary,
            contentColor = Color.White,
            modifier = Modifier.size(56.dp),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = if (isTracking) "Stop tracking" else "Start drive",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
