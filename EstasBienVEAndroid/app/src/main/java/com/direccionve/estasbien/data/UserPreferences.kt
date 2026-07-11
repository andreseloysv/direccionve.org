package com.direccionve.estasbien.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.firstOrNull
import androidx.datastore.preferences.preferencesDataStore
import com.direccionve.estasbien.data.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "estasbien_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val NAME = stringPreferencesKey("user_name")
        val PHONE = stringPreferencesKey("user_phone")
        val PLUS_CODE = stringPreferencesKey("user_plus_code")
        val LATITUDE = doublePreferencesKey("user_lat")
        val LONGITUDE = doublePreferencesKey("user_lng")
        val REGISTERED = booleanPreferencesKey("is_registered")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_ALERT_ID = stringPreferencesKey("last_alert_id")
        val LAST_ALERT_RESPONDED = booleanPreferencesKey("last_alert_responded")
        val LAST_LOCATION_UPDATE = longPreferencesKey("last_location_update")
    }

    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_DONE] == true
    }

    val isRegistered: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.REGISTERED] == true
    }

    val userProfile: Flow<UserProfile?> = context.dataStore.data.map { prefs ->
        val name = prefs[Keys.NAME] ?: return@map null
        val phone = prefs[Keys.PHONE] ?: return@map null
        val plusCode = prefs[Keys.PLUS_CODE] ?: return@map null
        UserProfile(
            name = name,
            phone = phone,
            plusCode = plusCode,
            latitude = prefs[Keys.LATITUDE] ?: 0.0,
            longitude = prefs[Keys.LONGITUDE] ?: 0.0
        )
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = profile.name
            prefs[Keys.PHONE] = profile.phone
            prefs[Keys.PLUS_CODE] = profile.plusCode
            prefs[Keys.LATITUDE] = profile.latitude
            prefs[Keys.LONGITUDE] = profile.longitude
            prefs[Keys.REGISTERED] = true
        }
    }

    suspend fun saveAlertResponse(earthquakeId: String, responded: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_ALERT_ID] = earthquakeId
            prefs[Keys.LAST_ALERT_RESPONDED] = responded
        }
    }

    suspend fun updateLocation(lat: Double, lng: Double, plusCode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LATITUDE] = lat
            prefs[Keys.LONGITUDE] = lng
            prefs[Keys.PLUS_CODE] = plusCode
            prefs[Keys.LAST_LOCATION_UPDATE] = System.currentTimeMillis()
        }
    }

    suspend fun updateName(name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NAME] = name
        }
    }

    val lastAlertId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_ALERT_ID]
    }

    val lastLocationUpdate: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[Keys.LAST_LOCATION_UPDATE]
    }

    suspend fun setOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_DONE] = true
        }
    }

    suspend fun clearAllData() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun exportCsv(): String {
        val prefs = context.dataStore.data.firstOrNull() ?: return ""
        val name = prefs[Keys.NAME] ?: ""
        val phone = prefs[Keys.PHONE] ?: ""
        val plusCode = prefs[Keys.PLUS_CODE] ?: ""
        val lat = prefs[Keys.LATITUDE] ?: 0.0
        val lng = prefs[Keys.LONGITUDE] ?: 0.0
        val lastAlert = prefs[Keys.LAST_ALERT_ID] ?: ""
        val lastResponded = prefs[Keys.LAST_ALERT_RESPONDED] ?: false

        val header = "nombre,telefono,plus_code,latitud,longitud,ultima_alerta_id,ultima_alerta_respondida"
        val row = "\"$name\",\"$phone\",\"$plusCode\",$lat,$lng,\"$lastAlert\",$lastResponded"
        return "$header\n$row\n"
    }
}
