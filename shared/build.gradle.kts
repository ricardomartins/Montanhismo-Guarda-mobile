import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinxSerialization)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kmpNativeCoroutines)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        all {
            languageSettings.optIn("kotlinx.cinterop.ExperimentalForeignApi")
            languageSettings.optIn("kotlin.experimental.ExperimentalObjCName")
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
        }
        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.resources)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.kermit)
            implementation(libs.ksoup)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            api(libs.kmp.observable.viewmodel)
        }
    }
}

android {
    namespace = "pt.rikmartins.clubemg.mobile.shared"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = 24
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("pt.rikmartins.clubemg.mobile.cache")
        }
    }
}