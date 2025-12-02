plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

android {
    namespace = "com.example.attendance_android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.attendance_android"
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
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.compose.ui:ui-text:1.6.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.navigation:navigation-compose:2.7.0")
// or latest stable
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
//    implementation ("com.google.accompanist:accompanist-navigation-animation:<version>")
// only if using Hilt
// optionally for animation:
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") // or current
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation ("androidx.navigation:navigation-compose:2.6.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")
    // okhttp is optional (I used HttpURLConnection in the example). If you prefer okhttp:
    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    // Room components
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")
    implementation ("androidx.compose.material:material-icons-extended")

    //face recognition
    implementation("androidx.camera:camera-core:1.2.2")
    implementation("androidx.camera:camera-camera2:1.2.2")
    implementation("androidx.camera:camera-lifecycle:1.2.2")
    implementation("androidx.camera:camera-view:1.2.2")
    implementation("androidx.camera:camera-extensions:1.2.2")

    // ML Kit - on-device Face Detection
    implementation("com.google.mlkit:face-detection:16.1.5")
// TensorFlow Lite (runtime) + optional GPU + support lib for image ops
    implementation("org.tensorflow:tensorflow-lite:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
// helper utils (TensorImage etc.)
    implementation("org.tensorflow:tensorflow-lite-gpu:2.11.0")
// optional, only if you want GPU delegate

    // Jetpack lifecycle / activity / compose helpers (if using compose)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-ktx:1.7.2")
    implementation("androidx.activity:activity-compose:1.7.2")
// Kotlin Coroutines (use for async camera/ML/TFLite)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")
// Optional: Coil or accompanist if you want to show bitmaps in Compose
    implementation("io.coil-kt:coil-compose:2.2.2")
// for images in Compose
    implementation("com.google.accompanist:accompanist-permissions:0.30.1")
// permissions helper
// Optional: logging / debugging helpers
    implementation("com.jakewharton.timber:timber:5.0.1")
// (If you use Gson or Moshi for simple JSON, add them)
    implementation("com.google.code.gson:gson:2.10.1")
// optional

    // Use explicit add(...) to avoid unresolved kapt(...) helper in some Gradle Kotlin DSL environments
    add("kapt", "androidx.room:room-compiler:2.5.2")

}