package com.direccionve.estasbien.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import androidx.work.*
import com.direccionve.estasbien.data.EarthquakeRepository
import com.direccionve.estasbien.data.PlusCodeUtils
import com.direccionve.estasbien.data.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that:
 * 1. Updates user's location (GPS)
 * 2. Checks USGS for new earthquakes near the user
 * Runs every 15 minutes (minimum WorkManager interval).
 */
class EarthquakeCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val prefs = UserPreferences(applicationContext)
        val profile = prefs.userProfile.firstOrNull() ?: return Result.success()

        // 1. Update location
        var currentLat = profile.latitude
        var currentLng = profile.longitude
        try {
            val locationManager = applicationContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                currentLat = location.latitude
                currentLng = location.longitude
                val newPlusCode = PlusCodeUtils.encode(currentLat, currentLng)
                prefs.updateLocation(currentLat, currentLng, newPlusCode)
            }
        } catch (_: Exception) {
            // Use last known location
        }

        // 2. Check for earthquakes near current location
        val repo = EarthquakeRepository()
        val alertQuakes = repo.getAlertQuakes(
            userLat = currentLat,
            userLng = currentLng,
            radiusKm = 500.0,
            minMagnitude = 4.0
        )

        if (alertQuakes.isNotEmpty()) {
            val (quake, distance) = alertQuakes.first()
            val mag = quake.properties.mag ?: 0.0
            val place = quake.properties.place ?: "Venezuela"

            // Only alert if this is a new earthquake
            val lastId = prefs.lastAlertId.firstOrNull()
            if (quake.id != lastId) {
                prefs.saveAlertResponse(quake.id, false)
                NotificationHelper.showEarthquakeAlert(
                    context = applicationContext,
                    magnitude = "%.1f".format(mag),
                    place = place,
                    earthquakeId = quake.id
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "earthquake_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<EarthquakeCheckWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
