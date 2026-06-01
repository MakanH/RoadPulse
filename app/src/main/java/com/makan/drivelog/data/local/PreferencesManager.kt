package com.makan.drivelog.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "drivelog_prefs")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Existing keys
        private val KEY_AUTO_TRACKING    = booleanPreferencesKey("auto_tracking")
        private val KEY_SPEED_UNIT       = stringPreferencesKey("speed_unit")
        private val KEY_DISTANCE_UNIT    = stringPreferencesKey("distance_unit")
        private val KEY_BG_NOTIFICATION  = booleanPreferencesKey("bg_notification")
        // Appearance
        private val KEY_THEME            = stringPreferencesKey("theme")
        private val KEY_MAP_STYLE        = stringPreferencesKey("map_style")
        // Tracking
        private val KEY_GPS_ACCURACY     = stringPreferencesKey("gps_accuracy")
        private val KEY_MIN_TRIP_DIST    = stringPreferencesKey("min_trip_distance")
        // Notifications
        private val KEY_TRIP_SUMMARY_NOTIF    = booleanPreferencesKey("trip_summary_notif")
        private val KEY_FRIEND_ACTIVITY_NOTIF = booleanPreferencesKey("friend_activity_notif")
        // Privacy
        private val KEY_SHARE_LIVE_LOCATION   = booleanPreferencesKey("share_live_location")
        private val KEY_PROFILE_VISIBILITY    = stringPreferencesKey("profile_visibility")
        // Onboarding
        private val KEY_ONBOARDING_MAP      = booleanPreferencesKey("onboarding_map_seen")
        private val KEY_ONBOARDING_TRIPS    = booleanPreferencesKey("onboarding_trips_seen")
        private val KEY_ONBOARDING_NAVIGATE = booleanPreferencesKey("onboarding_navigate_seen")
        private val KEY_ONBOARDING_BOARD    = booleanPreferencesKey("onboarding_board_seen")
        private val KEY_ONBOARDING_PROFILE  = booleanPreferencesKey("onboarding_profile_seen")
    }

    // ── Flows ─────────────────────────────────────────────────────────────────

    val autoTracking:              Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_TRACKING]         ?: true }
    val speedUnit:                 Flow<String>  = context.dataStore.data.map { it[KEY_SPEED_UNIT]            ?: "km/h" }
    val distanceUnit:              Flow<String>  = context.dataStore.data.map { it[KEY_DISTANCE_UNIT]         ?: "km" }
    val backgroundNotification:    Flow<Boolean> = context.dataStore.data.map { it[KEY_BG_NOTIFICATION]       ?: true }
    val theme:                     Flow<String>  = context.dataStore.data.map { it[KEY_THEME]                 ?: "dark" }
    val mapStyle:                  Flow<String>  = context.dataStore.data.map { it[KEY_MAP_STYLE]             ?: "dark" }
    val gpsAccuracy:               Flow<String>  = context.dataStore.data.map { it[KEY_GPS_ACCURACY]          ?: "high" }
    val minTripDistance:           Flow<String>  = context.dataStore.data.map { it[KEY_MIN_TRIP_DIST]         ?: "100m" }
    val tripSummaryNotification:   Flow<Boolean> = context.dataStore.data.map { it[KEY_TRIP_SUMMARY_NOTIF]   ?: true }
    val friendActivityNotification:Flow<Boolean> = context.dataStore.data.map { it[KEY_FRIEND_ACTIVITY_NOTIF]?: true }
    val shareLiveLocation:         Flow<Boolean> = context.dataStore.data.map { it[KEY_SHARE_LIVE_LOCATION]  ?: true }
    val profileVisibility:         Flow<String>  = context.dataStore.data.map { it[KEY_PROFILE_VISIBILITY]   ?: "everyone" }
    val onboardingMapSeen:         Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_MAP]      ?: false }
    val onboardingTripsSeen:       Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_TRIPS]    ?: false }
    val onboardingNavigateSeen:    Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_NAVIGATE] ?: false }
    val onboardingBoardSeen:       Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_BOARD]    ?: false }
    val onboardingProfileSeen:     Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING_PROFILE]  ?: false }

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setAutoTracking(v: Boolean)             { context.dataStore.edit { it[KEY_AUTO_TRACKING]         = v } }
    suspend fun setSpeedUnit(v: String)                 { context.dataStore.edit { it[KEY_SPEED_UNIT]            = v } }
    suspend fun setDistanceUnit(v: String)              { context.dataStore.edit { it[KEY_DISTANCE_UNIT]         = v } }
    suspend fun setBackgroundNotification(v: Boolean)   { context.dataStore.edit { it[KEY_BG_NOTIFICATION]       = v } }
    suspend fun setTheme(v: String)                     { context.dataStore.edit { it[KEY_THEME]                 = v } }
    suspend fun setMapStyle(v: String)                  { context.dataStore.edit { it[KEY_MAP_STYLE]             = v } }
    suspend fun setGpsAccuracy(v: String)               { context.dataStore.edit { it[KEY_GPS_ACCURACY]          = v } }
    suspend fun setMinTripDistance(v: String)           { context.dataStore.edit { it[KEY_MIN_TRIP_DIST]         = v } }
    suspend fun setTripSummaryNotification(v: Boolean)  { context.dataStore.edit { it[KEY_TRIP_SUMMARY_NOTIF]   = v } }
    suspend fun setFriendActivityNotification(v: Boolean){ context.dataStore.edit { it[KEY_FRIEND_ACTIVITY_NOTIF]= v } }
    suspend fun setShareLiveLocation(v: Boolean)        { context.dataStore.edit { it[KEY_SHARE_LIVE_LOCATION]  = v } }
    suspend fun setProfileVisibility(v: String)         { context.dataStore.edit { it[KEY_PROFILE_VISIBILITY]   = v } }
    suspend fun setOnboardingMapSeen()      { context.dataStore.edit { it[KEY_ONBOARDING_MAP]      = true } }
    suspend fun setOnboardingTripsSeen()    { context.dataStore.edit { it[KEY_ONBOARDING_TRIPS]    = true } }
    suspend fun setOnboardingNavigateSeen() { context.dataStore.edit { it[KEY_ONBOARDING_NAVIGATE] = true } }
    suspend fun setOnboardingBoardSeen()    { context.dataStore.edit { it[KEY_ONBOARDING_BOARD]    = true } }
    suspend fun setOnboardingProfileSeen()  { context.dataStore.edit { it[KEY_ONBOARDING_PROFILE]  = true } }
}
