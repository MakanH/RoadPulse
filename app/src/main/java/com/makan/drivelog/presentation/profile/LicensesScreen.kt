package com.makan.drivelog.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class License(val name: String, val description: String, val url: String)

private val LICENSES = listOf(
    License("MapLibre Android SDK", "Open-source maps rendering based on MapLibre GL", "https://maplibre.org"),
    License("OpenStreetMap", "Free editable map of the world", "https://www.openstreetmap.org"),
    License("OpenRouteService", "Open-source routing engine", "https://openrouteservice.org"),
    License("Nominatim", "Geocoding and reverse geocoding using OpenStreetMap data", "https://nominatim.openstreetmap.org"),
    License("Firebase SDK", "Google Firebase Android SDK (Apache 2.0)", "https://firebase.google.com"),
    License("Hilt", "Dependency injection for Android (Apache 2.0)", "https://dagger.dev/hilt"),
    License("Coil", "Image loading library for Android (Apache 2.0)", "https://coil-kt.github.io/coil"),
    License("Jetpack Compose", "Android's modern UI toolkit (Apache 2.0)", "https://developer.android.com/jetpack/compose")
)

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface)
                }
                Text("Open source licenses", fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column {
                    LICENSES.forEachIndexed { i, license ->
                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp)) {
                            Text(license.name, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface)
                            Spacer(Modifier.height(2.dp))
                            Text(license.description, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(2.dp))
                            Text(license.url, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary)
                        }
                        if (i < LICENSES.lastIndex) {
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp), 0.5.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}
