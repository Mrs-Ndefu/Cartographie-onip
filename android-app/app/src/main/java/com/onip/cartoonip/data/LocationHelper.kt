package com.onip.cartoonip.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** [stale] = position reprise du cache du téléphone, et non relevée à l'instant. */
data class GpsResult(val latitude: Double, val longitude: Double, val accuracyMeters: Float, val stale: Boolean = false)

/**
 * GPS via l'API Android native (LocationManager) plutôt que Play Services Location — évite une
 * dépendance supplémentaire pour un besoin ponctuel (un relevé à la demande, pas un suivi continu).
 */
class LocationHelper(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /**
     * Écoute le GPS (et le réseau en complément) pendant au plus [maxWaitMs] et garde le relevé le
     * plus précis : le premier relevé reçu est souvent grossier (50 à 500 m) et s'affine en quelques
     * secondes. S'arrête dès qu'un relevé atteint [targetAccuracyMeters]. Renvoie null si aucun
     * relevé n'est arrivé dans le délai.
     */
    suspend fun bestFix(targetAccuracyMeters: Float = 20f, maxWaitMs: Long = 30_000): GpsResult? {
        if (!hasPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { manager.isProviderEnabled(it) }
        if (providers.isEmpty()) return null

        var best: Location? = null
        val listener = LocationListener { location ->
            val current = best
            if (current == null || location.accuracy < current.accuracy) best = location
        }
        try {
            providers.forEach { manager.requestLocationUpdates(it, 0L, 0f, listener, Looper.getMainLooper()) }
            withTimeoutOrNull(maxWaitMs) {
                while (best.let { it == null || it.accuracy > targetAccuracyMeters }) delay(250)
            }
        } catch (e: SecurityException) {
            return null
        } finally {
            manager.removeUpdates(listener)
        }
        return best?.let { GpsResult(it.latitude, it.longitude, it.accuracy) }
    }

    /**
     * Dernière position connue, seulement si elle date de moins de [maxAgeMs] — au-delà, l'agent a
     * pu se déplacer et la position ne correspond plus au foyer. Repli si [bestFix] n'aboutit pas.
     */
    fun recentLastKnown(maxAgeMs: Long = 2 * 60_000): GpsResult? {
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
            .filter { SystemClock.elapsedRealtimeNanos() - it.elapsedRealtimeNanos <= maxAgeMs * 1_000_000 }
            .minByOrNull { it.accuracy }
        return location?.let { GpsResult(it.latitude, it.longitude, it.accuracy, stale = true) }
    }
}
