import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("build-logic.android.kmp.library")
    id("build-logic.compose-resources")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
}

extensions.configure<ControlledComposeResourcesExtension>(
    "architectureComposeResources") {
        namespace("rhythhaus.shared.generated.resources")
    }

aboutLibraries {
    collect {
        configPath.set(layout.projectDirectory.dir("config"))
    }
    export {
        outputFile.set(
            layout.projectDirectory.file(
                "src/commonMain/composeResources/files/aboutlibraries.json"))
        includeMetaData.set(false)
        prettyPrint.set(true)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    listOf(
            iosArm64(),
            iosSimulatorArm64(),
        )
        .forEach { iosTarget ->
            iosTarget.binaries.framework {
                baseName = "Shared"
                isStatic = true
                export(projects.core.playback)
            }
            iosTarget.binaries.all {
                linkerOpts("-lsqlite3")
            }
        }

    jvm()

    android {
        namespace = "com.eterocell.rhythhaus.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.documentfile)
        }
        commonMain.dependencies {
            api(projects.core.model)
            api(projects.core.ui)
            api(projects.core.database)
            api(projects.core.platform)
            api(projects.core.playback)
            api(projects.feature.library.api)
            api(projects.feature.playlists.api)
            implementation(projects.feature.playlists.impl)
            implementation(projects.feature.nowplaying)
            implementation(projects.feature.search)
            implementation(projects.feature.settings)
            implementation(projects.taglib)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.coil.compose)
            implementation(libs.coil.core)
            implementation(libs.miuix.ui)
            implementation(libs.miuix.blur)
            implementation(libs.kermit)
            implementation(libs.koin.compose)
            implementation(libs.koin.core)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.ui)
            implementation(libs.navigationevent.compose)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.androidx.datastore.core)
            implementation(libs.androidx.datastore.preferences.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
            implementation(compose.desktop.currentOs)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
