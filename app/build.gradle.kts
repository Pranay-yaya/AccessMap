import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // for Room annotation processing
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.accessmap.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.accessmap.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"

        // Reads MAPS_API_KEY from local.properties -> exposed as a manifest placeholder
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localProperties.load(localPropertiesFile.inputStream())
        }
        manifestPlaceholders["MAPS_API_KEY"] = localProperties.getProperty("MAPS_API_KEY", "")
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
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // CameraX (1.4.0+ required for 16 KB page size support)
    implementation("androidx.camera:camera-core:1.4.2")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    // Google AI Edge LiteRT (official successor to TensorFlow Lite —
    // used instead because TFLite's native libraries are still not
    // 16 KB page size aligned as of this writing; LiteRT is Google's
    // recommended replacement with the same core Interpreter API)
    implementation("com.google.ai.edge.litert:litert:1.4.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
}