package com.makan.drivelog.presentation.navigate

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.makan.drivelog.data.service.NominatimGeocodingService
import com.makan.drivelog.data.service.OpenRouteService
import com.makan.drivelog.data.service.SearchResult
import com.makan.drivelog.domain.model.Route
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import javax.inject.Inject

enum class NavState { IDLE, SEARCHING, ROUTING, ROUTE_PREVIEW, NAVIGATING, ERROR }

data class NavigateUiState(
    val navState: NavState = NavState.IDLE,
    val query: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val selectedShortName: String = "",
    val selectedLat: Double = 0.0,
    val selectedLon: Double = 0.0,
    val route: Route? = null,
    val currentStepIndex: Int = 0,
    val userLat: Double? = null,
    val userLon: Double? = null,
    val distanceToNextTurn: Double = 0.0,
    val errorMessage: String? = null
)

@HiltViewModel
class NavigateViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geocodingService: NominatimGeocodingService,
    private val routeService: OpenRouteService
) : ViewModel() {

    private val _state = MutableStateFlow(NavigateUiState())
    val uiState: StateFlow<NavigateUiState> = _state.asStateFlow()

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var searchJob: Job? = null

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            _state.update { it.copy(userLat = loc.latitude, userLon = loc.longitude) }
            advanceStepIfNeeded(loc.latitude, loc.longitude)
            updateDistanceToTurn(loc.latitude, loc.longitude)
        }
    }

    init { startLocationUpdates() }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2_000L)
                .setMinUpdateIntervalMillis(1_000L).build()
            fusedLocationClient.requestLocationUpdates(req, locationCallback, android.os.Looper.getMainLooper())
        } catch (_: SecurityException) {}
    }

    override fun onCleared() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // ── Search ────────────────────────────────────────────────────────────────

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(navState = NavState.IDLE, searchResults = emptyList()) }
            return
        }
        _state.update { it.copy(navState = NavState.SEARCHING) }
        searchJob = viewModelScope.launch {
            delay(500)
            val results = geocodingService.forwardGeocode(query)
            _state.update { it.copy(searchResults = results) }
        }
    }

    // ── Route computation ─────────────────────────────────────────────────────

    fun onSelectDestination(result: SearchResult) {
        searchJob?.cancel()
        _state.update { it.copy(
            navState        = NavState.ROUTING,
            query           = result.shortName,
            selectedShortName = result.shortName,
            selectedLat     = result.lat,
            selectedLon     = result.lon,
            searchResults   = emptyList()
        ) }
        viewModelScope.launch {
            val state = _state.value
            val uLat  = state.userLat ?: run {
                _state.update { it.copy(navState = NavState.ERROR, errorMessage = "Location unavailable") }
                return@launch
            }
            val uLon = state.userLon!!
            val route = routeService.getRoute(uLat, uLon, result.lat, result.lon)
            if (route != null && route.geometry.size >= 2) {
                _state.update { it.copy(navState = NavState.ROUTE_PREVIEW, route = route, currentStepIndex = 0) }
            } else {
                _state.update { it.copy(navState = NavState.ERROR, errorMessage = "Could not find a route") }
            }
        }
    }

    // ── Navigation control ────────────────────────────────────────────────────

    fun onStartNavigation() {
        _state.update { it.copy(navState = NavState.NAVIGATING, currentStepIndex = 0) }
    }

    fun onEndNavigation() {
        _state.update { NavigateUiState(userLat = it.userLat, userLon = it.userLon) }
    }

    fun onClearDestination() {
        searchJob?.cancel()
        _state.update { NavigateUiState(userLat = it.userLat, userLon = it.userLon) }
    }

    fun clearError() { _state.update { it.copy(errorMessage = null) } }

    // ── Step advancement ──────────────────────────────────────────────────────

    private fun advanceStepIfNeeded(lat: Double, lon: Double) {
        val s = _state.value
        if (s.navState != NavState.NAVIGATING) return
        val route = s.route ?: return
        val idx   = s.currentStepIndex
        if (idx >= route.steps.lastIndex) return
        val step  = route.steps[idx]
        val endPt = route.geometry.getOrNull(step.endPointIndex) ?: return
        if (haversine(lat, lon, endPt.lat, endPt.lon) < 30.0) {
            _state.update { it.copy(currentStepIndex = idx + 1) }
        }
    }

    private fun updateDistanceToTurn(lat: Double, lon: Double) {
        val s = _state.value
        if (s.navState != NavState.NAVIGATING) return
        val route = s.route ?: return
        val step  = route.steps.getOrNull(s.currentStepIndex) ?: return
        val endPt = route.geometry.getOrNull(step.endPointIndex) ?: return
        _state.update { it.copy(distanceToNextTurn = haversine(lat, lon, endPt.lat, endPt.lon)) }
    }

    // ── Util ──────────────────────────────────────────────────────────────────

    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r    = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a    = sin(dLat / 2).pow(2) +
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }
}
