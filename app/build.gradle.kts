/*
 * SPDX-FileCopyrightText: 2024-2026 The Calyx Institute
 * SPDX-License-Identifier: Apache-2.0
 */

import com.android.build.api.dsl.ApplicationExtension

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

kotlin {
    jvmToolchain(21)
}

configure<ApplicationExtension> {
    compileSdk = 36
    namespace = "com.google.android.apps.photos"

    defaultConfig {
        applicationId = "com.google.android.apps.photos"
        minSdk = 35
        targetSdk = 36
        versionCode = 1000000100
        versionName = "1.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    // fuck yeah!
}
