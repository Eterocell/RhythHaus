import com.eterocell.gradle.architecture.ControlledComposeResourcesExtension
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test
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
        val escapedExpectedVersionName =
            expectedVersionName
                .get()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("$", "\\$")
        val expectedDeclaration =
            "const val versionName: String = \"$escapedExpectedVersionName\""
        check(expectedDeclaration in generatedFile.get().asFile.readText()) {
            "Generated RhythHausBuildInfo does not contain the expected version declaration: $expectedDeclaration"
        }
    }
}

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

plugins {
    id("build-logic.kmp.feature.impl")
    id("build-logic.android.kmp.library")
    id("build-logic.compose-resources")
    alias(libs.plugins.compose.compiler)
}

extensions.configure<ControlledComposeResourcesExtension>(
    "architectureComposeResources") {
        namespace("rhythhaus.feature.settings.generated.resources")
    }

kotlin {
    android {
        namespace = "com.eterocell.rhythhaus.settings"
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
        commonMain {
            kotlin.srcDir(generatedBuildInfoRoot)
        }
        commonMain.dependencies {
            api(projects.core.ui)
            api(libs.compose.runtime)
            api(libs.compose.ui)
            implementation(libs.compose.foundation)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.miuix.ui)
            implementation(libs.miuix.preference)
            implementation(libs.aboutlibraries.compose.m3)
            implementation(libs.compose.material3)
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

tasks.withType<KotlinCompilationTask<*>>().configureEach {
    dependsOn(generateRhythHausBuildInfo)
}

tasks.configureEach {
    if (name.startsWith("ksp")) {
        dependsOn(generateRhythHausBuildInfo)
    }
}

tasks.register<VerifyRhythHausVersionOverrideTask>(
    "verifyRhythHausVersionOverride") {
        dependsOn("compileKotlinJvm")
        expectedVersionName.set(rhythHausVersionName)
        generatedFile.set(generateRhythHausBuildInfo.flatMap { it.outputFile })
    }

tasks.withType<Test>().configureEach {
    systemProperty("rhythhaus.rootDir", rootProject.projectDir.absolutePath)
}
