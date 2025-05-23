plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.swipeflix"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.swipeflix"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64", "x86")
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // RecyclerView for card stacks
    implementation(libs.androidx.recyclerview)

    // ZXing (QR Code Scanner)
    implementation(libs.zxing.android.embedded)

    // Firebase (only what you need)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.database.ktx)
    implementation(libs.ktor.client.engine.z)

    //supabase
//    implementation(libs.bom)
    implementation(libs.xpostgrest.kt.z)
    implementation ("io.github.jan-tennert.supabase:realtime-kt:2.0.4")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // or latest

    //Delay
    implementation ("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") // Use the latest version

    implementation(libs.glide)
    kapt(libs.glide.compiler)
}


