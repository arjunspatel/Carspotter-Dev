package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class SpotCoordinates(
    val latitude: Double,
    val longitude: Double,
    val locationName: String
)

object LocationHelper {
    private const val TAG = "LocationHelper"

    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentSpotLocation(context: Context): SpotCoordinates? {
        if (!hasLocationPermission(context)) return null

        val fusedClient = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val location: Location? = withTimeoutOrNull(2500L) {
            try {
                val cts = CancellationTokenSource()
                fusedClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token).await()
            } catch (e: Exception) {
                Log.w(TAG, "getCurrentLocation failed: ${e.message}")
                null
            }
        } ?: withTimeoutOrNull(1200L) {
            try {
                fusedClient.lastLocation.await()
            } catch (e: Exception) {
                Log.w(TAG, "lastLocation failed: ${e.message}")
                null
            }
        }

        if (location == null) return null

        val label = reverseGeocodeLabel(context.applicationContext, location.latitude, location.longitude)
        return SpotCoordinates(
            latitude = location.latitude,
            longitude = location.longitude,
            locationName = label
        )
    }

    private suspend fun reverseGeocodeLabel(context: Context, lat: Double, lng: Double): String {
        val fallback = String.format(Locale.US, "%.4f, %.4f", lat, lng)
        if (!Geocoder.isPresent()) return fallback

        return withTimeoutOrNull(1500L) {
            withContext(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val addresses: List<Address> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine { cont ->
                            geocoder.getFromLocation(lat, lng, 1) { list ->
                                if (cont.isActive) cont.resume(list)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(lat, lng, 1).orEmpty()
                    }
                    val addr = addresses.firstOrNull() ?: return@withContext fallback
                    val city = addr.locality ?: addr.subAdminArea ?: addr.featureName
                    val region = addr.adminArea ?: addr.countryCode
                    when {
                        !city.isNullOrBlank() && !region.isNullOrBlank() -> "$city, $region"
                        !city.isNullOrBlank() -> city
                        !region.isNullOrBlank() -> region
                        else -> fallback
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Geocoder failed: ${e.message}")
                    fallback
                }
            }
        } ?: fallback
    }

    /**
     * Computes great-circle distance in miles between two lat/lng coordinates.
     */
    fun distanceMiles(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val earthRadiusMiles = 3958.8
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusMiles * c
    }

    /**
     * Computes screen-friendly compass bearing in radians (0 = North/up, PI/2 = East/right).
     */
    fun bearingRadians(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val dLambda = Math.toRadians(lng2 - lng1)
        val y = sin(dLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(dLambda)
        return atan2(y, x)
    }
}
