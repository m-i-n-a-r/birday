@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.minar.birday"
    compileSdk = 36
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.minar.birday"
        targetSdk = 36
        minSdk = 26
        versionCode = 34
        versionName = "4.6.0"
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

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    kotlin {
        jvmToolchain(17)
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

configurations.configureEach {
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk7")
    exclude("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
}

dependencies {

    // Default dependencies
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("androidx.fragment:fragment-ktx:1.8.9")

    // Transition
    implementation("androidx.transition:transition-ktx:1.6.0")

    // Constraint / motion layout
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Splashscreen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Material Components
    implementation("com.google.android.material:material:1.14.0-alpha04")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.4")

    // Navigation component
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.4")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.4")

    // Lifecycle and ViewModel
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // Room
    val roomVersion = "2.8.0"
    implementation("androidx.room:room-runtime:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Gson
    implementation("com.google.code.gson:gson:2.13.2")

    // App Intro
    implementation("com.github.AppIntro:AppIntro:6.3.1")

    // Facebook shimmer effect
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Confetti effect
    implementation("nl.dionsegijn:konfetti:1.3.2")

    // TastiCalendar (my library :D)
    implementation("com.github.m-i-n-a-r:tasticalendar:1.4.0")

    // [Testing] Basic
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // [Testing] ICU
    testImplementation("com.ibm.icu:icu4j:77.1")
}
