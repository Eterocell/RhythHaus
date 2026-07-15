package com.eterocell.gradle.android

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner

class AndroidReleaseChannelFunctionalTest {
    @Test
    fun abbreviatedPureAabRequestDisablesSplitConfiguration() {
        val result = runner("bR").build()

        assertTrue(result.output.contains("RELEASE_SPLITS=false"))
    }

    @Test
    fun fullExactSplitApkRequestKeepsSplitConfiguration() {
        val result = runner("verifyReleaseApks").build()

        assertTrue(result.output.contains("RELEASE_SPLITS=true"))
    }

    @Test
    fun abbreviatedExactSplitApkRequestKeepsSplitConfiguration() {
        val result = runner("vRelApks").build()

        assertTrue(result.output.contains("RELEASE_SPLITS=true"))
    }

    @Test
    fun abbreviatedMixedApkAndAabRequestsFailActionably() {
        val result = runner("vRelApks", "vRelAab").buildAndFail()

        assertTrue(result.output.contains("separate Gradle invocations"))
    }

    private fun runner(vararg taskNames: String): GradleRunner {
        val projectDir = fixture()
        return GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*taskNames, "--stacktrace")
    }

    private fun fixture(): File {
        val projectDir = Files.createTempDirectory("android-release-channel").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            "rootProject.name = \"android-release-channel-consumer\"\n",
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import com.eterocell.gradle.android.shouldConfigureSplitApks

            plugins {
                id("build-logic.android.abi-contract")
            }

            val splitEnabled = shouldConfigureSplitApks(
                splitRequested = true,
                requestedTasks = gradle.startParameter.taskNames,
            )
            println("RELEASE_SPLITS=${'$'}splitEnabled")

            tasks.register("bundleRelease")
            tasks.register("verifyReleaseAab")
            tasks.register("assembleRelease")
            tasks.register("verifyReleaseApks")
            """.trimIndent(),
        )
        projectDir.resolve("gradle.properties").writeText(
            "rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64\n",
        )
        return projectDir
    }
}
