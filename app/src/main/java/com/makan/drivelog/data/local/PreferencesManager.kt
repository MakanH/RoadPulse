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
        private val KEY_AUTO_TRACKING    = booleanPreferencesKey("auto_tracking")
        private val KEY_SPEED_UNIT       = stringPreferencesKey("speed_unit")
        private val KEY_DISTANCE_UNIT    = stringPreferencesKey("distance_unit")
        private val KEY_BG_NOTIFICATION  = booleanPreferencesKey("bg_notification")
    }

    val autoTracking:           Flow<Boolean> = context.dataStore.data.map { it[KEY_AUTO_TRACKING]   ?: true }
    val speedUnit:              Flow<String>  = context.dataStore.data.map { it[KEY_SPEED_UNIT]      ?: "km/h" }
    val distanceUnit:           Flow<String>  = context.dataStore.data.map { it[KEY_DISTANCE_UNIT]   ?: "km" }
    val backgroundNotification: Flow<Boolean> = context.dataStore.data.map { it[KEY_BG_NOTIFICATION] ?: true }

    suspend fun setAutoTracking(v: Boolean)          { context.dataStore.edit { it[KEY_AUTO_TRACKING]   = v } }
    suspend fun setSpeedUnit(v: String)              { context.dataStore.edit { it[KEY_SPEED_UNIT]      = v } }
    suspend fun setDistanceUnit(v: String)           { context.dataStore.edit { it[KEY_DISTANCE_UNIT]   = v } }
    suspend fun setBackgroundNotification(v: Boolean) { context.dataStore.edit { it[KEY_BG_NOTIFICATION] = v } }
}
