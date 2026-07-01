import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.build.logic.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionName'") })
val rhythHausVersionCode = providers.gradleProperty("rhythhaus.versionCode")
    .orElse(provider { throw GradleException("Missing required Gradle property 'rhythhaus.versionCode'") })

android {
    namespace = "com.eterocell.rhythhaus"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.eterocell.rhythhaus"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = rhythHausVersionCode.get().toIntOrNull()
            ?: throw GradleException("Gradle property 'rhythhaus.versionCode' must be an integer, was '${rhythHausVersionCode.get()}'")
        versionName = rhythHausVersionName.get()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
