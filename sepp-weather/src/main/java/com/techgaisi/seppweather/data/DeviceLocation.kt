package com.techgaisi.seppweather.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Standort über den eingebauten LocationManager – ohne Google-Play-Dienste,
 * funktioniert daher auf jedem Tablet. Für Wetter reicht der ungefähre Standort.
 */
internal object DeviceLocation {

    private const val FRESH_ENOUGH_MS = 30 * 60 * 1000L
    private const val LOCATION_TIMEOUT_MS = 15_000L
    private const val GEOCODER_TIMEOUT_MS = 5_000L

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(context: Context): Boolean {
        val lm = context.getSystemService(LocationManager::class.java) ?: return false
        return LocationManagerCompat.isLocationEnabled(lm)
    }

    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): Location? {
        if (!hasPermission(context)) return null
        val lm = context.getSystemService(LocationManager::class.java) ?: return null

        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        ).filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }

        // 1) Schnellster Weg: letzter bekannter Standort, wenn er frisch genug ist
        val last = providers
            .mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        if (last != null && System.currentTimeMillis() - last.time < FRESH_ENOUGH_MS) return last

        // 2) Neuen Standort anfordern (mit Zeitlimit)
        val provider = providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER } ?: return last
        val fresh = withTimeoutOrNull(LOCATION_TIMEOUT_MS) { requestSingle(context, lm, provider) }
        return fresh ?: last
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingle(
        context: Context,
        lm: LocationManager,
        provider: String,
    ): Location? = suspendCancellableCoroutine { cont ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            lm.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                if (cont.isActive) cont.resume(location)
            }
        } else {
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    lm.removeUpdates(this)
                    if (cont.isActive) cont.resume(location)
                }

                override fun onProviderEnabled(provider: String) = Unit
                override fun onProviderDisabled(provider: String) = Unit

                @Deprecated("Nur für ältere Android-Versionen nötig")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
            }
            cont.invokeOnCancellation { lm.removeUpdates(listener) }
            @Suppress("DEPRECATION")
            lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }
    }

    /** Ermittelt den Ortsnamen (z. B. „Zwettl“). Gibt null zurück, wenn es nicht klappt. */
    suspend fun placeName(context: Context, latitude: Double, longitude: Double): String? {
        if (!Geocoder.isPresent()) return null
        return withTimeoutOrNull(GEOCODER_TIMEOUT_MS) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<String?> { cont ->
                            geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (cont.isActive) cont.resume(addresses.firstOrNull()?.shortName())
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (cont.isActive) cont.resume(null)
                                    }
                                },
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()?.shortName()
                    }
                }.getOrNull()
            }
        }
    }

    private fun Address.shortName(): String? = locality ?: subLocality ?: subAdminArea ?: adminArea
}
