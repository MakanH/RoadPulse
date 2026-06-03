package com.makan.roadpulse.presentation.groups

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.JsonObject
import com.makan.roadpulse.domain.model.MemberLocation
import com.makan.roadpulse.presentation.map.getMapStyleUrl
import com.makan.roadpulse.presentation.profile.SettingsViewModel
import com.makan.roadpulse.presentation.theme.CoralPrimary
import com.makan.roadpulse.presentation.theme.TealPrimary
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val MEMBERS_SOURCE        = "group-members-source"
private const val MEMBERS_CIRCLE_LAYER  = "group-members-circles"
private const val MEMBERS_LABEL_LAYER   = "group-members-labels"

private val MEMBER_COLORS = listOf(
    "#5DCAA5", // teal
    "#378ADD", // blue
    "#D85A30", // coral
    "#9B59B6", // purple
    "#F39C12"  // amber
)

private fun colorForIndex(index: Int) = MEMBER_COLORS[index % MEMBER_COLORS.size]

@Composable
fun GroupMapScreen(
    onBack: () -> Unit,
    viewModel: GroupMapViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val memberLocations by viewModel.memberLocations.collectAsStateWithLifecycle()
    val group           by viewModel.group.collectAsStateWithLifecycle()
    val mapStylePref    by settingsViewModel.prefs.mapStyle.collectAsStateWithLifecycle("dark")
    val context = LocalContext.current

    val mapStyleState    = remember { mutableStateOf<Style?>(null) }
    val mapRef           = remember { mutableStateOf<MapLibreMap?>(null) }
    val centeredOnce     = remember { mutableStateOf(false) }
    val latestMapStylePref = rememberUpdatedState(mapStylePref)

    val mapView = remember {
        MapView(context).also { mv ->
            mv.onCreate(null)
            mv.getMapAsync { map: MapLibreMap ->
                mapRef.value = map
                map.uiSettings.isAttributionEnabled = false
                map.uiSettings.isLogoEnabled = false
                map.setStyle(getMapStyleUrl(latestMapStylePref.value)) { style ->
                    setupMemberLayers(style)
                    mapStyleState.value = style
                }
            }
        }
    }

    // Update member markers whenever locations change
    val style = mapStyleState.value
    LaunchedEffect(style, memberLocations) {
        if (style == null) return@LaunchedEffect
        updateMemberData(style, memberLocations)

        // Center on current user's location once
        if (!centeredOnce.value) {
            val me = memberLocations.firstOrNull { it.uid == viewModel.currentUid }
            if (me != null) {
                mapRef.value?.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(me.latitude, me.longitude), 14.0
                    )
                )
                centeredOnce.value = true
            }
        }
    }

    // Reload map style reactively when the preference changes
    LaunchedEffect(mapStylePref) {
        val map = mapRef.value ?: return@LaunchedEffect
        val styleUrl = getMapStyleUrl(mapStylePref)
        map.setStyle(styleUrl) { style ->
            setupMemberLayers(style)
            mapStyleState.value = style
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

    val isOwner = group?.ownerUid == viewModel.currentUid

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory  = { mapView },
            update   = { _ ->
                val latestUrl = getMapStyleUrl(latestMapStylePref.value)
                mapRef.value?.let { map ->
                    val currentUri = map.style?.uri
                    if (currentUri != null && currentUri != latestUrl) {
                        map.setStyle(latestUrl) { style ->
                            setupMemberLayers(style)
                            mapStyleState.value = style
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Center on me FAB
        FloatingActionButton(
            onClick = {
                val me = memberLocations.firstOrNull { it.uid == viewModel.currentUid }
                if (me != null) {
                    mapRef.value?.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(me.latitude, me.longitude), 14.0
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(start = 16.dp, end = 16.dp, bottom = 160.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TealPrimary,
            shape = CircleShape
        ) {
            Icon(Icons.Filled.GpsFixed, contentDescription = "Center on me")
        }

        // Bottom card
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Member speed row
                if (memberLocations.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        memberLocations.forEachIndexed { i, m ->
                            MemberSpeedChip(
                                member = m,
                                color = Color(android.graphics.Color.parseColor(colorForIndex(i))),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOwner) {
                        Button(
                            onClick = { viewModel.endSession(); onBack() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CoralPrimary)
                        ) { Text("End session", fontSize = 13.sp) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.stopSharing(); onBack() },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralPrimary)
                        ) { Text("Stop sharing", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemberSpeedChip(
    member: MemberLocation,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (member.isActive) 1f else 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                member.displayName.firstOrNull()?.uppercase() ?: "?",
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            "${(member.speed * 3.6).toInt()} km/h",
            fontSize = 10.sp,
            color = if (member.isActive) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── MapLibre helpers ───────────────────────────────────────────────────────────

private fun setupMemberLayers(style: Style) {
    val empty = FeatureCollection.fromFeatures(emptyList<Feature>())
    style.addSource(GeoJsonSource(MEMBERS_SOURCE, empty))

    style.addLayer(
        CircleLayer(MEMBERS_CIRCLE_LAYER, MEMBERS_SOURCE).withProperties(
            PropertyFactory.circleRadius(14f),
            PropertyFactory.circleColor(Expression.get("colorHex")),
            PropertyFactory.circleStrokeWidth(2f),
            PropertyFactory.circleStrokeColor("#FFFFFF")
        )
    )

    style.addLayer(
        SymbolLayer(MEMBERS_LABEL_LAYER, MEMBERS_SOURCE).withProperties(
            PropertyFactory.textField(Expression.get("displayName")),
            PropertyFactory.textSize(11f),
            PropertyFactory.textColor("#FFFFFF"),
            PropertyFactory.textOffset(arrayOf(0f, -2.5f)),
            PropertyFactory.textHaloColor("#000000"),
            PropertyFactory.textHaloWidth(1.5f),
            PropertyFactory.textAnchor("bottom")
        )
    )
}

@SuppressLint("RestrictedApi")
private fun updateMemberData(style: Style, members: List<MemberLocation>) {
    val features = members.mapIndexed { i, m ->
        val props = JsonObject().apply {
            addProperty("displayName", m.displayName)
            addProperty("bearing", m.bearing.toDouble())
            addProperty("colorHex", if (m.isActive) colorForIndex(i) else "#555555")
        }
        Feature.fromGeometry(Point.fromLngLat(m.longitude, m.latitude), props)
    }
    try {
        val source = style.getSourceAs<GeoJsonSource>(MEMBERS_SOURCE)
        source?.setGeoJson(FeatureCollection.fromFeatures(features))
    } catch (e: Exception) { /* style not ready yet */ }
}
