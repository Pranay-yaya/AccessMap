package com.accessmap.app.routing

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

sealed class GpsOriginResult {
    data class Success(val node: CampusNode, val distanceMeters: Double, val location: Location) : GpsOriginResult()
    data class OutsideCampus(val nearestNode: CampusNode, val distanceMeters: Double) : GpsOriginResult()
    data object NoPermission : GpsOriginResult()
    data object LocationUnavailable : GpsOriginResult()
}

/**
 * Shared GPS helper using Android's built-in LocationManager (no Google Play Services or API key).
 */
object CampusLocationHelper {

    // Maximum distance from any LPU node to consider the user "on campus"
    const val MAX_CAMPUS_RADIUS_METERS = 5000.0

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER
        )
        var best: Location? = null
        for (provider in providers) {
            val loc = try {
                if (lm.isProviderEnabled(provider)) lm.getLastKnownLocation(provider) else null
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            } ?: continue
            if (best == null || loc.accuracy < best.accuracy || loc.time > best.time + 60_000L) {
                best = loc
            }
        }
        return best
    }

    fun nearestCampusNodeFromGps(
        context: Context,
        graph: CampusGraph,
        knownLocation: Location? = null
    ): GpsOriginResult {
        if (!hasLocationPermission(context)) return GpsOriginResult.NoPermission
        val loc = knownLocation ?: getBestLastKnownLocation(context) ?: return GpsOriginResult.LocationUnavailable
        val (nearest, dist) = graph.nearestNodeWithDistance(loc.latitude, loc.longitude)
            ?: return GpsOriginResult.LocationUnavailable
        return if (dist <= MAX_CAMPUS_RADIUS_METERS) {
            GpsOriginResult.Success(nearest, dist, loc)
        } else {
            GpsOriginResult.OutsideCampus(nearest, dist)
        }
    }
}
