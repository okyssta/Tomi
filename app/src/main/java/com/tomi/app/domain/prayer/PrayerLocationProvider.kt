package com.tomi.app.domain.prayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.abs

class PrayerLocationProvider(private val context: Context) {
    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? = withContext(Dispatchers.Main) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return@withContext null
        }
        val manager = context.getSystemService(LocationManager::class.java) ?: return@withContext null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        val lastKnown = providers
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        if (providers.isEmpty()) return@withContext lastKnown
        if (lastKnown != null && abs(System.currentTimeMillis() - lastKnown.time) < 30 * 60_000L) {
            return@withContext lastKnown
        }

        val fresh = withTimeoutOrNull(15_000L) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    @Deprecated("Deprecated in API 29")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                val provider = providers.firstOrNull() ?: return@suspendCancellableCoroutine
                runCatching {
                    @Suppress("DEPRECATION")
                    manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                }.onFailure {
                    if (continuation.isActive) continuation.resume(lastKnown)
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        }
        fresh ?: lastKnown
    }
}
