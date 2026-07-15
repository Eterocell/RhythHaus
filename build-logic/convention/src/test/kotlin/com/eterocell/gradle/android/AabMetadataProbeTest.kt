package com.eterocell.gradle.android

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AabMetadataProbeTest {
    @Test
    fun realAgpBundleConvertsToCanonicalSdkMetadata() {
        val bundlePath = System.getProperty(AAB_PROBE_FILE_PROPERTY)
            ?.takeIf(String::isNotBlank)
            ?.let(Path::of)
            ?: error("Required AAB probe input is missing; pass -P$AAB_PROBE_FILE_PROPERTY=<release.aab>.")
        require(Files.isRegularFile(bundlePath) && Files.size(bundlePath) > 0L) {
            "Required AAB probe input must be an existing non-empty file."
        }

        assertEquals(
            ReleaseArtifactIdentity("com.eterocell.rhythhaus", "0.1.0", 100),
            probeAabMetadata(bundlePath),
        )
    }

    @Test
    fun releaseAabMustBeAnExistingNonemptyFile() {
        val missing = Files.createTempDirectory("missing-aab").resolve("release.aab")
        val empty = Files.createTempFile("empty-aab", ".aab")

        assertFailsWith<IllegalArgumentException> { requireReleaseAab(missing) }
        assertFailsWith<IllegalArgumentException> { requireReleaseAab(empty) }
    }

    @Test
    fun protoEntryMappingsRequireManifestAndResourceTable() {
        listOf(
            listOf("base/resources.pb"),
            listOf("base/manifest/AndroidManifest.xml"),
        ).forEach { entries ->
            assertFailsWith<IllegalArgumentException> { aabProtoEntryMappings(entries) }
        }
    }

    @Test
    fun protoEntryMappingsRejectDuplicateRequiredAndResourceEntries() {
        listOf(
            listOf(
                "base/manifest/AndroidManifest.xml",
                "base/manifest/AndroidManifest.xml",
                "base/resources.pb",
            ),
            listOf(
                "base/manifest/AndroidManifest.xml",
                "base/resources.pb",
                "base/resources.pb",
            ),
            listOf(
                "base/manifest/AndroidManifest.xml",
                "base/resources.pb",
                "base/res/drawable/icon.xml",
                "base/res/drawable/icon.xml",
            ),
            listOf(
                "base/manifest/AndroidManifest.xml",
                "base/resources.pb",
                "base/res/",
                "base/res/",
            ),
        ).forEach { entries ->
            val error = assertFailsWith<IllegalArgumentException> {
                aabProtoEntryMappings(entries)
            }
            assertTrue(error.message.orEmpty().contains("duplicate"))
        }
    }

    @Test
    fun protoEntryMappingsIgnoreUniqueDirectoryMarkersAfterDuplicateValidation() {
        assertEquals(
            listOf(
                AabProtoEntry("base/manifest/AndroidManifest.xml", "AndroidManifest.xml"),
                AabProtoEntry("base/resources.pb", "resources.pb"),
            ),
            aabProtoEntryMappings(
                listOf(
                    "base/",
                    "base/manifest/",
                    "base/manifest/AndroidManifest.xml",
                    "base/resources.pb",
                    "base/res/",
                ),
            ),
        )
    }

    @Test
    fun protoEntryMappingsPreserveSortedBaseResourcePaths() {
        assertEquals(
            listOf(
                AabProtoEntry("base/manifest/AndroidManifest.xml", "AndroidManifest.xml"),
                AabProtoEntry("base/resources.pb", "resources.pb"),
                AabProtoEntry("base/res/drawable/icon.xml", "res/drawable/icon.xml"),
                AabProtoEntry("base/res/mipmap/icon.png", "res/mipmap/icon.png"),
            ),
            aabProtoEntryMappings(
                listOf(
                    "base/res/mipmap/icon.png",
                    "base/resources.pb",
                    "base/res/drawable/icon.xml",
                    "base/manifest/AndroidManifest.xml",
                ),
            ),
        )
    }

    @Test
    fun protoEntryMappingsRejectMalformedAndUnsafeResourcePaths() {
        listOf(
            "base/res/../manifest/AndroidManifest.xml",
            "base/res/../../outside",
            "base/res\\drawable\\icon.xml",
            "base/res//icon.xml",
        ).forEach { unsafe ->
            val error = assertFailsWith<IllegalArgumentException> {
                aabProtoEntryMappings(
                    listOf(
                        "base/manifest/AndroidManifest.xml",
                        "base/resources.pb",
                        unsafe,
                    ),
                )
            }
            assertTrue(error.message.orEmpty().contains("unsafe base resource path"))
        }
    }

    @Test
    fun aabToolFailureDoesNotExposeSdkOutput() {
        val marker = "DO_NOT_LEAK_AAB_TOOL_OUTPUT"
        val error = assertFailsWith<IllegalArgumentException> {
            sanitizedAabToolFailure("aapt2", marker)
        }

        assertFalse(error.message.orEmpty().contains(marker))
        assertTrue(error.message.orEmpty().contains("aapt2"))
    }

    @Test
    fun aabIdentityMismatchIncludesExpectedAndActualValues() {
        val expected = ReleaseArtifactIdentity("com.eterocell.rhythhaus", "0.1.0", 100)
        val actual = expected.copy(versionCode = 101)

        val error = assertFailsWith<IllegalArgumentException> {
            validateReleaseAabIdentity(actual, expected)
        }

        assertTrue(error.message.orEmpty().contains("expected $expected"))
        assertTrue(error.message.orEmpty().contains("actual $actual"))
    }

    private companion object {
        const val AAB_PROBE_FILE_PROPERTY = "rhythhaus.aabProbeFile"
    }
}

