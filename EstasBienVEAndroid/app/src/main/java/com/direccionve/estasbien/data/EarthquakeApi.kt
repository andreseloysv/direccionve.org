package com.direccionve.estasbien.data

import com.direccionve.estasbien.data.model.UsgsResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * USGS Earthquake API — queries recent earthquakes near Venezuela.
 *
 * Venezuela bounding box: lat 0.5–12.5, lng -73.5–-59.5
 * We extend slightly to catch nearby events.
 */
interface EarthquakeApi {

    @GET("fdsnws/event/1/query")
    suspend fun getRecentQuakes(
        @Query("format") format: String = "geojson",
        @Query("minlatitude") minLat: Double = 0.0,
        @Query("maxlatitude") maxLat: Double = 15.0,
        @Query("minlongitude") minLng: Double = -76.0,
        @Query("maxlongitude") maxLng: Double = -58.0,
        @Query("minmagnitude") minMag: Double = 3.0,
        @Query("orderby") orderBy: String = "time",
        @Query("limit") limit: Int = 20,
        @Query("starttime") startTime: String? = null
    ): UsgsResponse

    companion object {
        const val BASE_URL = "https://earthquake.usgs.gov/"
    }
}
