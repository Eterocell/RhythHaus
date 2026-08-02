package com.eterocell.gradle.architecture

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

public abstract class ArchitectureCheckTask : DefaultTask() {
    @get:Input public abstract val dependencyEdges: ListProperty<String>
    @get:Input public abstract val kotlinModules: SetProperty<String>
    @get:Input public abstract val strictExplicitApiModules: SetProperty<String>
    @get:Input public abstract val sqlDelightOwners: SetProperty<String>
    @get:Input public abstract val iosExports: ListProperty<String>
    @get:Input public abstract val resourceRecords: ListProperty<String>
    @get:Input public abstract val androidTestConfigurations: ListProperty<String>
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) public abstract val resources: ConfigurableFileCollection
    @get:Input public abstract val sqlDelightRecords: ListProperty<String>
    @get:Input public abstract val sqlDelightArtifactRoots: MapProperty<String, String>
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE) public abstract val sqlDelightArtifacts: ConfigurableFileCollection

    @TaskAction
    public fun checkArchitecture() {
        val edges = dependencyEdges.get().map(::decodeEdge)
        val directEdges = edges.map { it.from to it.to }.toSet()
        val violations = mutableListOf<String>()

        findCycles(directEdges).forEach { violations += "ARCH-CYCLE $it" }
        edges.sortedWith(compareBy<Edge> { it.from }.thenBy { it.configuration }.thenBy { it.to })
            .filterNot { ArchitectureAllowList.isAllowed(it.from, it.to) }
            .forEach { violations += "ARCH-EDGE ${it.from} [${it.configuration}] -> ${it.to}" }
        resourceRecords.get().sorted().forEach { record ->
            val (module, sourceSet, kind, root, namespace) = record.split("|", limit = 5)
            if (kind == "UNSUPPORTED") {
                violations += "ARCH-RESOURCE $module [$sourceSet] root=$root unsupported"
            } else if ((kind == "COMPOSE" || kind == "ANDROID") && namespace == "<invalid>") {
                violations += "ARCH-RESOURCE $module [$sourceSet] root=$root namespace=$namespace"
            }
        }
        kotlinModules.get()
            .filter(ArchitectureAllowList::requiresExplicitApi)
            .filterNot(strictExplicitApiModules.get()::contains)
            .sorted()
            .forEach { violations += "ARCH-EXPLICIT-API $it" }
        val sqlDelightRoots = sqlDelightRecords.get().map(::decodeSqlDelightRecord)
        sqlDelightRoots.filter { it.status != "VALID" }.sortedBy { it.module }.forEach {
            violations += "ARCH-SQLDELIGHT ${it.module} [${it.database}] root=${it.root} ${it.status.lowercase()}"
        }
        val physicalArtifacts = sqlDelightArtifacts.files
        val physicalOwners = sqlDelightRoots
            .filter { it.status == "VALID" }
            .filter { root ->
                val rootPath = File(sqlDelightArtifactRoots.get().getValue(root.artifactRootKey())).toPath().toAbsolutePath().normalize()
                physicalArtifacts.any { artifact ->
                    artifact.toPath().toAbsolutePath().normalize().startsWith(rootPath)
                }
            }
            .map { it.module }
            .toSet()
        val owners = sqlDelightOwners.get().filter(physicalOwners::contains).sorted()
        val expectedOwner = ArchitectureAllowList.sqlDelightOwner()
        if (owners != listOf(expectedOwner)) {
            violations += "ARCH-SQLDELIGHT expected=$expectedOwner owners=${owners.joinToString(",").ifBlank { "<none>" }}"
        }
        iosExports.get().sorted().forEach { export ->
            val (module, target) = export.split("|", limit = 2)
            if (!ArchitectureAllowList.allowsIosExport(module, target)) violations += "ARCH-IOS-EXPORT $module -> $target"
        }
        violations.distinct().sorted().takeIf { it.isNotEmpty() }?.let { throw GradleException(it.joinToString("\n")) }
    }

    private fun findCycles(edges: Set<Pair<String, String>>): List<String> {
        val adjacency = edges.groupBy({ it.first }, { it.second }).mapValues { it.value.sorted() }
        val cycles = sortedSetOf<String>()
        fun visit(node: String, path: List<String>) {
            adjacency[node].orEmpty().forEach { next ->
                val index = path.indexOf(next)
                if (index >= 0) cycles += canonicalCycle(path.drop(index)) else visit(next, path + next)
            }
        }
        adjacency.keys.sorted().forEach { visit(it, listOf(it)) }
        return cycles.toList()
    }

    private fun canonicalCycle(nodes: List<String>): String =
        nodes.indices.map { start -> (nodes.drop(start) + nodes.take(start)).joinToString(" -> ") }
            .minOrNull()
            .orEmpty()
            .let { "$it -> ${it.substringBefore(" -> ")}" }

    private fun decodeEdge(value: String): Edge {
        val (from, configuration, to) = value.split("|", limit = 3)
        return Edge(from, configuration, to)
    }

    private fun decodeSqlDelightRecord(value: String): SqlDelightRecord {
        val (module, database, root, status) = value.split("|", limit = 4)
        return SqlDelightRecord(module, database, root, status)
    }

    private fun SqlDelightRecord.artifactRootKey(): String = "$module|$database|$root"

    private data class Edge(val from: String, val configuration: String, val to: String)
    private data class SqlDelightRecord(val module: String, val database: String, val root: String, val status: String)
}
