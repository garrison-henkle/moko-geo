/*
 * Copyright 2023 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.geo.compose

import androidx.compose.runtime.Composable
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.LocationTrackerAccuracy
import dev.icerock.moko.permissions.PermissionsController

interface LocationTrackerFactory {
    fun createLocationTracker(): LocationTracker
    fun createLocationTracker(permissionsController: PermissionsController): LocationTracker
}

@Composable
expect fun rememberLocationTrackerFactory(accuracy: LocationTrackerAccuracy): LocationTrackerFactory
