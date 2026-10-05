package com.accessmap.app.routing

import kotlin.math.*

/**
 * Straight-line (great-circle) distance between two GPS points, in meters.
 * Good enough for outdoor building-to-building paths. Not meaningful for
 * indoor/underground segments (GPS doesn't track reliably there) — for
 * those, pace out the distance manually and pass it in directly when
 * building the CampusEdge instead of using this function.
 */
fun haversineDistanceMeters(
    lat1: Double, lon1: Double,
    lat2: Double, lon2: Double
): Double {
    val earthRadiusM = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadiusM * c
}

/** Convenience overload using your CampusNode objects directly. */
fun haversineDistanceMeters(a: CampusNode, b: CampusNode): Double =
    haversineDistanceMeters(a.latitude, a.longitude, b.latitude, b.longitude)
