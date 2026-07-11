package com.direccionve.estasbien.data

/**
 * Minimal Plus Code encoder/decoder — same algorithm as DirecciónVE.js
 */
object PlusCodeUtils {

    private const val ALPHABET = "23456789CFGHJMPQRVWX"
    private val PAIR_RESOLUTIONS = doubleArrayOf(20.0, 1.0, 0.05, 0.0025, 0.000125)

    fun encode(lat: Double, lng: Double, length: Int = 10): String {
        var adjustedLat = lat.coerceIn(-90.0, 90.0)
        var adjustedLng = lng
        while (adjustedLng < -180) adjustedLng += 360
        while (adjustedLng >= 180) adjustedLng -= 360

        if (adjustedLat == 90.0) {
            val p = ((length.coerceAtMost(10) - 1) / 2)
            adjustedLat -= 0.9 * PAIR_RESOLUTIONS[p]
        }

        val sb = StringBuilder()
        var aLat = adjustedLat + 90
        var aLng = adjustedLng + 180
        var i = 0

        while (i < length) {
            if (i < 10) {
                val p = i / 2
                val r = PAIR_RESOLUTIONS[p]
                val dLat = (aLat / r).toInt().coerceAtMost(19)
                val dLng = (aLng / r).toInt().coerceAtMost(19)
                aLat -= dLat * r
                aLng -= dLng * r
                sb.append(ALPHABET[dLat])
                sb.append(ALPHABET[dLng])
                i += 2
                if (i == 8) sb.append('+')
            } else {
                var rLat = PAIR_RESOLUTIONS[4]
                var rLng = PAIR_RESOLUTIONS[4]
                for (g in 10 until i) { rLat /= 5; rLng /= 4 }
                val gLatR = rLat / 5
                val gLngR = rLng / 4
                val row = (aLat / gLatR).toInt().coerceAtMost(4)
                val col = (aLng / gLngR).toInt().coerceAtMost(3)
                aLat -= row * gLatR
                aLng -= col * gLngR
                sb.append(ALPHABET[row * 4 + col])
                i++
            }
        }

        if (!sb.contains('+')) sb.append('+')
        return sb.toString()
    }

    fun decode(code: String): LatLng? {
        val clean = code.uppercase().replace("\\s".toRegex(), "").replace("+", "")
        if (clean.length < 8) return null

        var lat = -90.0
        var lng = -180.0
        var latR = 0.0
        var lngR = 0.0

        val pairLen = clean.length.coerceAtMost(10)
        var i = 0
        while (i < pairLen) {
            val p = i / 2
            latR = PAIR_RESOLUTIONS[p]
            lngR = PAIR_RESOLUTIONS[p]
            val idxLat = ALPHABET.indexOf(clean[i])
            val idxLng = ALPHABET.indexOf(clean[i + 1])
            if (idxLat < 0 || idxLng < 0) return null
            lat += idxLat * latR
            lng += idxLng * lngR
            i += 2
        }

        return LatLng(lat + latR / 2, lng + lngR / 2)
    }

    fun isValid(code: String): Boolean {
        if (code.length < 8) return false
        val upper = code.uppercase().replace("\\s".toRegex(), "")
        if (!upper.contains('+')) return false
        return upper.replace("+", "").all { it in ALPHABET }
    }

    data class LatLng(val latitude: Double, val longitude: Double)
}
