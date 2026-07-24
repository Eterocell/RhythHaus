import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

@CacheableTask
abstract class GenerateRhythHausBuildInfoTask : DefaultTask() {
    @get:Input abstract val versionName: Property<String>

    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val escapedVersionName =
            versionName
                .get()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
        val generatedFile = outputFile.get().asFile
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(
            """
            package com.eterocell.rhythhaus.settings

            internal object RhythHausBuildInfo {
                const val versionName: String = "$escapedVersionName"
            }
            """
                .trimIndent() + "\n",
        )
    }
}

abstract class VerifyRhythHausVersionOverrideTask : DefaultTask() {
    @get:Input abstract val expectedVersionName: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedFile: RegularFileProperty

    @TaskAction
    fun verify() {
        val expectedDeclaration =
            "const val versionName: String = \"${expectedVersionName.get()}\""
        check(expectedDeclaration in generatedFile.get().asFile.readText()) {
            "Generated RhythHausBuildInfo does not contain the expected version declaration: $expectedDeclaration"
        }
    }
}

val nativeAudioResourceRoot =
    layout.buildDirectory.dir("generated/nativeAudioResources/jvmMain")
val macosAudioResourceArch =
    when (System.getProperty("os.arch").lowercase()) {
        "aarch64",
        "arm64" -> "macos-aarch64"
        else -> "macos-x64"
    }
val macosAudioHelperOutputFile =
    layout.buildDirectory
        .file(
            "generated/nativeAudioResources/jvmMain/native/$macosAudioResourceArch/librhythhaus_audio.dylib")
        .get()
        .asFile
val macosAudioHelperSourceFile =
    layout.projectDirectory
        .file("src/nativeInterop/macos/rhythhaus_audio.mm")
        .asFile
val javaHomePath = providers.systemProperty("java.home").get()
val rhythHausVersionName = providers.gradleProperty("rhythhaus.versionName")
val generatedBuildInfoRoot =
    layout.buildDirectory.dir("generated/rhythHausBuildInfo/commonMain/kotlin")
val generateRhythHausBuildInfo =
    tasks.register<GenerateRhythHausBuildInfoTask>(
        "generateRhythHausBuildInfo") {
            versionName.set(rhythHausVersionName)
            outputFile.set(
                generatedBuildInfoRoot.map {
                    it.file(
                        "com/eterocell/rhythhaus/settings/RhythHausBuildInfo.kt")
                },
            )
        }
val buildMacosAudioHelper by
    tasks.registering(Exec::class) {
        inputs.file(macosAudioHelperSourceFile)
        outputs.file(macosAudioHelperOutputFile)
        macosAudioHelperOutputFile.parentFile.mkdirs()
        executable = "clang++"
        args(
            "-dynamiclib",
            "-std=c++17",
            "-fobjc-arc",
            "-framework",
            "Foundation",
            "-framework",
            "AVFoundation",
            "-framework",
            "MediaPlayer",
            "-framework",
            "AppKit",
            "-I$javaHomePath/include",
            "-I$javaHomePath/include/darwin",
            macosAudioHelperSourceFile.absolutePath,
            "-o",
            macosAudioHelperOutputFile.absolutePath,
        )
    }

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.multiplatform.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.sqldelight)
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

sqldelight {
    databases {
        create("RhythHausDatabase") {
            packageName.set("com.eterocell.rhythhaus.library")
            dialect(
                "app.cash.sqldelight:sqlite-3-38-dialect:${libs.versions.sqldelight.get()}")
            schemaOutputDirectory.set(
                file("src/commonMain/sqldelight/databases"))
        }
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
        commonMain {
            kotlin.srcDir(generatedBuildInfoRoot)
        }
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.session)
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
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.coil.compose)
            implementation(libs.coil.core)
            implementation(libs.miuix.ui)
            implementation(libs.miuix.blur)
            implementation(libs.miuix.preference)
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
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.11.1")
            implementation(compose.desktop.currentOs)
        }
        named("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
    }
}

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateRhythHausBuildInfo)
}

tasks.register<VerifyRhythHausVersionOverrideTask>(
    "verifyRhythHausVersionOverride") {
        dependsOn("compileKotlinJvm")
        expectedVersionName.set(rhythHausVersionName)
        generatedFile.set(generateRhythHausBuildInfo.flatMap { it.outputFile })
    }

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}

tasks
    .matching {
        it.name in setOf("jvmProcessResources", "processJvmMainResources")
    }
    .configureEach {
        dependsOn(buildMacosAudioHelper)
    }
