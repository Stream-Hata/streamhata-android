import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

android {
    namespace = "dev.rdpctd.streamhata.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.rdpctd.streamhata.android"
        minSdk = 23
        targetSdk = 35
        versionCode = 705
        versionName = "0.7.5-alpha"
    }

    signingConfigs {
        create("release") {
            val storeFileValue = localProperties.getProperty("STREAMHATA_STORE_FILE")
            if (!storeFileValue.isNullOrBlank()) {
                storeFile = file(storeFileValue)
                storePassword = localProperties.getProperty("STREAMHATA_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("STREAMHATA_KEY_ALIAS")
                keyPassword = localProperties.getProperty("STREAMHATA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
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
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
