package com.makan.drivelog.presentation.map

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.makan.drivelog.data.service.TrackingState
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

private const val DARK_STYLE_URL = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
private const val SOURCE_PREFIX = "dl-src-"
private const val LAYER_PREFIX  = "dl-lyr-"

private class MapDrawingState {
    val drawnTripIds = mutableSetOf<Long>()
}

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    tripsWithPoints: List<TripWithPoints>,
    trackingState: TrackingState,
    monthLabel: String,
    onTripClick: (Long) -> Unit
) {
    val context = LocalContext.current
    val mapStyleState = remember { mutableStateOf<Style?>(null) }
    val drawingState  = remember { MapDrawingState() }

    // rememberUpdatedState so the factory closure always reads the latest values
    val latestTrips         = rememberUpdatedState(tripsWithPoints)
    val latestActiveTripId  = rememberUpdatedState(trackingState.currentTrip?.id)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(DARK_STYLE_URL) { style ->
                    mapStyleState.value = style
                    setupLocationComponent(map, style, context)
                    redrawTrips(style, latestTrips.value, drawingState, latestActiveTripId.value)
                }
            }
        }
    }

    // Re-draw whenever style becomes ready OR data changes
    val style = mapStyleState.value
    LaunchedEffect(style, tripsWithPoints, trackingState.currentTrip?.id) {
        style ?: return@LaunchedEffect
        redrawTrips(style, tripsWithPoints, drawingState, trackingState.currentTrip?.id)
    }

    // Forward lifecycle events to MapView
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START   -> mapView.onStart()
                Lifecycle.Event.ON_RESUME  -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE   -> mapView.onPause()
                Lifecycle.Event.ON_STOP    -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    Box(modifier = modifier) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        // "All trips — Month Year" overlay
        Text(
            text = "All trips — $monthLabel",
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@SuppressLint("MissingPermission")
private fun setupLocationComponent(map: MapLibreMap, style: Style, context: android.content.Context) {
    try {
        val options = LocationComponentOptions.builder(context)
            .pulseEnabled(true)
            .pulseColor(AndroidColor.parseColor("#5DCAA5"))
            .pulseAlpha(0.4f)
            .build()
        val activation = LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(options)
            .useDefaultLocationEngine(true)
            .build()
        map.locationComponent.activateLocationComponent(activation)
        map.locationComponent.isLocationComponentEnabled = true
        map.locationComponent.cameraMode = CameraMode.NONE
        map.locationComponent.renderMode = RenderMode.NORMAL
    } catch (e: Exception) {
        // Permission may not be granted yet; location component set up later
    }
}

private fun redrawTrips(
    style: Style,
    trips: List<TripWithPoints>,
    state: MapDrawingState,
    activeTripId: Long?
) {
    // Remove previously drawn layers/sources
    state.drawnTripIds.toList().forEach { id ->
        try { style.removeLayer("$LAYER_PREFIX$id") } catch (e: Exception) {}
        try { style.removeSource("$SOURCE_PREFIX$id") } catch (e: Exception) {}
    }
    state.drawnTripIds.clear()

    // Draw inactive trips first (bottom), active trip last (top)
    val sorted = trips.sortedWith(Comparator { a, b ->
        when {
            a.trip.id == activeTripId -> 1
            b.trip.id == activeTripId -> -1
            else -> 0
        }
    })

    sorted.forEachIndexed { index, twp ->
        if (twp.points.size < 2) return@forEachIndexed
        val trip = twp.trip
        val isActive = trip.id == activeTripId
        val color   = when { isActive -> "#5DCAA5"; index % 2 == 0 -> "#378ADD"; else -> "#D85A30" }
        val width   = if (isActive) 4f else 2f
        val opacity = when { isActive -> 1f; index % 2 == 0 -> 0.5f; else -> 0.4f }

        val pts = twp.points.map { Point.fromLngLat(it.longitude, it.latitude) }
        val feature = Feature.fromGeometry(LineString.fromLngLats(pts))
        val collection = FeatureCollection.fromFeatures(listOf(feature))

        val srcId = "$SOURCE_PREFIX${trip.id}"
        val lyrId = "$LAYER_PREFIX${trip.id}"
        try {
            style.addSource(GeoJsonSource(srcId, collection))
            style.addLayer(
                LineLayer(lyrId, srcId).withProperties(
                    PropertyFactory.lineColor(color),
                    PropertyFactory.lineWidth(width),
                    PropertyFactory.lineOpacity(opacity),
                    PropertyFactory.lineCap("round"),
                    PropertyFactory.lineJoin("round")
                )
            )
            state.drawnTripIds.add(trip.id)
        } catch (e: Exception) { /* layer/source might already exist from a race; ignore */ }
    }
}
