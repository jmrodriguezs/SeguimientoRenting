import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.manursan.seguimientokm"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.manursan.seguimientokm"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        minSdk = 26
        targetSdk = 36
        versionCode = 5
        versionName = "1.2.2"
        // Solo arquitecturas de móviles y tablets reales: sin x86/x86_64 (emuladores) la APK pesa la mitad
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a") }
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties()
    val hasReleaseKeystore = keystorePropsFile.exists()
    if (hasReleaseKeystore) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                val storeFilePath = keystoreProps.getProperty("storeFile") ?: "release-upload.jks"
                val storeFileCandidate = file(storeFilePath)
                storeFile = if (storeFileCandidate.exists()) storeFileCandidate else rootProject.file(storeFilePath)
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2025.08.01")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.exifinterface:exifinterface:1.4.1")
    // Lectura de tiques de repostaje: OCR de ML Kit en el dispositivo, sin enviar la imagen a ningún servidor.
    // Modelo empaquetado en la APK (funciona sin Google Play y sin conexión); ~11 MB por arquitectura, ver abiFilters.
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250107")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
}
