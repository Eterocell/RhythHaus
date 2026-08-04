import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("build-logic.kmp.feature.impl")
    id("build-logic.android.kmp.library")
    id("build-logic.compose-resources")
    alias(libs.plugins.compose.compiler)
}

extensions.configure<ControlledComposeResourcesExtension>(
    "architectureComposeResources") {
        namespace("rhythhaus.feature.nowplaying.generated.resources")
    }

kotlin {
    android {
        namespace = "com.eterocell.rhythhaus.nowplaying"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        withHostTest {}
        androidResources { enable = true }
    }
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(projects.core.playback)
            api(projects.core.ui)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            api(libs.compose.foundation)
            api(libs.compose.components.resources)
            api(libs.compose.animation)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.miuix.ui)
            implementation(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies { implementation(libs.kotlin.test) }
        jvmTest.dependencies {
            implementation(
                "org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
            implementation(compose.desktop.currentOs)
        }
    }
}
