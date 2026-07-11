package com.direccionve.estasbien.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * USGS Earthquake API response models.
 * Endpoint: https://earthquake.usgs.gov/fdsnws/event/1/query
 */

@JsonClass(generateAdapter = true)
data class UsgsResponse(
    val type: String,
    val features: List<UsgsFeature>
)

@JsonClass(generateAdapter = true)
data class UsgsFeature(
    val id: String,
    val properties: UsgsProperties,
    val geometry: UsgsGeometry
)

@JsonClass(generateAdapter = true)
data class UsgsProperties(
    val mag: Double?,
    val place: String?,
    val time: Long,
    val updated: Long?,
    val alert: String?,
    val tsunami: Int?,
    val title: String?,
    @Json(name = "felt") val feltReports: Int?
)

@JsonClass(generateAdapter = true)
data class UsgsGeometry(
    val coordinates: List<Double> // [lng, lat, depth]
) {
    val longitude: Double get() = coordinates.getOrElse(0) { 0.0 }
    val latitude: Double get() = coordinates.getOrElse(1) { 0.0 }
    val depthKm: Double get() = coordinates.getOrElse(2) { 0.0 }
}
