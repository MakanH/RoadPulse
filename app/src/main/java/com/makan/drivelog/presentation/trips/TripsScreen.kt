package com.makan.drivelog.presentation.trips

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.makan.drivelog.domain.model.Trip
import com.makan.drivelog.presentation.components.TripRow
import com.makan.drivelog.presentation.theme.TealPrimary
import com.makan.drivelog.presentation.trips.TripsViewModel.TripFilter
import com.makan.drivelog.presentation.util.LocalUnitPrefs

@Composable
fun TripsScreen(
    onTripClick: (Long) -> Unit = {},
    viewModel: TripsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is TripsViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = TealPrimary)
            }
        }
        is TripsViewModel.UiState.Success -> {
            TripsContent(
                grouped = state.grouped,
                stats = state.stats,
                filter = filter,
                onFilterChange = viewModel::setFilter,
                onTripClick = onTripClick
            )
        }
    }
}

@Composable
private fun TripsContent(
    grouped: Map<String, List<Trip>>,
    stats: TripsViewModel.SummaryStats,
    filter: TripFilter,
    onFilterChange: (TripFilter) -> Unit,
    onTripClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            SummaryCard(
                stats = stats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
        }
        item {
            FilterChipsRow(
                selected = filter,
                onSelect = onFilterChange,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
        }
        if (grouped.isEmpty()) {
            item { EmptyState() }
        } else {
            grouped.forEach { (dateLabel, trips) ->
                stickyHeader(key = dateLabel) {
                    DateHeader(dateLabel)
                }
                itemsIndexed(trips, key = { _, trip -> trip.id }) { index, trip ->
                    TripRow(trip = trip, index = index, onClick = { onTripClick(trip.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(stats: TripsViewModel.SummaryStats, modifier: Modifier = Modifier) {
    val units = LocalUnitPrefs.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCell("Total trips",    stats.totalTrips.toString(),                  Modifier.weight(1f))
                MetricCell("Total distance", units.formatDistanceKm(stats.totalDistanceKm), Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricCell("Total drive time", stats.totalDriveTime,                      Modifier.weight(1f))
                MetricCell("Top speed",        units.formatSpeedKmh(stats.topSpeedKmh),   Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.W500,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FilterChipsRow(
    selected: TripFilter,
    onSelect: (TripFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val chips = listOf(
        TripFilter.ALL_TIME   to "All time",
        TripFilter.THIS_MONTH to "This month",
        TripFilter.THIS_WEEK  to "This week",
        TripFilter.TODAY      to "Today"
    )
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        chips.forEach { (chipFilter, label) ->
            FilterChip(
                selected = selected == chipFilter,
                onClick = { onSelect(chipFilter) },
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun DateHeader(dateLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dateLabel,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.size(8.dp))
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Filled.DirectionsCar,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "No trips yet",
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Start driving to see your trips here",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
