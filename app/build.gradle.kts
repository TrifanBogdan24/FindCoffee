plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")   // Pentru baza de date
    // Plugin pentru ascunderea cheilor API (citeste din local.properties)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")

    // Pentru Flutter:
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.findcoffee"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.findcoffee"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Pentru baza de date:
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    kapt("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-ktx:$room_version")

    // Animatie LOTTIE (atunci cand search-ul nu intoarce niciun rezultat):
    implementation("com.airbnb.android:lottie-compose:6.1.0")

    // Pentru randarea imaginilor:
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Pentru scanarea codurilor QR:
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Pentru a cauta cafenele in Google Maps:
    implementation("com.google.android.libraries.places:places:4.1.0")
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Pentru Flutter:
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")       // Autentificare
    implementation("com.google.firebase:firebase-firestore-ktx")  // Baza de date


    // Teste Unitare
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.10.3")
    testImplementation("androidx.test:core:1.5.0")

    // Teste Instrumentate (folderul 'androidTest')
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Test endpoint API HTTP:
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

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
}

// Configurarea plugin-ului pentru a citi din local.properties
secrets {
    propertiesFileName = "local.properties"
}