private fun probeAabMetadata(bundlePath: Path): ReleaseArtifactIdentity {
    val sdkDirectory = probeAndroidSdkDirectory()
    val aapt2 = probeLatestExecutable(sdkDirectory.resolve("build-tools"), "aapt2")
    val apkAnalyzer = resolveApkAnalyzer(sdkDirectory.toFile()).toPath()
    val temporaryDirectory = Files.createTempDirectory("rhythhaus-aab-probe")
    try {
        val protoArchive = temporaryDirectory.resolve("base-proto.apk")
        val convertedApk = temporaryDirectory.resolve("base-binary.apk")
        ZipFile(bundlePath.toFile()).use { bundle ->
            ZipOutputStream(Files.newOutputStream(protoArchive)).use { output ->
                listOf(
                    "base/manifest/AndroidManifest.xml" to "AndroidManifest.xml",
                    "base/resources.pb" to "resources.pb",
                ).forEach { (sourceName, targetName) ->
                    val source = requireNotNull(bundle.getEntry(sourceName)) { "Release AAB is missing $sourceName." }
                    output.putNextEntry(ZipEntry(targetName))
                    bundle.getInputStream(source).use { it.copyTo(output) }
                    output.closeEntry()
                }
                bundle.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .filter { it.name.startsWith("base/res/") }
                    .sortedBy { it.name }
                    .forEach { source ->
                        val targetName = safeProbeResourcePath(source.name)
                        output.putNextEntry(ZipEntry(targetName))
                        bundle.getInputStream(source).use { it.copyTo(output) }
                        output.closeEntry()
                    }
            }
        }
        probeRunSdkTool(
            aapt2,
            "convert",
            "--output-format",
            "binary",
            "-o",
            convertedApk.toString(),
            protoArchive.toString(),
        )
        return parseApkAnalyzerIdentity(
            applicationIdOutput = probeRunSdkTool(
                apkAnalyzer,
                "manifest",
                "application-id",
                convertedApk.toString(),
            ),
            versionNameOutput = probeRunSdkTool(
                apkAnalyzer,
                "manifest",
                "version-name",
                convertedApk.toString(),
            ),
            versionCodeOutput = probeRunSdkTool(
                apkAnalyzer,
                "manifest",
                "version-code",
                convertedApk.toString(),
            ),
        )
    } finally {
        temporaryDirectory.toFile().deleteRecursively()
    }
}

private fun safeProbeResourcePath(sourceName: String): String {
    require(!sourceName.contains('\\')) { "Release AAB contains an unsafe base resource path." }
    val relative = sourceName.removePrefix("base/")
    val path = Path.of(relative)
    require(!path.isAbsolute && path.normalize() == path && path.startsWith("res") && path.nameCount > 1) {
        "Release AAB contains an unsafe base resource path."
    }
    return path.toString().replace(File.separatorChar, '/')
}

private fun probeAndroidSdkDirectory(): Path {
    val environmentPath = System.getenv("ANDROID_SDK_ROOT") ?: System.getenv("ANDROID_HOME")
    if (!environmentPath.isNullOrBlank()) return Path.of(environmentPath)
    val properties = Properties().apply {
        File("../local.properties").takeIf(File::isFile)?.inputStream()?.use(::load)
    }
    return properties.getProperty("sdk.dir")?.let(Path::of)
        ?: error("Android SDK directory is unavailable for the AAB probe.")
}

private fun probeLatestExecutable(parent: Path, executableName: String): Path =
    Files.list(parent).use { directories ->
        directories
            .filter(Files::isDirectory)
            .map { it.resolve(executableName) }
            .filter(Files::isExecutable)
            .max(Comparator.comparing { it.parent.fileName.toString() })
            .orElseThrow { IllegalStateException("Android SDK $executableName is unavailable.") }
    }

private fun probeRunSdkTool(executable: Path, vararg arguments: String): String {
    val output = ByteArrayOutputStream()
    val process = ProcessBuilder(executable.toString(), *arguments)
        .redirectErrorStream(true)
        .start()
    process.inputStream.use { it.copyTo(output) }
    require(process.waitFor() == 0) { "${executable.fileName} failed; rerun locally for diagnostic output." }
    return output.toString()
}
