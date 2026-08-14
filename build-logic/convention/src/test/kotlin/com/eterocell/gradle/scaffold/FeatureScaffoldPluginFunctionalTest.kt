package com.eterocell.gradle.scaffold

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome

class FeatureScaffoldPluginFunctionalTest {

    @Test
    fun generateFeatureModuleCreatesOnlyRequestedRealStructure() {
        val projectDir = scaffoldFixture()
        val result = runner(
            projectDir,
            "generateFeatureModule",
            "--module-name", "player",
            "--package-name", "com.eterocell.rhythhaus.player",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFeatureModule")?.outcome)

        assertTrue(projectDir.resolve("feature/player/build.gradle.kts").isFile)
        assertTrue(
            projectDir.resolve("feature/player/src/commonMain/kotlin/com/eterocell/rhythhaus/player/Feature.kt").isFile,
        )
        assertTrue(projectDir.resolve("feature/player/README.md").isFile)

        val generated = projectDir.resolve("feature/player").walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(projectDir).path.replace(File.separatorChar, '/') }
            .toSortedSet()
        assertFalse(
            generated.any { "UiState" in it || "UiEvent" in it || "UiEffect" in it || "Presenter" in it || "ViewModel" in it },
            "scaffold must never generate empty state/effect/presenter scaffolding: $generated",
        )
        assertFalse(projectDir.resolve("feature/player-api").exists(), "no API module without an api-contract")
    }

    @Test
    fun apiModuleIsGeneratedOnlyWhenAContractNameIsSupplied() {
        val projectDir = scaffoldFixture()
        val result = runner(
            projectDir,
            "generateFeatureModule",
            "--module-name", "player",
            "--package-name", "com.eterocell.rhythhaus.player",
            "--api-contract", "PlayerContract",
        ).build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":generateFeatureModule")?.outcome)

        assertTrue(projectDir.resolve("feature/player-api/build.gradle.kts").isFile)
        assertTrue(
            projectDir.resolve("feature/player-api/src/commonMain/kotlin/com/eterocell/rhythhaus/player/PlayerContract.kt").isFile,
        )
    }

    @Test
    fun blankModuleNameFailsClosed() {
        val projectDir = scaffoldFixture()
        val result = runner(
            projectDir,
            "generateFeatureModule",
            "--module-name", " ",
            "--package-name", "com.example.foo",
        ).buildAndFail()
        assertTrue(result.output.contains("module name is required"), result.output)
    }

    private fun scaffoldFixture(): File {
        val projectDir = Files.createTempDirectory("feature-scaffold").toFile()
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            pluginManagement { repositories { gradlePluginPortal(); mavenCentral(); google() } }
            dependencyResolutionManagement { repositories { mavenCentral(); google() } }
            rootProject.name = "feature-scaffold-consumer"
            """.trimIndent(),
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            plugins { id("build-logic.feature-scaffold") }
            """.trimIndent(),
        )
        return projectDir
    }

    private fun runner(projectDir: File, vararg args: String): GradleRunner =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments("help", *args, "--stacktrace")
}
