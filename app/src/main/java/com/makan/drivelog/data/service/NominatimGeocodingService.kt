package com.makan.drivelog.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class SearchResult(
    val displayName: String,
    val shortName: String,
    val lat: Double,
    val lon: Double
)

@Singleton
class NominatimGeocodingService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, String?>()

    fun clearCache() { cache.clear() }
    private val rateMutex = Mutex()
    private var lastRequestMs = 0L

    suspend fun getLocationName(lat: Double, lon: Double): String? {
        val key = "%.5f,%.5f".format(lat, lon)
        if (cache.containsKey(key)) return cache[key]

        rateMutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestMs
            if (elapsed < 1_000L) delay(1_000L - elapsed)
            lastRequestMs = System.currentTimeMillis()
        }

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/reverse?lat=$lat&lon=$lon&format=json&zoom=16")
                    .header("User-Agent", "DriveLog/1.0 (Android)")
                    .build()
                val body = client.newCall(request).execute().use { it.body?.string() }
                    ?: return@withContext null
                val json = JSONObject(body)
                val address = json.optJSONObject("address")
                val name = address?.optString("road")?.takeIf { it.isNotBlank() }
                    ?: json.optString("name").takeIf { it.isNotBlank() }
                    ?: address?.optString("suburb")?.takeIf { it.isNotBlank() }
                    ?: address?.optString("city")?.takeIf { it.isNotBlank() }
                cache[key] = name
                name
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun forwardGeocode(query: String): List<SearchResult> {
        if (query.isBlank()) return emptyList()

        rateMutex.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestMs
            if (elapsed < 1_000L) delay(1_000L - elapsed)
            lastRequestMs = System.currentTimeMillis()
        }

        return withContext(Dispatchers.IO) {
            try {
                val encoded = URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("https://nominatim.openstreetmap.org/search?q=$encoded&format=json&limit=5&countrycodes=ca")
                    .header("User-Agent", "DriveLog/1.0 (Android)")
                    .build()
                val body = client.newCall(request).execute().use { it.body?.string() }
                    ?: return@withContext emptyList()
                val array = JSONArray(body)
                (0 until array.length()).map { i ->
                    val obj = array.getJSONObject(i)
                    val display = obj.getString("display_name")
                    val short   = obj.optString("name").takeIf { it.isNotBlank() }
                        ?: display.split(",").firstOrNull()?.trim() ?: display
                    SearchResult(
                        displayName = display,
                        shortName   = short,
                        lat = obj.getDouble("lat"),
                        lon = obj.getDouble("lon")
                    )
                }
            } catch (_: Exception) {
                emptyList()
            }
        }
    }
}
