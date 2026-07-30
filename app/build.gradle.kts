@file:Suppress("UnstableApiUsage")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.navigation.safeargs)
}

kotlin {
    jvmToolchain(17)
}

configure<com.android.build.api.dsl.ApplicationExtension>  {
    namespace = "com.minar.birday"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.minar.birday"
        targetSdk = 37
        minSdk = 26
        versionCode = 37
        versionName = "4.7.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    androidResources {
        generateLocaleConfig = true
    }



    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
        dex {
            useLegacyPackaging = false
        }
        resources {
            excludes += listOf("META-INF/*.version")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    lint {
        disable += listOf("MissingTranslation", "MissingQuantity")
    }
}

dependencies {

    // Default dependencies
    implementation(libs.appcompat)
    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)

    // Transition
    implementation(libs.transition.ktx)

    // Constraint / motion layout
    implementation(libs.constraintlayout)

    // Splashscreen
    implementation(libs.core.splashscreen)

    // Material Components
    implementation(libs.material)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Navigation component
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)

    // Lifecycle and ViewModel
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.recyclerview)

    // Room
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // Gson
    implementation(libs.gson)

    // App Intro
    implementation(libs.appintro)

    // Facebook shimmer effect
    implementation(libs.shimmer)

    // Confetti effect
    implementation(libs.konfetti)

    // TastiCalendar (my library :D)
    implementation(libs.tasticalendar)

    // Image cropping
    implementation(libs.imageCropper)

    // [Testing] Basic
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // [Testing] ICU
    testImplementation(libs.icu4j)
}
