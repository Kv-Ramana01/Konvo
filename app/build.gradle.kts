
plugins {
    id("com.android.application")
    //alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("kotlin-kapt")
}

android {
    namespace   = "com.example.konvo"
    compileSdk  = 35

    defaultConfig {
        applicationId = "com.example.konvo"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0"
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
    kotlinOptions { jvmTarget = "11" }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {

    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.3")


    implementation(libs.accompanist.navigation.animation)

    implementation("com.airbnb.android:lottie-compose:6.2.0")

    implementation(platform(libs.androidx.compose.bom.v20240500))
    androidTestImplementation(platform(libs.androidx.compose.bom.v20240500))

    /* ---------- AndroidX / Compose ---------- */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    implementation(platform(libs.androidx.compose.bom.v20240500))
    implementation(libs.androidx.core.splashscreen)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    /* ---------- Hilt ---------- */
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")


    // Hilt for ViewModel
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    kapt("androidx.hilt:hilt-compiler:1.2.0")

    /* ---------- Firebase ---------- */

    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")


    implementation(libs.coil.compose)


    implementation("com.squareup:javapoet:1.13.0")
    kapt        ("com.squareup:javapoet:1.13.0")

    implementation("androidx.credentials:credentials:1.6.0-alpha03")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0-alpha03")


    implementation("com.google.android.gms:play-services-auth:21.1.0")



    debugImplementation("com.google.firebase:firebase-appcheck-debug")


    debugCompileOnly("com.google.firebase:firebase-appcheck-playintegrity")


    releaseImplementation("com.google.firebase:firebase-appcheck-playintegrity")

    implementation("androidx.compose.material:material-icons-extended:1.6.1")
    implementation(libs.androidx.foundation)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "com.squareup" && requested.name == "javapoet") {
            useVersion("1.13.0")
        }
    }
}
