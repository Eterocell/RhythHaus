import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("build-logic.kmp.core")
    id("build-logic.android.kmp.library")
    id("build-logic.compose-resources")
    alias(libs.plugins.compose.compiler)
}

extensions.configure<ControlledComposeResourcesExtension>(
    "architectureComposeResources") {
        namespace("rhythhaus.core.ui.generated.resources")
    }

compose.resources {
    publicResClass = true
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "com.eterocell.rhythhaus.ui"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.compose.animation)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.coil.compose)
            implementation(libs.coil.core)
            api(libs.compose.foundation)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            api(libs.compose.components.resources)
            implementation(libs.miuix.ui)
            implementation(libs.miuix.blur)
            api(libs.kotlinx.coroutinesCore)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation(
                "org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
            implementation(compose.desktop.currentOs)
        }
    }
}
