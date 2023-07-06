/*
 * Copyright 2023 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

package dev.icerock.moko.geo.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dev.icerock.moko.geo.LocationTracker
import dev.icerock.moko.geo.LocationTrackerAccuracy
import dev.icerock.moko.permissions.PermissionsController

@Composable
actual fun rememberLocationTrackerFactory(accuracy: LocationTrackerAccuracy): LocationTrackerFactory {
    val context: Context = LocalContext.current
    return remember(context) {
        object : LocationTrackerFactory {
            override fun createLocationTracker(): LocationTracker {
                return LocationTracker(
                    permissionsController = PermissionsController(
                        applicationContext = context.applicationContext
                    ),
                    accuracy = accuracy,
                )
            }

            override fun createLocationTracker(
                permissionsController: PermissionsController
            ): LocationTracker {
                return LocationTracker(
                    permissionsController = permissionsController,
                    accuracy = accuracy,
                )
            }
        }
    }
}
