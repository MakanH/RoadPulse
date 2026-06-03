package com.makan.roadpulse.presentation.map

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.graphics.PointF
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
import androidx.compose.runtime.compositionLocalOf
import com.google.gson.JsonObject
import com.makan.roadpulse.data.service.TrackingState
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
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

internal const val DARK_STYLE_URL  = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
internal const val LIGHT_STYLE_URL = "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json"
private const val SOURCE_PREFIX    = "dl-src-"
private const val LAYER_PREFIX     = "dl-lyr-"
private const val HM_SRC_PREFIX    = "dl-hm-src-"
private const val HM_LYR_PREFIX    = "dl-hm-lyr-"

// Speed brackets: 0-30, 30-60, 60-90, 90-120, 120-150, 150-180, 180+ km/h
private val HEATMAP_COLORS = arrayOf(
    "#97C459", // green    0–30
    "#EF9F27", // amber   30–60
    "#D85A30", // orange  60–90
    "#E24B4A", // red     90–120
    "#A32D2D", // deep red 120–150
    "#7F77DD", // purple  150–180
    "#D4537E"  // pink    180+
)

private fun speedMsToBracket(speedMs: Double): Int {
    val kmh = speedMs * 3.6
    return when {
        kmh < 30  -> 0
        kmh < 60  -> 1
        kmh < 90  -> 2
        kmh < 120 -> 3
        kmh < 150 -> 4
        kmh < 180 -> 5
        else      -> 6
    }
}

val LocalMapStyleUrl = compositionLocalOf { DARK_STYLE_URL }

fun getMapStyleUrl(pref: String): String =
    if (pref == "light") LIGHT_STYLE_URL else DARK_STYLE_URL

internal class MapDrawingState {
    val drawnTripIds    = mutableSetOf<Long>()
    val heatmapLayerIds = mutableSetOf<String>()
    val heatmapSrcIds   = mutableSetOf<String>()
    var mapLibre: MapLibreMap? = null
}

@Composable
fun MapLibreMapView(
    modifier: Modifier = Modifier,
    tripsWithPoints: List<TripWithPoints>,
    trackingState: TrackingState,
    monthLabel: String,
    isHeatmapMode: Boolean = false,
    onTripClick: (Long) -> Unit,
    recenterTrigger: Int = 0,
    onRecenterFailed: () -> Unit = {}
) {
    val context       = LocalContext.current
    val mapStyleUrl   = LocalMapStyleUrl.current
    val mapStyleState = remember { mutableStateOf<Style?>(null) }
    val drawingState  = remember { MapDrawingState() }

    val latestTrips        = rememberUpdatedState(tripsWithPoints)
    val latestActiveTripId = rememberUpdatedState(trackingState.currentTrip?.id)
    val latestOnTripClick  = rememberUpdatedState(onTripClick)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                drawingState.mapLibre = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(mapStyleUrl) { style ->
                    mapStyleState.value = style
                    setupLocationComponent(map, style, context)
                    redrawTrips(style, latestTrips.value, drawingState, latestActiveTripId.value)
                }
                map.addOnMapClickListener { latLng ->
                    val screenPt   = map.projection.toScreenLocation(latLng)
                    val flatLayers = drawingState.drawnTripIds.map { "$LAYER_PREFIX$it" }
                    val hmLayers   = drawingState.heatmapLayerIds.toList()
                    val allLayers  = (flatLayers + hmLayers).toTypedArray()
                    if (allLayers.isNotEmpty()) {
                        val hits = map.queryRenderedFeatures(PointF(screenPt.x, screenPt.y), *allLayers)
                        val tripId = hits.firstOrNull()
                            ?.properties()?.get("tripId")?.asString?.toLongOrNull()
                        if (tripId != null) {
                            latestOnTripClick.value(tripId)
                            return@addOnMapClickListener true
                        }
                    }
                    false
                }
            }
        }
    }

    val style = mapStyleState.value
    LaunchedEffect(style, tripsWithPoints, trackingState.currentTrip?.id, isHeatmapMode) {
        style ?: return@LaunchedEffect
        val activeTripId = trackingState.currentTrip?.id
        if (isHeatmapMode) {
            // Remove flat polylines
            drawingState.drawnTripIds.toList().forEach { id ->
                try { style.removeLayer("$LAYER_PREFIX$id") } catch (_: Exception) {}
                try { style.removeSource("$SOURCE_PREFIX$id") } catch (_: Exception) {}
            }
            drawingState.drawnTripIds.clear()
            // Clear stale heatmap and redraw
            clearHeatmapLayers(style, drawingState)
            drawHeatmap(style, tripsWithPoints, drawingState)
        } else {
            // Remove heatmap, restore flat polylines
            clearHeatmapLayers(style, drawingState)
            redrawTrips(style, tripsWithPoints, drawingState, activeTripId)
        }
    }

    // Reload map style reactively when the preference changes
    LaunchedEffect(mapStyleUrl) {
        val map = drawingState.mapLibre ?: return@LaunchedEffect
        map.setStyle(mapStyleUrl) { style ->
            drawingState.drawnTripIds.clear()
            drawingState.heatmapLayerIds.clear()
            drawingState.heatmapSrcIds.clear()
            mapStyleState.value = style
            setupLocationComponent(map, style, context)
        }
    }

    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger == 0) return@LaunchedEffect
        val map = drawingState.mapLibre ?: run { onRecenterFailed(); return@LaunchedEffect }
        val loc = map.locationComponent.lastKnownLocation
            ?: run { onRecenterFailed(); return@LaunchedEffect }
        map.animateCamera(
            CameraUpdateFactory.newLatLngZoom(LatLng(loc.latitude, loc.longitude), 15.0),
            500
        )
    }

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
    } catch (e: Exception) { /* permission not yet granted */ }
}

