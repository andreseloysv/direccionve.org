package com.direccionve.estasbien.data.model

/**
 * User's response status after an earthquake alert.
 */
enum class ResponseStatus {
    SAFE,       // Responded "I'm OK"
    NEEDS_HELP, // Responded "I need help"
    NO_RESPONSE // Timer expired, no response
}

data class UserProfile(
    val name: String,
    val phone: String,
    val plusCode: String,
    val latitude: Double,
    val longitude: Double
)

data class UserStatus(
    val userId: String,
    val earthquakeId: String,
    val status: ResponseStatus,
    val respondedAt: Long? = null,
    val plusCode: String,
    val latitude: Double,
    val longitude: Double
)

data class AlertState(
    val earthquakeId: String,
    val magnitude: Double,
    val place: String,
    val time: Long,
    val distance: Double?, // km from user
    val responded: Boolean = false,
    val response: ResponseStatus? = null,
    val timeoutMinutes: Int = 30
)
