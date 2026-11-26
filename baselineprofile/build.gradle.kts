import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.androidx.baselineprofile)
}

kotlin { compilerOptions { jvmTarget = JvmTarget.JVM_11 } }

android {
    namespace = "com.eterocell.rhythhaus.baselineprofile"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    targetProjectPath = ":androidApp"

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

baselineProfile { useConnectedDevices = true }

dependencies {
    implementation(libs.androidx.benchmark.macro.junit)
    implementation(libs.androidx.testExt.junit)
    implementation(libs.androidx.uiautomator)
}