internal fun redrawTrips(
    style: Style,
    trips: List<TripWithPoints>,
    state: MapDrawingState,
    activeTripId: Long?
) {
    state.drawnTripIds.toList().forEach { id ->
        try { style.removeLayer("$LAYER_PREFIX$id") } catch (e: Exception) {}
        try { style.removeSource("$SOURCE_PREFIX$id") } catch (e: Exception) {}
    }
    state.drawnTripIds.clear()

    val sorted = trips.sortedWith { a, b ->
        when {
            a.trip.id == activeTripId -> 1
            b.trip.id == activeTripId -> -1
            else -> 0
        }
    }

    sorted.forEachIndexed { index, twp ->
        if (twp.points.size < 2) return@forEachIndexed
        val trip     = twp.trip
        val isActive = trip.id == activeTripId
        val color    = when { isActive -> "#5DCAA5"; index % 2 == 0 -> "#378ADD"; else -> "#D85A30" }
        val width    = if (isActive) 4f else 2f
        val opacity  = when { isActive -> 1f; index % 2 == 0 -> 0.5f; else -> 0.4f }

        val props = JsonObject().apply { addProperty("tripId", trip.id.toString()) }
        val pts   = twp.points.map { Point.fromLngLat(it.longitude, it.latitude) }
        val feat  = Feature.fromGeometry(LineString.fromLngLats(pts), props)

        val srcId = "$SOURCE_PREFIX${trip.id}"
        val lyrId = "$LAYER_PREFIX${trip.id}"
        try {
            style.addSource(GeoJsonSource(srcId, FeatureCollection.fromFeatures(listOf(feat))))
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
        } catch (e: Exception) { /* race condition guard */ }
    }
}

private fun clearHeatmapLayers(style: Style, state: MapDrawingState) {
    state.heatmapLayerIds.toList().forEach { id ->
        try { style.removeLayer(id) } catch (_: Exception) {}
    }
    state.heatmapSrcIds.toList().forEach { id ->
        try { style.removeSource(id) } catch (_: Exception) {}
    }
    state.heatmapLayerIds.clear()
    state.heatmapSrcIds.clear()
}

private fun drawHeatmap(style: Style, trips: List<TripWithPoints>, state: MapDrawingState) {
    trips.forEach { twp ->
        if (twp.points.size < 2) return@forEach

        // Sample large trips to cap rendering work
        val points = if (twp.points.size > 500) {
            val step = (twp.points.size / 500).coerceAtLeast(2)
            twp.points.filterIndexed { i, _ -> i % step == 0 || i == twp.points.lastIndex }
        } else twp.points

        // Build one feature list per speed bracket
        val bracketFeatures: Array<MutableList<Feature>> = Array(7) { mutableListOf() }
        for (i in 0 until points.size - 1) {
            val avgSpeedMs = (points[i].speed + points[i + 1].speed) / 2.0
            val bracket = speedMsToBracket(avgSpeedMs)
            val props = JsonObject().apply { addProperty("tripId", twp.trip.id.toString()) }
            bracketFeatures[bracket].add(
                Feature.fromGeometry(
                    LineString.fromLngLats(listOf(
                        Point.fromLngLat(points[i].longitude, points[i].latitude),
                        Point.fromLngLat(points[i + 1].longitude, points[i + 1].latitude)
                    )),
                    props
                )
            )
        }

        // One source + layer per bracket that has any segments
        bracketFeatures.forEachIndexed { bracketIdx, features ->
            if (features.isEmpty()) return@forEachIndexed
            val srcId = "$HM_SRC_PREFIX${twp.trip.id}-$bracketIdx"
            val lyrId = "$HM_LYR_PREFIX${twp.trip.id}-$bracketIdx"
            try {
                style.addSource(GeoJsonSource(srcId, FeatureCollection.fromFeatures(features)))
                style.addLayer(
                    LineLayer(lyrId, srcId).withProperties(
                        PropertyFactory.lineColor(HEATMAP_COLORS[bracketIdx]),
                        PropertyFactory.lineWidth(3.5f),
                        PropertyFactory.lineCap("round"),
                        PropertyFactory.lineJoin("round")
                    )
                )
                state.heatmapLayerIds.add(lyrId)
                state.heatmapSrcIds.add(srcId)
            } catch (_: Exception) {}
        }
    }
}
