package com.makan.roadpulse.presentation.tripdetail

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
import com.makan.roadpulse.domain.model.LocationPoint
import com.makan.roadpulse.presentation.map.DARK_STYLE_URL
import com.makan.roadpulse.presentation.map.getMapStyleUrl
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun TripDetailMapView(
    modifier: Modifier = Modifier,
    points: List<LocationPoint>,
    scrubIndex: Int,
    styleUrl: String = DARK_STYLE_URL
) {
    val context = LocalContext.current
    val mapRef   = remember { mutableStateOf<MapLibreMap?>(null) }
    val styleRef = remember { mutableStateOf<Style?>(null) }

    val latestPoints   = rememberUpdatedState(points)
    val latestScrubIdx = rememberUpdatedState(scrubIndex)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                mapRef.value = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(styleUrl) { style ->
                    styleRef.value = style
                    drawTripRoute(style, map, latestPoints.value)
                    updateScrubDot(style, latestPoints.value, latestScrubIdx.value)
                }
            }
        }
    }

    // Redraw when points or style change
    LaunchedEffect(styleRef.value, points) {
        val style = styleRef.value ?: return@LaunchedEffect
        val map   = mapRef.value  ?: return@LaunchedEffect
        drawTripRoute(style, map, points)
        updateScrubDot(style, points, scrubIndex)
    }

    // Move scrub dot when slider changes
    LaunchedEffect(styleRef.value, scrubIndex) {
        val style = styleRef.value ?: return@LaunchedEffect
        updateScrubDot(style, points, scrubIndex)
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

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun drawTripRoute(style: Style, map: MapLibreMap, points: List<LocationPoint>) {
    // Clear previous layers/sources
    listOf("trip-route-layer", "trip-start-circle", "trip-start-label",
           "trip-end-circle", "trip-end-label", "scrub-dot-layer")
        .forEach { id -> try { style.removeLayer(id) } catch (e: Exception) {} }
    listOf("trip-route-src", "trip-start-src", "trip-end-src", "scrub-dot-src")
        .forEach { id -> try { style.removeSource(id) } catch (e: Exception) {} }

    if (points.size < 2) return

    // Route polyline
    val pts     = points.map { Point.fromLngLat(it.longitude, it.latitude) }
    val routeFeat = Feature.fromGeometry(LineString.fromLngLats(pts))
    style.addSource(GeoJsonSource("trip-route-src", FeatureCollection.fromFeatures(listOf(routeFeat))))
    style.addLayer(
        LineLayer("trip-route-layer", "trip-route-src").withProperties(
            PropertyFactory.lineColor("#5DCAA5"),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineCap("round"),
            PropertyFactory.lineJoin("round")
        )
    )

    // Start marker
    val first = points.first()
    style.addSource(GeoJsonSource("trip-start-src", Feature.fromGeometry(
        Point.fromLngLat(first.longitude, first.latitude)
    )))
    style.addLayer(
        CircleLayer("trip-start-circle", "trip-start-src").withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor("#378ADD"),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF")
        )
    )
    style.addLayer(
        SymbolLayer("trip-start-label", "trip-start-src").withProperties(
            PropertyFactory.textField("Start"),
            PropertyFactory.textColor("#B5D4F4"),
            PropertyFactory.textSize(9f),
            PropertyFactory.textOffset(arrayOf(1.5f, 0f)),
            PropertyFactory.textAnchor("left"),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )
    )

    // End marker
    val last = points.last()
    style.addSource(GeoJsonSource("trip-end-src", Feature.fromGeometry(
        Point.fromLngLat(last.longitude, last.latitude)
    )))
    style.addLayer(
        CircleLayer("trip-end-circle", "trip-end-src").withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor("#D85A30"),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF")
        )
    )
    style.addLayer(
        SymbolLayer("trip-end-label", "trip-end-src").withProperties(
            PropertyFactory.textField("End"),
            PropertyFactory.textColor("#F5C4B3"),
            PropertyFactory.textSize(9f),
            PropertyFactory.textOffset(arrayOf(1.5f, 0f)),
            PropertyFactory.textAnchor("left"),
            PropertyFactory.textIgnorePlacement(true),
            PropertyFactory.textAllowOverlap(true)
        )
    )

    // Initial scrub dot at first point
    style.addSource(GeoJsonSource("scrub-dot-src", Feature.fromGeometry(
        Point.fromLngLat(first.longitude, first.latitude)
    )))
    style.addLayer(
        CircleLayer("scrub-dot-layer", "scrub-dot-src").withProperties(
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleColor("#FFFFFF"),
            PropertyFactory.circleStrokeWidth(2.5f),
            PropertyFactory.circleStrokeColor("#5DCAA5")
        )
    )

    // Fit camera to route bounds
    try {
        val boundsBuilder = LatLngBounds.Builder()
        points.forEach { boundsBuilder.include(LatLng(it.latitude, it.longitude)) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 80))
    } catch (e: Exception) { /* invalid bounds */ }
}

private fun updateScrubDot(style: Style, points: List<LocationPoint>, index: Int) {
    if (points.isEmpty()) return
    val safeIndex = index.coerceIn(0, points.lastIndex)
    val pt = points[safeIndex]
    try {
        (style.getSource("scrub-dot-src") as? GeoJsonSource)?.setGeoJson(
            Feature.fromGeometry(Point.fromLngLat(pt.longitude, pt.latitude))
        )
    } catch (e: Exception) { /* source not yet added */ }
}
