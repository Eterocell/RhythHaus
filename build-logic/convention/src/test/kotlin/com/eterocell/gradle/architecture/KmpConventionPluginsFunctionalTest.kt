package com.eterocell.gradle.architecture

import java.io.File
import java.nio.file.Files
import java.util.jar.JarFile
import kotlin.test.assertFalse
import kotlin.test.assertEquals
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

    @Test
    fun featureImplementationConventionPublishesProductionKspMetadataForEveryMainTarget() {
        val projectDir = featureImplementationFixture()
        val result = runner(projectDir, "verifyFeatureImplementationConvention").build()

        assertTrue(result.output.contains("EXPLICIT_API=null"), result.output)
        assertTrue(
            result.output.contains("EXPECT_ACTUAL_CLASSES=true"),
            result.output,
        )
        assertTrue(result.output.contains("KSP_MODULE=:feature:nowplaying"), result.output)
        assertTrue(
            result.output.contains(
                "KSP_PACKAGE_ROOTS=com.eterocell.rhythhaus.nowplaying",
            ),
            result.output,
        )
        assertTrue(
            result.output.contains(
                "KSP_SOURCE_ROOTS=" + result.output.lineValue("EXPECTED_KSP_SOURCE_ROOTS"),
            ),
            result.output,
        )
        assertEquals(4, Regex("KSP_CONFIGURATION=ksp(?:Android|Jvm|IosArm64|IosSimulatorArm64)").findAll(result.output).count(), result.output)
        assertEquals(4, Regex("""KSP_REGISTRATION=:feature:nowplaying\|ksp(?:Android|Jvm|IosArm64|IosSimulatorArm64)\|:architecture-processor""").findAll(result.output).count(), result.output)
    }

    @Test
    fun featureImplementationConventionExternalProcessorRejectsInvalidProductionPackage() =
        assertFeatureProcessorFailure(
            source = "package outside.feature\n/** Invalid package. */\npublic class InvalidFeaturePackage",
            expectedDiagnostic = "ARCH-PACKAGE :feature:nowplaying:InvalidFeature.kt (outside.feature)",
        )

    @Test
    fun featureImplementationConventionExternalProcessorRejectsUndocumentedPublicDeclaration() =
        assertFeatureProcessorFailure(
            source = "package com.eterocell.rhythhaus.nowplaying\npublic class MissingFeatureKDoc",
            expectedDiagnostic = "ARCH-KDOC :feature:nowplaying:InvalidFeature.kt:2 (com.eterocell.rhythhaus.nowplaying.MissingFeatureKDoc)",
        )

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

    private fun featureImplementationFixture(): File {
        val projectDir = Files.createTempDirectory("feature-implementation-convention").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "feature-implementation-convention-consumer"
            include(":architecture-processor", ":feature:nowplaying")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("")
        projectDir.resolve("architecture-processor/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText("plugins { id(\"org.jetbrains.kotlin.jvm\") }")
        }
        projectDir.resolve("feature/nowplaying/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                import com.eterocell.gradle.architecture.ArchitectureModelRegistry
                import java.io.File
                import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
                import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask

                plugins {
                    id("build-logic.kmp.feature.impl")
                    id("build-logic.android.kmp.library")
                }

                kotlin {
                    android {
                        namespace = "com.eterocell.rhythhaus.nowplaying"
                        compileSdk = 37
                        minSdk = 29
                        withHostTest {}
                        androidResources { enable = true }
                    }
                    jvm()
                    iosArm64()
                    iosSimulatorArm64()
                }

                tasks.register("verifyFeatureImplementationConvention") {
                    doLast {
                        @Suppress("UNCHECKED_CAST")
                        val options = project.extensions.getByName("ksp")
                            .javaClass
                            .getMethod("getArguments")
                            .invoke(project.extensions.getByName("ksp")) as Map<String, String>
                        println("EXPLICIT_API=" + project.tasks.withType(KotlinCompilationTask::class.java)
                            .flatMap { it.compilerOptions.freeCompilerArgs.get() }
                            .singleOrNull { it.startsWith("-Xexplicit-api=") })
                        println("EXPECT_ACTUAL_CLASSES=" + project.tasks.withType(KotlinCompilationTask::class.java)
                            .map { "-Xexpect-actual-classes" in it.compilerOptions.freeCompilerArgs.get() }
                            .all { it })
                        println("KSP_MODULE=" + options.getValue("architecture.module"))
                        println("KSP_PACKAGE_ROOTS=" + options.getValue("architecture.packageRoots"))
                        println("KSP_SOURCE_ROOTS=" + options.getValue("architecture.sourceRoots"))
                        val expectedSourceRoots = project.extensions
                            .getByType(KotlinMultiplatformExtension::class.java)
                            .sourceSets
                            .filterNot { it.name.contains("test", ignoreCase = true) }
                            .flatMap { it.kotlin.srcDirs }
                            .map { it.absolutePath }
                            .sorted()
                            .joinToString(File.pathSeparator)
                        println("EXPECTED_KSP_SOURCE_ROOTS=" + expectedSourceRoots)
                        project.configurations.names.filter {
                            it in setOf("kspAndroid", "kspJvm", "kspIosArm64", "kspIosSimulatorArm64")
                        }
                            .sorted()
                            .forEach { println("KSP_CONFIGURATION=" + it) }
                        ArchitectureModelRegistry.forRoot(project).snapshot().kspRegistrations
                            .sorted()
                            .forEach { println("KSP_REGISTRATION=" + it.module + "|" + it.configuration + "|" + it.processor) }
                    }
                }
                """.trimIndent(),
            )
        }
        return projectDir
    }

    private fun assertFeatureProcessorFailure(source: String, expectedDiagnostic: String) {
        val processorJar = externallyProvidedProcessorJar()
        val projectDir = featureImplementationFixture()
        val featureProject = projectDir.resolve("feature/nowplaying")
        featureProject.resolve("src/commonMain/kotlin/InvalidFeature.kt").apply {
            parentFile.mkdirs()
            writeText(source)
        }
        featureProject.resolve("build.gradle.kts").appendText(
            """

            dependencies.add("kspJvm", files("${processorJar.invariantSeparatorsPath}"))
            """.trimIndent(),
        )

        val result = runner(projectDir, ":feature:nowplaying:compileKotlinJvm").buildAndFail()

        assertTrue(result.output.contains(expectedDiagnostic), result.output)
        assertTrue(result.output.contains(":feature:nowplaying:kspKotlinJvm"), result.output)
        assertFalse(result.output.contains(":feature:nowplaying:kspKotlinJvm SKIPPED"), result.output)
        assertFalse(result.output.contains(":feature:nowplaying:kspKotlinJvm NO-SOURCE"), result.output)
    }

    private fun externallyProvidedProcessorJar(): File {
        val path = checkNotNull(System.getProperty("rhythhaus.architectureProcessorJar")) {
            "Expected -Prhythhaus.architectureProcessorJar for feature processor fixture"
        }
        val processorJar = File(path).canonicalFile
        assertTrue(processorJar.isFile && processorJar.length() > 0, "Missing processor JAR: $processorJar")
        JarFile(processorJar).use { jar ->
            assertTrue(
                jar.getJarEntry("com/eterocell/rhythhaus/architecture/ArchitectureProcessorProvider.class") != null,
                "Processor provider missing from $processorJar",
            )
        }
        return processorJar
    }

    private fun String.lineValue(prefix: String): String =
        lineSequence()
            .single { it.startsWith("$prefix=") }
            .removePrefix("$prefix=")

    private fun runner(projectDir: File, task: String = "verifyExplicitApi"): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", task, "--stacktrace")
}
