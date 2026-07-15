package com.eterocell.gradle.android

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

data class AabProtoEntry(
    val sourceName: String,
    val targetName: String,
)

fun requireReleaseAab(path: Path): Path {
    require(Files.isRegularFile(path) && Files.size(path) > 0L) {
        "Release AAB must be one existing non-empty file."
    }
    return path
}

fun aabProtoEntryMappings(entryNames: List<String>): List<AabProtoEntry> {
    val duplicates = entryNames.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Release AAB contains duplicate entries: $duplicates." }
    val required = listOf(
        AabProtoEntry("base/manifest/AndroidManifest.xml", "AndroidManifest.xml"),
        AabProtoEntry("base/resources.pb", "resources.pb"),
    )
    required.forEach { entry ->
        require(entry.sourceName in entryNames) { "Release AAB is missing ${entry.sourceName}." }
    }
    val resources = entryNames.asSequence()
        .filterNot { it.endsWith('/') }
        .filter { it == "base/res" || it.startsWith("base/res/") || it.startsWith("base/res\\") }
        .map { sourceName -> AabProtoEntry(sourceName, safeAabResourceTarget(sourceName)) }
        .sortedBy(AabProtoEntry::sourceName)
        .toList()
    return required + resources
}

fun sanitizedAabToolFailure(toolName: String, ignoredOutput: String): Nothing {
    ignoredOutput.length
    throw IllegalArgumentException("$toolName failed; rerun the tool locally for diagnostic output.")
}

fun validateReleaseAabIdentity(
    actual: ReleaseArtifactIdentity,
    expected: ReleaseArtifactIdentity,
) {
    require(actual == expected) { "Release AAB identity mismatch: expected $expected, actual $actual." }
}

fun resolveAapt2(sdkDirectory: File, buildToolsRevision: String): File =
    sdkDirectory.resolve("build-tools/$buildToolsRevision/aapt2")
        .takeIf { it.isFile && it.canExecute() }
        ?: throw GradleException(
            "Android SDK aapt2 is unavailable for build tools $buildToolsRevision.",
        )

@CacheableTask
abstract class VerifyReleaseAabTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aabFile: RegularFileProperty

    @get:Input
    abstract val expectedApplicationId: Property<String>

    @get:Input
    abstract val expectedVersionName: Property<String>

    @get:Input
    abstract val expectedVersionCode: Property<Int>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val aapt2Executable: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val apkAnalyzerExecutable: RegularFileProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun verify() {
        val bundle = requireReleaseAab(aabFile.get().asFile.toPath())
        val expected = ReleaseArtifactIdentity(
            expectedApplicationId.get(),
            expectedVersionName.get(),
            expectedVersionCode.get(),
        )
        val protoArchive = temporaryDir.resolve("base-proto.apk")
        val convertedApk = temporaryDir.resolve("base-binary.apk")
        temporaryDir.mkdirs()
        val mappings = ZipFile(bundle.toFile()).use { zip ->
            val entries = zip.entries().asSequence().toList()
            val resolvedMappings = aabProtoEntryMappings(entries.map { it.name })
            ZipOutputStream(protoArchive.outputStream()).use { output ->
                resolvedMappings.forEach { mapping ->
                    val source = requireNotNull(zip.getEntry(mapping.sourceName)) {
                        "Release AAB is missing ${mapping.sourceName}."
                    }
                    output.putNextEntry(ZipEntry(mapping.targetName))
                    zip.getInputStream(source).use { it.copyTo(output) }
                    output.closeEntry()
                }
            }
            resolvedMappings
        }
        runTool(
            aapt2Executable.get().asFile,
            "convert",
            "--output-format",
            "binary",
            "-o",
            convertedApk.absolutePath,
            protoArchive.absolutePath,
        )
        val analyzer = apkAnalyzerExecutable.get().asFile
        val actual = parseApkAnalyzerIdentity(
            applicationIdOutput = runTool(analyzer, "manifest", "application-id", convertedApk.absolutePath),
            versionNameOutput = runTool(analyzer, "manifest", "version-name", convertedApk.absolutePath),
            versionCodeOutput = runTool(analyzer, "manifest", "version-code", convertedApk.absolutePath),
        )
        validateReleaseAabIdentity(actual, expected)
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(
                buildString {
                    appendLine("outputs: 1")
                    appendLine("resources: ${mappings.size - 2}")
                    appendLine("applicationId: ${actual.applicationId}")
                    appendLine("versionName: ${actual.versionName}")
                    appendLine("versionCode: ${actual.versionCode}")
                },
            )
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
        if (result.exitValue != 0) sanitizedAabToolFailure(executable.name, errorOutput.toString())
        return standardOutput.toString()
    }
}

private fun safeAabResourceTarget(sourceName: String): String {
    require(sourceName.startsWith("base/res/") && !sourceName.contains('\\')) {
        "Release AAB contains an unsafe base resource path."
    }
    val relative = sourceName.removePrefix("base/")
    require(relative.isNotEmpty() && relative.split('/').none { it.isEmpty() || it == "." || it == ".." }) {
        "Release AAB contains an unsafe base resource path."
    }
    val path = Path.of(relative)
    require(!path.isAbsolute && path.normalize() == path && path.startsWith("res") && path.nameCount > 1) {
        "Release AAB contains an unsafe base resource path."
    }
    return relative
}
