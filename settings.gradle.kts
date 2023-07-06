/*
 * Copyright 2019 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

enableFeaturePreview("VERSION_CATALOGS")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

val localProperties = java.util.Properties().apply{
    File("${rootDir.absolutePath}/local.properties").inputStream().use {
        load(it)
    }
}

dependencyResolutionManagement {
    repositories {
        maven{
            setUrl(localProperties.getProperty("artifactory.maven.url"))
            credentials {
                username = localProperties.getProperty("artifactory.username")
                password = localProperties.getProperty("artifactory.password")
            }
            isAllowInsecureProtocol = true
        }
        mavenCentral()
        google()
    }
}

include(":geo")
include(":geo-compose")
include(":sample:android-app")
include(":sample:mpp-library")
