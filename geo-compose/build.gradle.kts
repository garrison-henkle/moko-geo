import org.jfrog.gradle.plugin.artifactory.Constant.ALL_PUBLICATIONS
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.properties.loadProperties

/*
 * Copyright 2023 IceRock MAG Inc. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dev.icerock.moko.gradle.multiplatform.mobile")
    id("dev.icerock.moko.gradle.publication")
    id("dev.icerock.moko.gradle.stub.javadoc")
    id("dev.icerock.moko.gradle.detekt")
    id("org.jetbrains.compose")
    `maven-publish`
    id("com.jfrog.artifactory")
}

val localProperties = loadProperties(rootProject.file("local.properties").absolutePath)

android {
    namespace = "dev.icerock.moko.geo.compose"

    defaultConfig {
        minSdk = 21
    }
}

dependencies {
    commonMainApi(libs.mokoGeo)
    commonMainApi(compose.runtime)

    androidMainImplementation(libs.appCompat)
    androidMainImplementation(compose.foundation)
}

kotlin {
    android {
        publishLibraryVariants("release")
    }

    ios()

    targets.withType<KotlinNativeTarget> {
        binaries.framework(listOf(RELEASE))
    }
}

artifactory {
    val artifactoryUrl = localProperties.getProperty("artifactory.url")
    val artifactoryRepository = localProperties.getProperty("artifactory.repo")
    val artifactoryUsername = localProperties.getProperty("artifactory.username")
    val artifactoryPassword = localProperties.getProperty("artifactory.password")

    setContextUrl(artifactoryUrl)
    publish {
        repository {
            repoKey = artifactoryRepository
            username = artifactoryUsername
            password = artifactoryPassword
        }

        defaults {
            publications(ALL_PUBLICATIONS)
        }
    }
}