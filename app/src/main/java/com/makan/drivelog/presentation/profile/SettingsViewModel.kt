package com.makan.drivelog.presentation.profile

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.makan.drivelog.data.local.PreferencesManager
import com.makan.drivelog.data.service.AuthService
import com.makan.drivelog.data.service.NominatimGeocodingService
import com.makan.drivelog.data.service.UserProfileService
import com.makan.drivelog.domain.repository.TripRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val prefs: PreferencesManager,
    private val repository: TripRepository,
    private val geocodingService: NominatimGeocodingService,
    private val authService: AuthService,
    private val userProfileService: UserProfileService,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _snackbar = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val snackbar = _snackbar.asSharedFlow()

    // ── Setters ───────────────────────────────────────────────────────────────

    fun cycleTheme()                             { viewModelScope.launch {
        val options = listOf("system", "dark", "light")
        val cur = prefs.theme.first()
        prefs.setTheme(options[(options.indexOf(cur) + 1) % options.size])
    }}
    fun cycleMapStyle()                          { viewModelScope.launch {
        prefs.setMapStyle(if (prefs.mapStyle.first() == "dark") "light" else "dark")
    }}
    fun setAutoTracking(v: Boolean)              { viewModelScope.launch { prefs.setAutoTracking(v) } }
    fun cycleGpsAccuracy()                       { viewModelScope.launch {
        prefs.setGpsAccuracy(if (prefs.gpsAccuracy.first() == "high") "balanced" else "high")
    }}
    fun cycleMinTripDistance()                   { viewModelScope.launch {
        val options = listOf("100m", "500m", "1 km")
        val cur = prefs.minTripDistance.first()
        prefs.setMinTripDistance(options[(options.indexOf(cur) + 1) % options.size])
    }}
    fun setSpeedUnit(v: String)                  { viewModelScope.launch { prefs.setSpeedUnit(v) } }
    fun setDistanceUnit(v: String)               { viewModelScope.launch { prefs.setDistanceUnit(v) } }
    fun setBackgroundNotification(v: Boolean)    { viewModelScope.launch { prefs.setBackgroundNotification(v) } }
    fun setTripSummaryNotification(v: Boolean)   { viewModelScope.launch { prefs.setTripSummaryNotification(v) } }
    fun setFriendActivityNotification(v: Boolean){ viewModelScope.launch { prefs.setFriendActivityNotification(v) } }
    fun setShareLiveLocation(v: Boolean)         { viewModelScope.launch { prefs.setShareLiveLocation(v) } }
    fun cycleProfileVisibility()                 { viewModelScope.launch {
        val cur = prefs.profileVisibility.first()
        val next = if (cur == "everyone") "friends only" else "everyone"
        prefs.setProfileVisibility(next)
        auth.currentUser?.uid?.let { uid -> userProfileService.updateVisibility(uid, next) }
    }}

    fun signOut() = authService.signOut()

    fun deleteAllData() { viewModelScope.launch { repository.deleteAllData() } }

    fun deleteAccountAndAllData() { viewModelScope.launch {
        repository.deleteAllData()
        authService.deleteAccount()
    }}

    fun clearGeocodingCache() {
        geocodingService.clearCache()
        _snackbar.tryEmit("Cache cleared")
    }

    fun exportTrips() { viewModelScope.launch {
        val trips = repository.getAllTrips().first()
        val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val csv = buildString {
            appendLine("tripName,startTime,endTime,distanceKm,avgSpeedKmh,topSpeedKmh,durationMinutes")
            trips.forEach { t ->
                val end = t.endTime
                val durationMin = if (end != null) (end - t.startTime) / 60_000L else 0L
                appendLine("\"${t.name}\",${dateFmt.format(Date(t.startTime))},${if (end != null) dateFmt.format(Date(end)) else ""},%.3f,%.1f,%.1f,$durationMin"
                    .format(t.distance / 1_000.0, t.avgSpeed * 3.6, t.topSpeed * 3.6))
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "drivelog_trips_${System.currentTimeMillis()}.csv")
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out).also { it.write(csv); it.flush() }
            }
            _snackbar.emit("Trips exported to Downloads")
        } else {
            _snackbar.emit("Export failed")
        }
    }}

    fun sendFeedback() {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:feedback@drivelog.app")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "DriveLog Feedback")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }
}
