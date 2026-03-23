plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.chloelearns"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.chloelearns"
        minSdk = 19
        targetSdk = 19
        versionCode = 1
        versionName = "1.0"

        // x86 for the Intel Atom, plus armeabi-v7a as fallback
        ndk {
            abiFilters += listOf("x86", "armeabi-v7a")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    // Keep deps minimal for API 19 compat — no Compose, no ViewBinding
    implementation("androidx.appcompat:appcompat:1.3.1")
    implementation("androidx.core:core-ktx:1.6.0")
}
