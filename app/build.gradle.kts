import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace   = "com.mktech.contactsapp"
    compileSdk  = 36

    defaultConfig {
        applicationId    = "com.mktech.contactsapp"
        minSdk           = 26
        targetSdk        = 34
        versionCode      = 3
        versionName      = "1.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
//    composeOptions {
//        kotlinCompilerExtensionVersion = "1.5.15"
//    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    // ✅ No longer needed with Kotlin 2.x + compose plugin — remove this block
    // composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // ── Core Android ─────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")


    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.firebase:firebase-config:23.0.1")       // firebase remote config
    implementation("com.google.android.gms:play-services-ads:23.0.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.13.0")
//    implementation("androidx.activity:activity:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1") //for ads
    // ── Compose BOM ───────────────────────────────────────────────────────────
    // Single BOM controls ALL compose library versions — never pin them individually
    val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // ── Navigation ────────────────────────────────────────────────────────────
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // ── Room ──────────────────────────────────────────────────────────────────
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // ── ViewModel ─────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // ── Image loading ─────────────────────────────────────────────────────────
    implementation("io.coil-kt:coil-compose:2.7.0")

    // ── Permissions ───────────────────────────────────────────────────────────
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")

    // ── Firebase BOM ──────────────────────────────────────────────────────────
    // ✅ BOM manages ALL Firebase versions — never add firebase-* versions manually
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-analytics-ktx")      // no version — BOM controls it
    implementation("com.google.firebase:firebase-crashlytics-ktx")    // no version — BOM controls it

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
