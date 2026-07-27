package com.eterocell.gradle.architecture

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

class KmpConventionPluginsFunctionalTest {
    @Test
    fun coreConventionEnablesExplicitApi() {
        assertExplicitApi(
            pluginId = "build-logic.kmp.core",
            expectedExplicitApi = "-Xexplicit-api=strict",
        )
    }

    @Test
    fun featureApiConventionEnablesExplicitApi() {
        assertExplicitApi(
            pluginId = "build-logic.kmp.feature.api",
            expectedExplicitApi = "-Xexplicit-api=strict",
        )
    }

    @Test
    fun featureImplementationConventionDoesNotForceExplicitApi() {
        assertExplicitApi(
            pluginId = "build-logic.kmp.feature.impl",
            expectedExplicitApi = "null",
        )
    }

    private fun assertExplicitApi(
        pluginId: String,
        expectedExplicitApi: String,
    ) {
        val result = runner(fixture(pluginId)).build()

        assertTrue(result.output.contains("EXPLICIT_API=$expectedExplicitApi"))
    }

    private fun fixture(pluginId: String): File {
        val projectDir = Files.createTempDirectory("kmp-convention-plugin").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            "rootProject.name = \"kmp-convention-consumer\"\n",
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
            import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

            plugins {
                id("$pluginId")
            }

            val kotlin = extensions.getByType<KotlinMultiplatformExtension>()
            kotlin.jvm()

            tasks.register("verifyExplicitApi") {
                doLast {
                    val explicitApi = tasks
                        .withType<KotlinCompilationTask<*>>()
                        .flatMap { it.compilerOptions.freeCompilerArgs.get() }
                        .singleOrNull { it.startsWith("-Xexplicit-api=") }
                    println("EXPLICIT_API=${'$'}explicitApi")
                }
            }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun runner(projectDir: File): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", "verifyExplicitApi", "--stacktrace")
}
