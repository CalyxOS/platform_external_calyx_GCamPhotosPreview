plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

kotlin {
    jvmToolchain(21)
}

android {
    compileSdk = 35
    namespace = "com.google.android.apps.photos"

    defaultConfig {
        applicationId = "com.google.android.apps.photos"
        minSdk = 30
        targetSdk = 32
        versionCode = 1000000004
        versionName = "0.4"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
            )
        }

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.viewpager2)

    implementation(libs.davemorrissey.subsampling.scale.image.view)
}
