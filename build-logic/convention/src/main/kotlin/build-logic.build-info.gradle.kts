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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

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

extensions.getByType<KotlinMultiplatformExtension>().sourceSets {
    commonMain { kotlin.srcDir(generatedBuildInfoRoot) }
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
