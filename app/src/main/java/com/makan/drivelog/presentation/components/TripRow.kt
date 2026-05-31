package com.makan.drivelog.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.makan.drivelog.domain.model.Trip
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class TripAccent(val bgColor: Color, val iconColor: Color, val labelColor: Color) {
    TEAL(Color(0xFFE1F5EE), Color(0xFF0F6E56), Color(0xFF5DCAA5)),
    BLUE(Color(0xFFE6F1FB), Color(0xFF185FA5), Color(0xFF378ADD)),
    CORAL(Color(0xFFFAECE7), Color(0xFF993C1D), Color(0xFFD85A30))
}

@Composable
fun TripRow(trip: Trip, index: Int, onClick: () -> Unit) {
    val accent = TripAccent.entries[index % TripAccent.entries.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(accent.bgColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                tint = accent.iconColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = trip.name, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
            Text(text = tripSubtitle(trip), fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "${(trip.avgSpeed * 3.6).toInt()} km/h",
            fontSize = 12.sp, fontWeight = FontWeight.Medium, color = accent.labelColor)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
    }
}

fun tripSubtitle(trip: Trip): String {
    val date = SimpleDateFormat("EEE, HH:mm", Locale.getDefault()).format(Date(trip.startTime))
    val distKm = "%.1f km".format(trip.distance / 1000.0)
    val endMs = trip.endTime ?: System.currentTimeMillis()
    val durationMs = endMs - trip.startTime
    val hours = durationMs / 3_600_000
    val mins = (durationMs % 3_600_000) / 60_000
    val duration = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    return "$date · $distKm · $duration"
}
