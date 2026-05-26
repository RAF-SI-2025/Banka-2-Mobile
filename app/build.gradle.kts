import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "rs.raf.banka2.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "rs.raf.banka2.mobile"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Default je Android emulator host alias (10.0.2.2 = host machine iz emulator-a).
        // Debug buildType nasledjuje ovo (lokalni dev preko emulator-a + docker compose).
        // Release buildType override-uje na K8s production URL — vidi buildTypes.release.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "true")
            // API_BASE_URL = "http://10.0.2.2:8080/" nasledjuje iz defaultConfig.
        }
        release {
            // R8 minify + shrink ISKLJUCENO — sa minify-em release APK je crashovao
            // pri login-u (najverovatnije Moshi codegen adapter look-up sa obfuscated
            // DTO klasama, ili Hilt @HiltViewModel reflection). Bez minify-a APK je
            // oko 25 MB umesto 3.4 MB, ali za fakultetski projekt + GitHub Release
            // distribuciju velicina nije bitna; pouzdanost je. Ako se ikad bude
            // vracalo na minify, treba dodati kompletne Moshi + Hilt + Retrofit +
            // Kotlin metadata pravila u proguard-rules.pro i lokalno testirati
            // assembleRelease + adb install + smoke test pre push-a.
            isMinifyEnabled = false
            isShrinkResources = false
            buildConfigField("Boolean", "ENABLE_HTTP_LOGGING", "false")
            // Production K8s deploy — fakultetski klaster sa Envoy Gateway HTTPRoute.
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"https://banka-2.radenkovic.rs/api/\""
            )
            // Potpisujemo release APK sa Android SDK debug.keystore — NIJE production-grade
            // (debug keystore je share-ovan medju developerima i auto-generisan), ali za
            // fakultetski projekt + GitHub Release distribuciju tim moze instalirati APK
            // direkt sa Releases stranice. Za pravi production switch: kreirati custom
            // keystore (keytool -genkey -keystore release.jks ...), dodati signingConfigs
            // blok i ucitati lozinku iz local.properties (gitignored) ili GitHub Secrets.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        // Java 21 je trenutno najjaca verzija koju AGP 9.2 + Gradle 9.4 + Android
        // desugaring (`desugar_jdk_libs` 2.1.6) potpuno podrzavaju za Android target.
        // Java 25 radi na BE-u, ali Android ART runtime sa minSdk 24 jos uvek nema
        // desugar pravila za Java 22+ feature-e (Pattern matching, virtual threads,
        // sealed types u JDK 25 form-i). Cim AGP/desugar to podrze, prebacujemo na 25.
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        disable += setOf(
            "GradleDependency",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
            "IconLauncherShape",
            "IconDipSize",
            "IconDuplicates",
            "IconLocation",
            "MonochromeLauncherIcon"
        )
        warningsAsErrors = true
        abortOnError = true
    }
}

// Kotlin compiler options — AGP 9.0+ deprecated `android { kotlinOptions { } }` blok,
// preporuka iz https://kotl.in/u1r8ln je top-level `kotlin { compilerOptions { } }`.
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
        )
    }
}

dependencies {
    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)

    // Compose BOM + UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi.core)
    ksp(libs.moshi.codegen)

    // DataStore + Encrypted prefs (token storage)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)

    // Core library desugaring (java.time on minSdk 24)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Test (unit)
    testImplementation(libs.junit)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    // Test (instrumented)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Compose debug tooling
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
