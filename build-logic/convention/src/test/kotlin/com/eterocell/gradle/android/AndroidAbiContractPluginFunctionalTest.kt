package com.eterocell.gradle.android

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidAbiContractPluginFunctionalTest {
    @Test
    fun approvedPropertiesAreExposedToConsumerBuildScripts() {
        val projectDir = fixture(
            """
            rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64
            """.trimIndent(),
        )

        val result = runner(projectDir).build()

        assertEquals(TaskOutcome.SUCCESS, result.task(":printAbiContract")?.outcome)
        assertTrue(result.output.contains("ABI_CONTRACT_ABIS=[arm64-v8a, armeabi-v7a, x86_64]"))
        assertTrue(result.output.contains("ABI_CONTRACT_SPLIT=false"))
    }

    @Test
    fun missingAbiPropertyFailsConsumerConfiguration() {
        val result = runner(fixture()).buildAndFail()

        assertTrue(
            result.output.contains(
                "Missing required Gradle property 'rhythhaus.android.abis'; expected 'arm64-v8a,armeabi-v7a,x86_64'.",
            ),
        )
    }

    @Test
    fun duplicateAbiPropertyFailsConsumerConfiguration() {
        val projectDir = fixture(
            "rhythhaus.android.abis=arm64-v8a,arm64-v8a,x86_64",
        )

        val result = runner(projectDir).buildAndFail()

        assertTrue(result.output.contains("contains duplicate ABI(s): arm64-v8a"))
    }

    @Test
    fun reorderedAbiPropertyFailsConsumerConfiguration() {
        val projectDir = fixture(
            "rhythhaus.android.abis=x86_64,armeabi-v7a,arm64-v8a",
        )

        val result = runner(projectDir).buildAndFail()

        assertTrue(
            result.output.contains(
                "must resolve to exactly [arm64-v8a, armeabi-v7a, x86_64] in this order",
            ),
        )
    }

    @Test
    fun uppercaseTrueDoesNotEnableSplitApkMode() {
        val projectDir = fixture(
            """
            rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64
            rhythhaus.android.splitApk=TRUE
            """.trimIndent(),
        )

        val result = runner(projectDir).build()

        assertTrue(result.output.contains("ABI_CONTRACT_SPLIT=false"))
    }

    @Test
    fun exactLowercaseTrueEnablesSplitApkModeAndReusesConfigurationCache() {
        val projectDir = fixture(
            """
            rhythhaus.android.abis=arm64-v8a,armeabi-v7a,x86_64
            rhythhaus.android.splitApk=true
            """.trimIndent(),
        )
        val runner = runner(projectDir, "--configuration-cache")

        val first = runner.build()
        val second = runner.build()

        assertTrue(first.output.contains("ABI_CONTRACT_SPLIT=true"))
        assertTrue(second.output.contains("Reusing configuration cache."))
    }

    private fun fixture(gradleProperties: String = ""): File {
        val projectDir = Files.createTempDirectory("android-abi-contract").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            "rootProject.name = \"android-abi-contract-consumer\"\n",
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import com.eterocell.gradle.android.RhythHausAndroidAbiContractExtension

            plugins {
                id("build-logic.android.abi-contract")
            }

            val abiContract = extensions.getByType<RhythHausAndroidAbiContractExtension>()

            tasks.register("printAbiContract") {
                inputs.property("abis", abiContract.abis.get())
                inputs.property("splitApkEnabled", abiContract.splitApkEnabled.get())
                doLast {
                    val abis = inputs.properties.getValue("abis")
                    val splitApkEnabled = inputs.properties.getValue("splitApkEnabled")
                    println("ABI_CONTRACT_ABIS=${'$'}abis")
                    println("ABI_CONTRACT_SPLIT=${'$'}splitApkEnabled")
                }
            }
            """.trimIndent(),
        )
        projectDir.resolve("gradle.properties").writeText(gradleProperties)
        return projectDir
    }

    private fun runner(projectDir: File, vararg extraArguments: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("printAbiContract", *extraArguments, "--stacktrace")
}
