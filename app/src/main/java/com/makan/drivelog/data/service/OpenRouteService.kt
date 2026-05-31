package com.makan.drivelog.data.service

import com.makan.drivelog.BuildConfig
import com.makan.drivelog.domain.model.LatLng
import com.makan.drivelog.domain.model.Route
import com.makan.drivelog.domain.model.RouteStep
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenRouteService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun getRoute(
        startLat: Double, startLon: Double,
        endLat: Double,   endLon: Double
    ): Route? = withContext(Dispatchers.IO) {
        try {
            val key = BuildConfig.ORS_API_KEY
            // ORS uses lon,lat order
            val url = "https://api.openrouteservice.org/v2/directions/driving-car" +
                      "?api_key=$key&start=$startLon,$startLat&end=$endLon,$endLat"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/json, application/geo+json")
                .build()
            val body = client.newCall(request).execute().use { it.body?.string() }
                ?: return@withContext null
            parseRoute(body)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseRoute(json: String): Route? = try {
        val root    = JSONObject(json)
        val feature = root.getJSONArray("features").getJSONObject(0)

        // Geometry: coordinates are [lon, lat] pairs
        val coords   = feature.getJSONObject("geometry").getJSONArray("coordinates")
        val geometry = (0 until coords.length()).map { i ->
            val pt = coords.getJSONArray(i)
            LatLng(lat = pt.getDouble(1), lon = pt.getDouble(0))
        }

        // Summary
        val props    = feature.getJSONObject("properties")
        val summary  = props.getJSONObject("summary")

        // Steps from all segments
        val segments = props.getJSONArray("segments")
        val steps    = mutableListOf<RouteStep>()
        for (si in 0 until segments.length()) {
            val segSteps = segments.getJSONObject(si).getJSONArray("steps")
            for (sj in 0 until segSteps.length()) {
                val s     = segSteps.getJSONObject(sj)
                val wayPts = s.getJSONArray("way_points")
                steps += RouteStep(
                    instruction     = s.optString("instruction", "Continue"),
                    distanceMeters  = s.getDouble("distance"),
                    durationSeconds = s.getDouble("duration"),
                    type            = s.optInt("type", 6),
                    startPointIndex = wayPts.getInt(0),
                    endPointIndex   = wayPts.getInt(1)
                )
            }
        }

        Route(
            geometry        = geometry,
            distanceMeters  = summary.getDouble("distance"),
            durationSeconds = summary.getDouble("duration"),
            steps           = steps
        )
    } catch (_: Exception) { null }
}
