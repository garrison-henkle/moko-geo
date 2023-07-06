/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.geo

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
import platform.CoreLocation.CLLocationAccuracy
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.CoreLocation.kCLLocationAccuracyReduced

actual class LocationTracker(
    actual val permissionsController: PermissionsController,
    actual val accuracy: LocationTrackerAccuracy,
) {
    private val locationsChannel = Channel<LatLng>(Channel.BUFFERED)
    private val extendedLocationsChannel = Channel<ExtendedLocation>(Channel.BUFFERED)
    private val trackerScope = CoroutineScope(Dispatchers.Main)
    private val tracker = Tracker(
        locationsChannel = locationsChannel,
        extendedLocationsChannel = extendedLocationsChannel,
        scope = trackerScope
    )
    private val locationManager = CLLocationManager().apply {
        delegate = tracker
        desiredAccuracy = accuracy.toIosAccuracy()
    }

    actual suspend fun startTracking(
        requestPrecise: Boolean,
        requirePrecise: Boolean,
    ) {
        val permission = if(requestPrecise) Permission.LOCATION else Permission.COARSE_LOCATION
        permissionsController.providePermission(permission, allowPartialAndroidGrants = true)
        // if permissions request failed - execution stops here

        locationManager.startUpdatingLocation()
    }

    actual fun stopTracking() {
        locationManager.stopUpdatingLocation()
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

    private fun LocationTrackerAccuracy.toIosAccuracy(): CLLocationAccuracy {
        return when (this) {
            LocationTrackerAccuracy.Best -> kCLLocationAccuracyBest
            LocationTrackerAccuracy.Medium -> kCLLocationAccuracyKilometer
            LocationTrackerAccuracy.LowPower -> kCLLocationAccuracyReduced
        }
    }
}
