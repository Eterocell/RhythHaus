import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.api.tasks.Exec

val nativeAudioResourceRoot = layout.buildDirectory.dir("generated/nativeAudioResources/jvmMain")
val macosAudioResourceArch = when (System.getProperty("os.arch").lowercase()) {
    "aarch64", "arm64" -> "macos-aarch64"
    else -> "macos-x64"
}
val macosAudioHelperOutputFile = layout.buildDirectory.file("generated/nativeAudioResources/jvmMain/native/$macosAudioResourceArch/librhythhaus_audio.dylib").get().asFile
val macosAudioHelperSourceFile = layout.projectDirectory.file("src/nativeInterop/macos/rhythhaus_audio.mm").asFile
val javaHomePath = providers.systemProperty("java.home").get()
val buildMacosAudioHelper by tasks.registering(Exec::class) {
    inputs.file(macosAudioHelperSourceFile)
    outputs.file(macosAudioHelperOutputFile)
    macosAudioHelperOutputFile.parentFile.mkdirs()
    executable = "clang++"
    args(
        "-dynamiclib",
        "-std=c++17",
        "-fobjc-arc",
        "-framework", "Foundation",
        "-framework", "AVFoundation",
        "-I$javaHomePath/include",
        "-I$javaHomePath/include/darwin",
        macosAudioHelperSourceFile.absolutePath,
        "-o", macosAudioHelperOutputFile.absolutePath,
    )
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        create("RhythHausDatabase") {
            packageName.set("com.eterocell.rhythhaus.library")
            dialect("app.cash.sqldelight:sqlite-3-38-dialect:${libs.versions.sqldelight.get()}")
        }
    }
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
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
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.sqldelight.android.driver)
        }
        jvmMain {
            resources.srcDir(nativeAudioResourceRoot)
            dependencies {
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        commonMain.dependencies {
            implementation(projects.taglib)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks.matching { it.name in setOf("jvmProcessResources", "processJvmMainResources") }.configureEach {
    dependsOn(buildMacosAudioHelper)
}