package com.eterocell.gradle.scaffold

import org.gradle.api.DefaultTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.kotlin.dsl.register
import java.io.File

/**
 * Generates a real requested feature-module structure and nothing else. The task creates the
 * module build file, the Kotlin source skeleton, and a README, and — only when an API contract
 * name is supplied — a minimal API module. It never generates speculative empty
 * `UiState`/`UiEvent`/`UiEffect`/Presenter/ViewModel scaffolding.
 */
public abstract class GenerateFeatureModuleTask : DefaultTask() {
    @get:Input
    @get:Option(option = "module-name", description = "Feature module name (e.g. player).")
    public abstract val moduleName: Property<String>

    @get:Input
    @get:Option(option = "package-name", description = "Kotlin package (e.g. com.eterocell.rhythhaus.player).")
    public abstract val packageName: Property<String>

    @get:Input
    @get:Optional
    @get:Option(option = "api-contract", description = "Optional API contract name; generates an API module when present.")
    public abstract val apiContract: Property<String>

    @TaskAction
    public fun generate() {
        val name = moduleName.get().trim()
        val pkg = packageName.get().trim()
        require(name.isNotBlank()) { "module name is required" }
        require(pkg.isNotBlank()) { "package name is required" }
        require(name.all { it.isLowerCase() || it.isDigit() }) {
            "module name must be lowercase alphanumeric, got: $name"
        }

        val moduleDir = project.rootDir.resolve("feature/$name")
        val pkgPath = pkg.replace('.', '/')

        moduleDir.resolve("build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins {
                    id("build-logic.kmp.feature.impl")
                    id("build-logic.android.kmp.library")
                }

                kotlin {
                    android {
                        namespace = "$pkg"
                        compileSdk = 37
                        minSdk = 29
                        withHostTest {}
                        androidResources { enable = true }
                    }
                    jvm()
                    iosArm64()
                    iosSimulatorArm64()
                }
                """.trimIndent(),
            )
        }

        moduleDir.resolve("src/commonMain/kotlin/$pkgPath/Feature.kt").apply {
            parentFile.mkdirs()
            writeText(
                """
                package $pkg

                /** Public surface placeholder for the `$name` feature. */
                public object Feature
                """.trimIndent(),
            )
        }

        moduleDir.resolve("README.md").writeText("# $name\n\nReal requested `$name` feature module.\n")

        val contract = apiContract.getOrNull()?.trim()
        if (contract != null) {
            require(contract.isNotBlank()) { "api-contract must be a non-blank name when supplied" }
            val apiDir = project.rootDir.resolve("feature/$name-api")
            apiDir.resolve("build.gradle.kts").apply {
                parentFile.mkdirs()
                writeText(
                    """
                    plugins {
                        id("build-logic.kmp.feature.api")
                    }
                    """.trimIndent(),
                )
            }
            apiDir.resolve("src/commonMain/kotlin/$pkgPath/$contract.kt").apply {
                parentFile.mkdirs()
                writeText(
                    """
                    package $pkg

                    /** Requested API contract. */
                    public interface $contract
                    """.trimIndent(),
                )
            }
        }
    }
}

/** Generates only real requested feature-module structure; no speculative empty scaffolding. */
public class FeatureScaffoldPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) { "build-logic.feature-scaffold must be applied to the root project" }
        project.tasks.register<GenerateFeatureModuleTask>("generateFeatureModule") {
            group = "scaffold"
            description = "Generates a real requested feature-module structure."
        }
    }
}
