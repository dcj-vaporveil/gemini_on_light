plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.thelightphone.gemini"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.thelightphone.gemini"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Light Phone SDK Client
    implementation(project(":sdk:client"))

    // AndroidX Jetpack DataStore Preferences for secure on-device storage
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // OkHttp for direct Gemini HTTPS REST endpoint calls
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Jetpack Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
}
