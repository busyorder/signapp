plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias (libs.plugins.google.services)
}

android {
    namespace = "com.busyorder.signapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.busyorder.signapp"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.filament.android)
    implementation(libs.androidx.compiler)
    // ✅ CameraX (latest stable)
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("com.google.mlkit:object-detection-custom:17.0.0")

    implementation("androidx.cardview:cardview:1.0.0")

    // Firebase
    implementation (platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation ("com.google.firebase:firebase-auth-ktx")
    implementation ("com.google.firebase:firebase-storage-ktx")


    // ML Kit
    implementation ("com.google.mlkit:vision-common:17.3.0")

    // TensorFlow Lite
    implementation ("org.tensorflow:tensorflow-lite:2.14.0")

    // ✅ OkHttp (HTTP client for backend)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ Kotlin coroutines (for background tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // ✅ AndroidX + Material
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // ✅ Lifecycle support
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")

    // ✅ Optional: Logging & testing
    implementation("androidx.core:core-ktx:1.13.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    // Socket.IO Client
    implementation("io.socket:socket.io-client:2.1.0") {
        exclude(group = "org.json", module = "json")
    }

    implementation("com.google.mlkit:pose-detection:17.0.0")
    implementation("com.google.mlkit:pose-detection-accurate:17.0.0")

    // MediaPipe Tasks (latest stable)
    implementation("com.google.mediapipe:tasks-vision:0.10.14")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}