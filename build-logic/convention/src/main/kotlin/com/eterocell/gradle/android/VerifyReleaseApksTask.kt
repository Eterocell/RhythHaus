package com.eterocell.gradle.android

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject

@CacheableTask
abstract class VerifyReleaseApksTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val apkDirectory: DirectoryProperty

    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val supportedAbis: ListProperty<String>

    @get:Input
    abstract val splitApkEnabled: Property<Boolean>

    @get:Input
    abstract val expectedApplicationId: Property<String>

    @get:Input
    abstract val expectedVersionName: Property<String>

    @get:Input
    abstract val expectedVersionCode: Property<Int>

    @get:Input
    abstract val releaseSigningConfigured: Property<Boolean>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val sdkDirectory: DirectoryProperty

    @get:Input
    abstract val buildToolsRevision: Property<String>

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val directory = apkDirectory.get()
        val builtArtifacts = builtArtifactsLoader.get().load(directory)
            ?: throw GradleException("Release APK metadata is missing from the configured artifact directory.")
        val supported = supportedAbis.get()
        val expectedIdentity = ReleaseArtifactIdentity(
            expectedApplicationId.get(),
            expectedVersionName.get(),
            expectedVersionCode.get(),
        )
        val descriptors = builtArtifacts.elements.map { artifact ->
            val abi = releaseApkAbiFilter(
                artifact.filters.map { ReleaseApkFilter(it.filterType.name, it.identifier) },
                supported,
            )
            val outputPath = Path.of(artifact.outputFile)
            val path = if (outputPath.isAbsolute) outputPath else directory.asFile.toPath().resolve(outputPath)
            require(Files.isRegularFile(path) && Files.size(path) > 0L) {
                "Release APK metadata references a missing or empty output."
            }
            require(artifact.versionName == expectedIdentity.versionName) {
                "Release APK metadata version name mismatch."
            }
            require(artifact.versionCode == expectedIdentity.versionCode) {
                "Release APK metadata version code mismatch."
            }
            ReleaseApkDescriptor(path, abi)
        }
        require(builtArtifacts.applicationId == expectedIdentity.applicationId) {
            "Release APK metadata application ID mismatch."
        }
        validateReleaseApkSet(descriptors, supported, splitApkEnabled.get())

        val apkanalyzer = resolveApkAnalyzer(sdkDirectory.get().asFile)
        val apksigner = resolveApkSigner(
            sdkDirectory.get().asFile,
            buildToolsRevision.get(),
        )
        val entriesByFilter = linkedMapOf<String, Set<String>>()
        var allSignaturesVerified = apksigner != null
        descriptors.sortedWith(compareBy({ it.abi == null }, { it.abi.orEmpty() })).forEach { descriptor ->
            entriesByFilter[descriptor.abi ?: "unfiltered"] = validateApkTagLibEntries(descriptor, supported)
            validateReleaseArtifactIdentity(
                actual = parseApkAnalyzerIdentity(
                    applicationIdOutput = runTool(apkanalyzer, "manifest", "application-id", descriptor.path.toString()),
                    versionNameOutput = runTool(apkanalyzer, "manifest", "version-name", descriptor.path.toString()),
                    versionCodeOutput = runTool(apkanalyzer, "manifest", "version-code", descriptor.path.toString()),
                ),
                expected = expectedIdentity,
            )
            if (apksigner != null) {
                allSignaturesVerified = allSignaturesVerified &&
                    runToolForSuccess(apksigner, "verify", "--verbose", "-Werr", descriptor.path.toString())
            }
        }
        val signingStatus = releaseSigningStatus(
            signingConfigured = releaseSigningConfigured.get(),
            toolAvailable = apksigner != null,
            verificationSucceeded = allSignaturesVerified,
        )
        val filters = descriptors.map { it.abi ?: "unfiltered" }.sortedWith(filterComparator(supported))
        val report = buildList {
            add("mode: ${if (splitApkEnabled.get()) "split" else "ordinary"}")
            add("outputs: ${descriptors.size}")
            add("filters: ${filters.joinToString(prefix = "[", postfix = "]")}")
            filters.forEach { filter ->
                add("taglib[$filter]: ${entriesByFilter.getValue(filter).sorted()}")
            }
            add("applicationId: ${expectedIdentity.applicationId}")
            add("versionName: ${expectedIdentity.versionName}")
            add("versionCode: ${expectedIdentity.versionCode}")
            add("signed: $signingStatus")
        }.joinToString(separator = "\n", postfix = "\n")
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(report)
        }
    }

    private fun runTool(executable: File, vararg arguments: String): String {
        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        val result = try {
            execOperations.exec {
                commandLine(executable, *arguments)
                isIgnoreExitValue = true
                this.standardOutput = standardOutput
                this.errorOutput = errorOutput
            }
        } catch (failure: Exception) {
            throw GradleException("${executable.name} could not be executed.")
        }
        if (result.exitValue != 0) sanitizedToolFailure(executable.name, errorOutput.toString())
        return standardOutput.toString()
    }

    private fun runToolForSuccess(executable: File, vararg arguments: String): Boolean = try {
        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(executable, *arguments)
            isIgnoreExitValue = true
            this.standardOutput = standardOutput
            this.errorOutput = errorOutput
        }.exitValue == 0
    } catch (failure: Exception) {
        false
    }
}

internal fun resolveApkAnalyzer(sdkDirectory: File): File {
    val latest = sdkDirectory.resolve("cmdline-tools/latest/bin/apkanalyzer")
    if (latest.isFile && latest.canExecute()) return latest
    val cmdlineTools = sdkDirectory.resolve("cmdline-tools")
    val versioned = cmdlineTools.listFiles()
        .orEmpty()
        .filter { it.isDirectory && it.name != "latest" }
        .sortedWith { left, right ->
            compareRevisions(right.name, left.name).takeIf { it != 0 }
                ?: right.name.compareTo(left.name)
        }
        .map { it.resolve("bin/apkanalyzer") }
        .firstOrNull { it.isFile && it.canExecute() }
    return versioned ?: throw GradleException(
        "Android SDK apkanalyzer is unavailable; install Android SDK Command-line Tools.",
    )
}

internal fun resolveApkSigner(sdkDirectory: File, buildToolsRevision: String): File? =
    sdkDirectory.resolve("build-tools/$buildToolsRevision/apksigner")
        .takeIf { it.isFile && it.canExecute() }

private fun compareRevisions(left: String, right: String): Int {
    val leftParts = left.split('.').map { it.toIntOrNull() ?: -1 }
    val rightParts = right.split('.').map { it.toIntOrNull() ?: -1 }
    repeat(maxOf(leftParts.size, rightParts.size)) { index ->
        val comparison = leftParts.getOrElse(index) { 0 }.compareTo(rightParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

private fun filterComparator(supportedAbis: List<String>): Comparator<String> =
    compareBy { filter ->
        val index = supportedAbis.indexOf(filter)
        if (index >= 0) index else supportedAbis.size
    }
