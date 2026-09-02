package com.eterocell.gradle.architecture

import java.io.File
import java.util.Collections
import java.util.IdentityHashMap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

public class ArchitectureCheckPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) { "build-logic.architecture-check must be applied to the root project" }
        val architectureCheck = project.tasks.register<ArchitectureCheckTask>("architectureCheck") {
            group = "verification"
            description = "Checks the configured project architecture."
        }
        val versionsPluginAggregationDependencies =
            Collections.newSetFromMap(IdentityHashMap<ProjectDependency, Boolean>())
        fun captureVersionsPluginAggregation() {
            project.configurations.findByName("dependencyUpdatesAggregation")
                ?.dependencies
                ?.withType(ProjectDependency::class.java)
                ?.forEach(versionsPluginAggregationDependencies::add)
        }
        project.pluginManager.withPlugin("com.github.ben-manes.versions") { captureVersionsPluginAggregation() }
        project.pluginManager.withPlugin("io.github.ben-manes.versions") { captureVersionsPluginAggregation() }
        project.gradle.projectsEvaluated {
            architectureCheck.configure {
                val projects = project.allprojects.sortedBy(Project::getPath)
                val registry = ArchitectureModelRegistry.forRoot(project).snapshot()
                dependencyEdges.set(projects.flatMap { consumer ->
                    consumer.configurations.sortedBy { it.name }.flatMap { configuration ->
                        val directSelfDependencyCount = configuration.dependencies
                            .withType(ProjectDependency::class.java)
                            .count { dependency -> dependency.path == consumer.path }
                        configuration.dependencies.withType(ProjectDependency::class.java)
                            .filterNot { dependency ->
                                val isVersionsPluginAggregation = dependency in versionsPluginAggregationDependencies
                                val isKspRegistration = registry.kspRegistrations.any { registration ->
                                    dependency.path == registration.processor &&
                                        consumer.path == registration.module &&
                                        configuration.name == registration.configuration
                                }
                                val isAndroidSyntheticSelfDependency = registry
                                    .suppressesAndroidSyntheticSelfDependency(
                                        consumerPath = consumer.path,
                                        configurationName = configuration.name,
                                        providerPath = dependency.path,
                                        directSelfDependencyCount = directSelfDependencyCount,
                                    )
                                isVersionsPluginAggregation || isKspRegistration || isAndroidSyntheticSelfDependency
                            }
                            .map { "${consumer.path}|${configuration.name}|${it.path}" }
                    }
                }.distinct().sorted())
                kotlinModules.set(projects.filter { it.extensions.findByType(KotlinBaseExtension::class.java) != null }.map(Project::getPath).toSortedSet())
                strictExplicitApiModules.set(projects.filter(::hasStrictExplicitApi).map(Project::getPath).toSortedSet())
                sqlDelightOwners.set(registry.sqlDelightOwners)
                iosExports.set(projects.flatMap(::iosExports).distinct().sorted())
                val resourceRecords = mutableListOf<String>()
                projects.forEach { candidate ->
                    candidate.standardResourceRoots().forEach { resource ->
                        resourceRecords += resource.encode(candidate.path, project.rootDir)
                        resources.from(candidate.fileTree(resource.root))
                    }
                }
                resources.from(
                    registry.resources.map { records ->
                        records
                            .filter { it.kind != "UNSUPPORTED" }
                            .map { it.root }
                    },
                )
                this.resourceRecords.set(
                    registry.resources.map { records ->
                        (resourceRecords + records.map { it.encode(project.rootDir) }).sorted()
                    },
                )
                this.androidTestConfigurations.set(
                    registry.androidTestConfigurations
                        .map { "${it.module}|${it.configuration}" }
                        .sorted(),
                )
                val sqlDelightRecords = mutableListOf<String>()
                val sqlDelightArtifactRoots = sortedMapOf<String, String>()
                registry.sqlDelightRoots.forEach { root ->
                    sqlDelightRecords += root.encode(project.rootDir)
                    if (root.status == "VALID") {
                        sqlDelightArtifactRoots[root.artifactRootKey(project.rootDir)] = root.root.absolutePath
                        val artifacts = project.fileTree(root.root) {
                            include("**/*.sq", "**/*.sqm", "**/*.db")
                        }
                        sqlDelightArtifacts.from(artifacts)
                    }
                }
                this.sqlDelightRecords.set(sqlDelightRecords.distinct().sorted())
                this.sqlDelightArtifactRoots.set(sqlDelightArtifactRoots)
            }
        }
    }

    private fun hasStrictExplicitApi(project: Project): Boolean =
        project.extensions.findByType(KotlinBaseExtension::class.java)?.explicitApi == ExplicitApiMode.Strict

    private fun Project.standardResourceRoots(): List<ResourceRoot> {
        val normalizedBuildDirectory = layout.buildDirectory.get().asFile.toPath().toAbsolutePath().normalize()
        val multiplatform = extensions.findByType(KotlinMultiplatformExtension::class.java)
        if (multiplatform != null) {
            return multiplatform.sourceSets
                .filter { it.name.endsWith("Main") }
                .flatMap { sourceSet ->
                    val standard = sourceSet.resources.srcDirs
                        .filterNot { root ->
                            root.toPath().toAbsolutePath().normalize().startsWith(normalizedBuildDirectory)
                        }
                        .map { root ->
                            ResourceRoot(sourceSet.name, "KOTLIN", root, "")
                        }
                    standard
                }
        }
        return extensions.findByType(SourceSetContainer::class.java)
            ?.findByName("main")
            ?.resources
            ?.srcDirs
            ?.map { ResourceRoot("main", "JVM", it, "") }
            .orEmpty()
    }

    private fun iosExports(project: Project): List<String> {
        val kotlin = project.extensions.findByType(KotlinMultiplatformExtension::class.java) ?: return emptyList()
        return kotlin.targets.withType(KotlinNativeTarget::class.java).flatMap { target ->
            target.binaries.withType(Framework::class.java).flatMap { framework ->
                val exports = project.configurations.getByName(framework.exportConfigurationName).dependencies.withType(ProjectDependency::class.java).map { "${project.path}|${it.path}" }
                if (framework.transitiveExport) exports + "${project.path}|<transitive export>" else exports
            }
        }
    }

    private data class ResourceRoot(
        val sourceSet: String,
        val kind: String,
        val root: File,
        val namespace: String,
    ) {
        fun encode(module: String, rootProject: File): String =
            "$module|$sourceSet|$kind|${root.relativeToOrSelf(rootProject).invariantSeparatorsPath}|$namespace"
    }
}

private fun ArchitectureModelRegistry.ResourceRecord.encode(rootProject: File): String =
    "$module|$sourceSet|$kind|${root.rootRelativeOrAbsolute(rootProject)}|$namespace"

private fun ArchitectureModelRegistry.SqlDelightRootRecord.encode(rootProject: File): String =
    "$module|$database|${root.rootRelativeOrAbsolute(rootProject)}|$status"

private fun ArchitectureModelRegistry.SqlDelightRootRecord.artifactRootKey(rootProject: File): String =
    "$module|$database|${root.rootRelativeOrAbsolute(rootProject)}"

private fun File.rootRelativeOrAbsolute(rootProject: File): String {
    val path = toPath().toAbsolutePath().normalize()
    val root = rootProject.toPath().toAbsolutePath().normalize()
    val encoded = if (path.startsWith(root)) root.relativize(path).toString() else path.toString()
    return encoded.replace(File.separatorChar, '/')
}
