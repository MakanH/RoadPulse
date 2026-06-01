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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.makan.drivelog.presentation.trips.TripsViewModel.YearMonth
import com.makan.drivelog.presentation.util.LocalUnitPrefs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripsScreen(
    onTripClick: (Long) -> Unit = {},
    viewModel: TripsViewModel = hiltViewModel()
) {
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val filter          by viewModel.filter.collectAsStateWithLifecycle()
    val yearMonthFilter by viewModel.yearMonthFilter.collectAsStateWithLifecycle()
    var showPicker      by remember { mutableStateOf(false) }

    when (val state = uiState) {
        is TripsViewModel.UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = TealPrimary) }
        }
        is TripsViewModel.UiState.Success -> {
            TripsContent(
                state           = state,
                filter          = filter,
                yearMonthFilter = yearMonthFilter,
                onFilterChange  = viewModel::setFilter,
                onShowPicker    = { showPicker = true },
                onClearYmFilter = viewModel::clearYearMonthFilter,
                onTripClick     = onTripClick
            )

            if (showPicker) {
                YearMonthPickerSheet(
                    availableYears    = state.availableYears,
                    tripsPerYearMonth = state.tripsPerYearMonth,
                    currentFilter     = yearMonthFilter,
                    onApply           = { ym ->
                        viewModel.setYearMonthFilter(ym.year, ym.month)
                        showPicker = false
                    },
                    onShowAll         = {
                        viewModel.clearYearMonthFilter()
                        showPicker = false
                    },
                    onDismiss         = { showPicker = false }
                )
            }
        }
    }
}

// ── Main content ──────────────────────────────────────────────────────────────

@Composable
private fun TripsContent(
    state: TripsViewModel.UiState.Success,
    filter: TripFilter,
    yearMonthFilter: YearMonth?,
    onFilterChange: (TripFilter) -> Unit,
    onShowPicker: () -> Unit,
    onClearYmFilter: () -> Unit,
    onTripClick: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            SummaryCard(stats = state.stats,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp))
        }
        item {
            FilterRow(
                filter          = filter,
                yearMonthFilter = yearMonthFilter,
                onFilterChange  = onFilterChange,
                onShowPicker    = onShowPicker,
                onClearYmFilter = onClearYmFilter,
                modifier        = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
        }
        if (state.grouped.isEmpty()) {
            item { EmptyState() }
        } else {
            state.grouped.forEach { (dateLabel, trips) ->
                stickyHeader(key = dateLabel) { DateHeader(dateLabel) }
                itemsIndexed(trips, key = { _, trip -> trip.id }) { index, trip ->
                    TripRow(trip = trip, index = index, onClick = { onTripClick(trip.id) })
                }
            }
        }
    }
}

// ── Filter row (chips or active year/month chip + calendar icon) ──────────────

@Composable
private fun FilterRow(
    filter: TripFilter,
    yearMonthFilter: YearMonth?,
    onFilterChange: (TripFilter) -> Unit,
    onShowPicker: () -> Unit,
    onClearYmFilter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            if (yearMonthFilter == null) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        TripFilter.ALL_TIME   to "All time",
                        TripFilter.THIS_MONTH to "This month",
                        TripFilter.THIS_WEEK  to "This week"
                    ).forEach { (chipFilter, label) ->
                        FilterChip(
                            selected = filter == chipFilter,
                            onClick  = { onFilterChange(chipFilter) },
                            label    = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            } else {
                ActiveYearMonthChip(ymFilter = yearMonthFilter, onClear = onClearYmFilter)
            }
        }
        IconButton(onClick = onShowPicker) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = "Browse by month",
                tint = if (yearMonthFilter != null) TealPrimary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActiveYearMonthChip(ymFilter: YearMonth, onClear: () -> Unit) {
    val months = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
    val label  = "${months[ymFilter.month - 1]} ${ymFilter.year}"
    FilterChip(
        selected     = true,
        onClick      = onClear,
        label        = { Text(label, fontSize = 12.sp) },
        trailingIcon = {
            Icon(Icons.Filled.Close, contentDescription = "Clear",
                modifier = Modifier.size(14.dp))
        }
    )
}

// ── Summary card ──────────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(stats: TripsViewModel.SummaryStats, modifier: Modifier = Modifier) {
    val units = LocalUnitPrefs.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell("Total trips",    stats.totalTrips.toString(),                   Modifier.weight(1f))
                MetricCell("Total distance", units.formatDistanceKm(stats.totalDistanceKm), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCell("Total drive time", stats.totalDriveTime,                       Modifier.weight(1f))
                MetricCell("Top speed",        units.formatSpeedKmh(stats.topSpeedKmh),    Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MetricCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.W500, color = MaterialTheme.colorScheme.onSurface)
    }
}

// ── Date header ───────────────────────────────────────────────────────────────

@Composable
private fun DateHeader(dateLabel: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dateLabel, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.size(8.dp))
        HorizontalDivider(modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.DirectionsCar, contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
            Spacer(Modifier.height(16.dp))
            Text("No trips yet", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text("Start driving to see your trips here", fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Year/month picker bottom sheet ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearMonthPickerSheet(
    availableYears: List<Int>,
    tripsPerYearMonth: Map<Int, Set<Int>>,
    currentFilter: YearMonth?,
    onApply: (YearMonth) -> Unit,
    onShowAll: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear  by remember(currentFilter) { mutableStateOf(currentFilter?.year) }
    var selectedMonth by remember(currentFilter) { mutableStateOf(currentFilter?.month) }

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor    = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Text("Browse by month", fontSize = 16.sp, fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(16.dp))

            // ── Year row ──────────────────────────────────────────────────────
            Text("Year", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (availableYears.isEmpty()) {
                Text("No trips recorded yet", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier            = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableYears.forEach { year ->
                        FilterChip(
                            selected = selectedYear == year,
                            onClick  = { selectedYear = year; selectedMonth = null },
                            label    = { Text(year.toString()) }
                        )
                    }
                }
            }

            // ── Month grid (3 columns × 4 rows) ───────────────────────────────
            if (selectedYear != null) {
                Spacer(Modifier.height(16.dp))
                Text("Month", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))

                val monthsWithTrips = tripsPerYearMonth[selectedYear] ?: emptySet()
                val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun",
                                        "Jul","Aug","Sep","Oct","Nov","Dec")

                monthNames.chunked(3).forEachIndexed { rowIdx, rowNames ->
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowNames.forEachIndexed { colIdx, name ->
                            val month   = rowIdx * 3 + colIdx + 1
                            val enabled = monthsWithTrips.contains(month)
                            FilterChip(
                                selected = selectedMonth == month,
                                onClick  = { if (enabled) selectedMonth = month },
                                enabled  = enabled,
                                label    = { Text(name, fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // pad last row if < 3 items
                        repeat(3 - rowNames.size) { Spacer(Modifier.weight(1f)) }
                    }
                    if (rowIdx < 3) Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Actions ───────────────────────────────────────────────────────
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onShowAll) { Text("Show all") }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick  = {
                        val y = selectedYear; val m = selectedMonth
                        if (y != null && m != null) onApply(YearMonth(y, m))
                    },
                    enabled  = selectedYear != null && selectedMonth != null,
                    colors   = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                    shape    = RoundedCornerShape(12.dp)
                ) { Text("Apply") }
            }
        }
    }
}
