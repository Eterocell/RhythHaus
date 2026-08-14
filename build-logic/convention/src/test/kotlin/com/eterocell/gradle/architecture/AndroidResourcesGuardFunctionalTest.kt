package com.eterocell.gradle.architecture

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

/**
 * Proves that a module shipping Compose resources but forgetting
 * `androidResources { enable = true }` fails the build, so Android packages the
 * generated `.cvr` assets. Regression for the Settings crash where
 * `stringResource(Res.string.back)` threw `MissingResourceException` on Android.
 */
class AndroidResourcesGuardFunctionalTest {

    @Test
    fun composeResourcesWithoutAndroidResourcesEnableFailsClosed() {
        val projectDir = Files.createTempDirectory("android-resources-guard").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "android-resources-guard-consumer"
            include(":feature:foo")
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText("")
        projectDir.resolve("feature/foo/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins {
                    id("build-logic.kmp.feature.impl")
                    id("build-logic.android.kmp.library")
                }

                kotlin {
                    android {
                        namespace = "com.example.foo"
                        compileSdk = 37
                        minSdk = 29
                        withHostTest {}
                        // Deliberately omits androidResources { enable = true }
                    }
                    jvm()
                    iosArm64()
                    iosSimulatorArm64()
                }
                """.trimIndent(),
            )
        }
        projectDir.resolve("feature/foo/src/commonMain/composeResources/values/strings.xml").apply {
            parentFile.mkdirs()
            writeText("""<resources><string name="back">Back</string></resources>""")
        }

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", "--stacktrace")
            .buildAndFail()

        assertTrue(
            result.output.contains("androidResources.enable is false"),
            result.output,
        )
        assertTrue(
            result.output.contains("src/commonMain/composeResources"),
            result.output,
        )
    }
}
