package com.onip.cartoonip.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine

data class GpsResult(val latitude: Double, val longitude: Double, val accuracyMeters: Float)

/**
 * GPS via l'API Android native (LocationManager) plutôt que Play Services Location — évite une
 * dépendance supplémentaire pour un besoin ponctuel (un relevé à la demande, pas un suivi continu).
 */
class LocationHelper(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Suspend jusqu'à obtenir un relevé — à envelopper avec un timeout côté appelant. */
    suspend fun awaitFix(): GpsResult? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }

        return suspendCancellableCoroutine { cont ->
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (cont.isActive) cont.resumeWith(Result.success(GpsResult(location.latitude, location.longitude, location.accuracy)))
                }
            }
            try {
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            } catch (e: SecurityException) {
                cont.resumeWith(Result.success(null))
                return@suspendCancellableCoroutine
            }
            cont.invokeOnCancellation { manager.removeUpdates(listener) }
        }
    }

    /** Dernière position connue — utilisée comme repli si awaitFix() dépasse le délai imparti. */
    fun lastKnown(): GpsResult? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider ->
                try {
                    manager.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    null
                }
            }
            .maxByOrNull { it.time }
        return location?.let { GpsResult(it.latitude, it.longitude, it.accuracy) }
    }
}
