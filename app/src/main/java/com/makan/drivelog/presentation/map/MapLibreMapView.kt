package com.makan.drivelog.presentation.map

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

internal const val DARK_STYLE_URL  = "https://basemaps.cartocdn.com/gl/dark-matter-gl-style/style.json"
internal const val LIGHT_STYLE_URL = "https://basemaps.cartocdn.com/gl/positron-gl-style/style.json"
private const val SOURCE_PREFIX = "dl-src-"
private const val LAYER_PREFIX  = "dl-lyr-"

val LocalMapStyleUrl = compositionLocalOf { DARK_STYLE_URL }

fun getMapStyleUrl(pref: String): String =
    if (pref == "light") LIGHT_STYLE_URL else DARK_STYLE_URL

internal class MapDrawingState {
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
    val mapStyleUrl = LocalMapStyleUrl.current
    val mapStyleState = remember { mutableStateOf<Style?>(null) }
    val drawingState  = remember { MapDrawingState() }

    val latestTrips        = rememberUpdatedState(tripsWithPoints)
    val latestActiveTripId = rememberUpdatedState(trackingState.currentTrip?.id)
    val latestOnTripClick  = rememberUpdatedState(onTripClick)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(mapStyleUrl) { style ->
                    mapStyleState.value = style
                    setupLocationComponent(map, style, context)
                    redrawTrips(style, latestTrips.value, drawingState, latestActiveTripId.value)
                }
                map.addOnMapClickListener { latLng ->
                    val screenPt = map.projection.toScreenLocation(latLng)
                    val layerIds = drawingState.drawnTripIds.map { "$LAYER_PREFIX$it" }.toTypedArray()
                    if (layerIds.isNotEmpty()) {
                        val hits = map.queryRenderedFeatures(
                            PointF(screenPt.x, screenPt.y), *layerIds
                        )
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
    LaunchedEffect(style, tripsWithPoints, trackingState.currentTrip?.id) {
        style ?: return@LaunchedEffect
        redrawTrips(style, tripsWithPoints, drawingState, trackingState.currentTrip?.id)
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
private fun setupLocationComponent(
    map: MapLibreMap,
    style: Style,
    context: android.content.Context
) {
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
        val trip    = twp.trip
        val isActive = trip.id == activeTripId
        val color   = when { isActive -> "#5DCAA5"; index % 2 == 0 -> "#378ADD"; else -> "#D85A30" }
        val width   = if (isActive) 4f else 2f
        val opacity = when { isActive -> 1f; index % 2 == 0 -> 0.5f; else -> 0.4f }

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
