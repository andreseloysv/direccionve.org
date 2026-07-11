package com.direccionve.estasbien.data

import com.direccionve.estasbien.data.model.UsgsFeature
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.*

class EarthquakeRepository {

    private val api: EarthquakeApi by lazy {
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

        Retrofit.Builder()
            .baseUrl(EarthquakeApi.BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(EarthquakeApi::class.java)
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /**
     * Fetch recent earthquakes near Venezuela (last 24h, M≥3.0).
     */
    suspend fun getRecentQuakes(
        hoursBack: Int = 24,
        minMagnitude: Double = 3.0
    ): Result<List<UsgsFeature>> = withContext(Dispatchers.IO) {
        try {
            val startTime = dateFormat.format(
                Date(System.currentTimeMillis() - hoursBack * 3600_000L)
            )
            val response = api.getRecentQuakes(
                minMag = minMagnitude,
                startTime = startTime
            )
            Result.success(response.features)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if there's a significant quake (M≥4.0) near the user.
     */
    suspend fun getAlertQuakes(
        userLat: Double,
        userLng: Double,
        radiusKm: Double = 500.0,
        minMagnitude: Double = 4.0
    ): List<Pair<UsgsFeature, Double>> {
        val result = getRecentQuakes(hoursBack = 1, minMagnitude = minMagnitude)
        val quakes = result.getOrDefault(emptyList())

        return quakes.mapNotNull { quake ->
            val dist = haversineKm(
                userLat, userLng,
                quake.geometry.latitude, quake.geometry.longitude
            )
            if (dist <= radiusKm) quake to dist else null
        }.sortedBy { it.second }
    }

    companion object {
        /**
         * Haversine distance in km between two points.
         */
        fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
            val R = 6371.0
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = sin(dLat / 2).pow(2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLng / 2).pow(2)
            return R * 2 * atan2(sqrt(a), sqrt(1 - a))
        }
    }
}
