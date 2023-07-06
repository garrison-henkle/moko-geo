/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.geo

import android.annotation.SuppressLint
import android.content.Context
import android.location.Criteria
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import dev.icerock.moko.permissions.PartiallyDeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

actual class LocationTracker(
    actual val permissionsController: PermissionsController,
    private val intervalMs: Long = 1_000,
    private val distanceM: Float = 500f,
    actual val accuracy: LocationTrackerAccuracy = LocationTrackerAccuracy.Best,
) {
    private var locationManager: LocationManager? = null
    private var isStarted: Boolean = false
    private val locationsChannel = Channel<LatLng>(Channel.CONFLATED)
    private val extendedLocationsChannel = Channel<ExtendedLocation>(Channel.CONFLATED)
    private val trackerScope = CoroutineScope(Dispatchers.Main)

    private val locationListener = LocationListener { location ->
        onLocationResult(location)
    }

    fun bind(lifecycle: Lifecycle, context: Context, fragmentManager: FragmentManager) {
        permissionsController.bind(lifecycle, fragmentManager)

        locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        @SuppressLint("MissingPermission")
        if (isStarted) {
            locationManager?.requestLocationUpdates(accuracy.toProvider(), intervalMs, distanceM, locationListener)
        }

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                locationManager?.removeUpdates(locationListener)
            }
        })
    }

    private fun onLocationResult(location: android.location.Location) {
        val speedAccuracy = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) null
        else location.speedAccuracyMetersPerSecond.toDouble()

        val bearingAccuracy = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) null
        else location.bearingAccuracyDegrees.toDouble()

        val verticalAccuracy = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) null
        else location.verticalAccuracyMeters.toDouble()

        val latLng = LatLng(
            location.latitude,
            location.longitude
        )

        val locationPoint = Location(
            coordinates = latLng,
            coordinatesAccuracyMeters = location.accuracy.toDouble()
        )

        val speed = Speed(
            speedMps = location.speed.toDouble(),
            speedAccuracyMps = speedAccuracy
        )

        val azimuth = Azimuth(
            azimuthDegrees = location.bearing.toDouble(),
            azimuthAccuracyDegrees = bearingAccuracy
        )

        val altitude = Altitude(
            altitudeMeters = location.altitude,
            altitudeAccuracyMeters = verticalAccuracy
        )

        val extendedLocation = ExtendedLocation(
            location = locationPoint,
            azimuth = azimuth,
            speed = speed,
            altitude = altitude,
            timestampMs = location.time
        )

        trackerScope.launch {
            extendedLocationsChannel.send(extendedLocation)
            locationsChannel.send(latLng)
        }
    }

    @SuppressLint("MissingPermission")
    actual suspend fun startTracking(
        requestPrecise: Boolean,
        requirePrecise: Boolean,
    ) {
        val permission = if(requestPrecise) Permission.LOCATION else Permission.COARSE_LOCATION
        try{
            permissionsController.providePermission(permission, allowPartialAndroidGrants = true)
        } catch (ex: PartiallyDeniedException){
            if(requirePrecise || Permission.COARSE_LOCATION !in ex.granted){ throw ex }
        }
        // if permissions request failed - execution stops here

        isStarted = true
        locationManager?.requestLocationUpdates(accuracy.toProvider(), intervalMs, distanceM, locationListener)
    }

    actual fun stopTracking() {
        isStarted = false
        locationManager?.removeUpdates(locationListener)
    }

    actual fun getLocationsFlow(): Flow<LatLng> {
        return channelFlow {
            val sendChannel = channel
            val job = launch {
                while (isActive) {
                    val latLng = locationsChannel.receive()
                    sendChannel.send(latLng)
                }
            }

            awaitClose { job.cancel() }
        }
    }

    actual fun getExtendedLocationsFlow(): Flow<ExtendedLocation> {
        return channelFlow {
            val sendChannel = channel
            val job = launch {
                while (isActive) {
                    val extendedLocation = extendedLocationsChannel.receive()
                    sendChannel.send(extendedLocation)
                }
            }

            awaitClose { job.cancel() }
        }
    }

    private fun LocationTrackerAccuracy.toProvider(): String = (locationManager
        ?.getProviders(true)?.let{ providers ->
            val android12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            when(this){
                LocationTrackerAccuracy.Best -> when{
                    android12 && LocationManager.FUSED_PROVIDER in providers -> LocationManager.FUSED_PROVIDER
                    LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
                    LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
                    else -> LocationManager.PASSIVE_PROVIDER
                }
                LocationTrackerAccuracy.Medium -> {
                    if(LocationManager.NETWORK_PROVIDER in providers){
                        LocationManager.NETWORK_PROVIDER
                    } else LocationManager.PASSIVE_PROVIDER
                }
                LocationTrackerAccuracy.LowPower -> LocationManager.PASSIVE_PROVIDER
            }
        } ?: LocationManager.PASSIVE_PROVIDER).also{ android.util.Log.e("mokoEnhanced", "using location provider: $it") }
}
