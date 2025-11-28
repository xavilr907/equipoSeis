plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    // Room (kapt)
    id("kotlin-kapt")

    // Firebase (Google Services plugin)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.univalle.inventarioapp"

    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.univalle.inventarioapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

/** ⬇️ KAPT fuera del bloque android */
kapt {
    correctErrorTypes = true
}

dependencies {

    // ================================
    // ANDROIDX / UI
    // ================================
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Biometría (para tu Sprint 1 — luego se puede borrar)
    implementation("androidx.biometric:biometric:1.1.0")

    // Lottie (si usas animaciones)
    implementation("com.airbnb.android:lottie:6.4.0")

    // ================================
    // TESTING
    // ================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ================================
    // ROOM (Sprint 1) — luego lo eliminaremos
    // ================================
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // ================================
    // LIFECYCLE
    // ================================
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ================================
    // NAVIGATION
    // ================================
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // ================================
    // FIREBASE (BOM + Auth + Firestore)
    // ================================
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-auth-ktx")       // ✔ NECESARIO PARA InventoryWidget
    implementation("com.google.firebase:firebase-firestore-ktx")  // ✔ Firestore

    // (Opcional) Analytics
    // implementation("com.google.firebase:firebase-analytics-ktx")
}
