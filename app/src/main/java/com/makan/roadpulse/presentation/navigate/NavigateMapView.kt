package com.makan.roadpulse.presentation.navigate

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.makan.roadpulse.domain.model.Route
import com.makan.roadpulse.presentation.map.DARK_STYLE_URL
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun NavigateMapView(
    modifier: Modifier = Modifier,
    route: Route?,
    isNavigating: Boolean,
    styleUrl: String = DARK_STYLE_URL,
    recenterTrigger: Int = 0
) {
    val context = LocalContext.current
    val mapRef   = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val latestRoute        = rememberUpdatedState(route)
    val latestIsNavigating = rememberUpdatedState(isNavigating)
    val latestStyleUrl     = rememberUpdatedState(styleUrl)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                mapRef.value = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(latestStyleUrl.value) { style ->
                    styleRef.value = style
                    setupLocationComponent(map, style, context)
                    latestRoute.value?.let {
                        drawRoute(style, map, it, latestIsNavigating.value)
                    }
                }
            }
        }
    }

    // Redraw route when it changes
    LaunchedEffect(styleRef.value, route, isNavigating) {
        val style = styleRef.value ?: return@LaunchedEffect
        val map   = mapRef.value  ?: return@LaunchedEffect
        if (route != null) {
            drawRoute(style, map, route, isNavigating)
        } else {
            clearRoute(style)
        }
    }

    // Switch camera mode when navigation starts/ends
    LaunchedEffect(mapRef.value, isNavigating) {
        val map = mapRef.value ?: return@LaunchedEffect
        try {
            if (isNavigating) {
                map.locationComponent.cameraMode = CameraMode.TRACKING_GPS
                map.locationComponent.zoomWhileTracking(16.0)
                map.locationComponent.tiltWhileTracking(45.0)
            } else {
                map.locationComponent.cameraMode = CameraMode.NONE
                // If route loaded, re-fit camera to full route
                val style = styleRef.value
                val r     = route
                if (style != null && r != null) fitCamera(map, r)
            }
        } catch (_: Exception) {}
    }

    // Animate camera to user's current location on FAB tap
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger == 0) return@LaunchedEffect
        val map = mapRef.value ?: return@LaunchedEffect
        try {
            val loc = map.locationComponent.lastKnownLocation ?: return@LaunchedEffect
            map.animateCamera(
                CameraUpdateFactory.newLatLngZoom(MapLatLng(loc.latitude, loc.longitude), 15.0),
                500
            )
        } catch (_: Exception) {}
    }

    // Reload map style reactively when the preference changes
    LaunchedEffect(styleUrl) {
        val map = mapRef.value ?: return@LaunchedEffect
        map.setStyle(styleUrl) { style ->
            styleRef.value = style
            setupLocationComponent(map, style, context)
        }
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

    AndroidView(
        factory  = { mapView },
        update   = { _ ->
            val latestUrl = latestStyleUrl.value
            mapRef.value?.let { map ->
                val currentUri = map.style?.uri
                if (currentUri != null && currentUri != latestUrl) {
                    map.setStyle(latestUrl) { style ->
                        styleRef.value = style
                        setupLocationComponent(map, style, context)
                    }
                }
            }
        },
        modifier = modifier
    )
}

@SuppressLint("MissingPermission")
private fun setupLocationComponent(map: MapLibreMap, style: Style, context: android.content.Context) {
    try {
        val opts = LocationComponentOptions.builder(context)
            .pulseEnabled(true)
            .pulseColor(AndroidColor.parseColor("#5DCAA5"))
            .pulseAlpha(0.4f)
            .build()
        val activation = LocationComponentActivationOptions
            .builder(context, style)
            .locationComponentOptions(opts)
            .useDefaultLocationEngine(true)
            .build()
        map.locationComponent.activateLocationComponent(activation)
        map.locationComponent.isLocationComponentEnabled = true
        map.locationComponent.cameraMode  = CameraMode.NONE
        map.locationComponent.renderMode  = RenderMode.COMPASS
    } catch (_: Exception) {}
}

private fun drawRoute(style: Style, map: MapLibreMap, route: Route, isNavigating: Boolean) {
    clearRoute(style)

    val pts = route.geometry.map { Point.fromLngLat(it.lon, it.lat) }

    // Route polyline
    style.addSource(GeoJsonSource("nav-route-src",
        FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(LineString.fromLngLats(pts))))))
    style.addLayer(LineLayer("nav-route-layer", "nav-route-src").withProperties(
        PropertyFactory.lineColor("#5DCAA5"),
        PropertyFactory.lineWidth(4f),
        PropertyFactory.lineCap("round"),
        PropertyFactory.lineJoin("round")
    ))

    // Start marker (blue)
    val first = route.geometry.first()
    style.addSource(GeoJsonSource("nav-start-src",
        Feature.fromGeometry(Point.fromLngLat(first.lon, first.lat))))
    style.addLayer(CircleLayer("nav-start-circle", "nav-start-src").withProperties(
        PropertyFactory.circleRadius(8f),
        PropertyFactory.circleColor("#378ADD"),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleStrokeColor("#FFFFFF")
    ))

    // End marker (coral)
    val last = route.geometry.last()
    style.addSource(GeoJsonSource("nav-end-src",
        Feature.fromGeometry(Point.fromLngLat(last.lon, last.lat))))
    style.addLayer(CircleLayer("nav-end-circle", "nav-end-src").withProperties(
        PropertyFactory.circleRadius(8f),
        PropertyFactory.circleColor("#D85A30"),
        PropertyFactory.circleStrokeWidth(2f),
        PropertyFactory.circleStrokeColor("#FFFFFF")
    ))

    if (!isNavigating) fitCamera(map, route)
}

private fun fitCamera(map: MapLibreMap, route: Route) {
    try {
        val builder = LatLngBounds.Builder()
        route.geometry.forEach { builder.include(MapLatLng(it.lat, it.lon)) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
    } catch (_: Exception) {}
}

private fun clearRoute(style: Style) {
    listOf("nav-route-layer", "nav-start-circle", "nav-end-circle")
        .forEach { try { style.removeLayer(it) } catch (_: Exception) {} }
    listOf("nav-route-src", "nav-start-src", "nav-end-src")
        .forEach { try { style.removeSource(it) } catch (_: Exception) {} }
}
