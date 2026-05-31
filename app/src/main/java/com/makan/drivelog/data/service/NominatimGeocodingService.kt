package com.makan.drivelog.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NominatimGeocodingService @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = mutableMapOf<String, String?>()
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
}
