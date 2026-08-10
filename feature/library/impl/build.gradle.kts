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
        namespace("rhythhaus.feature.library.generated.resources")
    }

kotlin {
    android {
        namespace = "com.eterocell.rhythhaus.library.impl"
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
            api(projects.feature.library.api)
            api(projects.core.model)
            api(projects.core.ui)
            api(projects.core.database)
            api(projects.core.platform)
            implementation(projects.taglib)
            api(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.material3)
            implementation(libs.miuix.ui)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.koin.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.documentfile)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesCore)
        }
        jvmTest.dependencies {
            implementation(
                "org.jetbrains.compose.ui:ui-test:${libs.versions.compose.multiplatform.get()}")
            implementation(compose.desktop.currentOs)
            implementation(libs.sqldelight.sqlite.driver)
        }
        named("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}
