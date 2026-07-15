package com.eterocell.gradle.android

import java.nio.file.Path
import java.util.zip.ZipFile

data class ReleaseArtifactIdentity(
    val applicationId: String,
    val versionName: String,
    val versionCode: Int,
)

data class ReleaseApkDescriptor(
    val path: Path,
    val abi: String?,
)

data class ReleaseApkFilter(
    val type: String,
    val identifier: String,
)

fun releaseApkAbiFilter(
    filters: List<ReleaseApkFilter>,
    supportedAbis: List<String>,
): String? {
    if (filters.isEmpty()) return null
    require(filters.size == 1) { "Release APK metadata must contain at most one ABI filter." }
    val filter = filters.single()
    require(filter.type == "ABI") { "Unsupported release APK filter type '${filter.type}'." }
    require(filter.identifier in supportedAbis) {
        "Unsupported release APK ABI filter '${filter.identifier}'; expected one of $supportedAbis."
    }
    return filter.identifier
}

fun validateReleaseApkSet(
    descriptors: List<ReleaseApkDescriptor>,
    supportedAbis: List<String>,
    splitApkEnabled: Boolean,
) {
    val expectedFilters = if (splitApkEnabled) supportedAbis.map<String, String?> { it } + null else listOf(null)
    val actualFilters = descriptors.map(ReleaseApkDescriptor::abi)
    require(actualFilters.size == expectedFilters.size && actualFilters.toSet() == expectedFilters.toSet()) {
        "Release APK output mismatch: expected filters ${formatFilters(expectedFilters)}, " +
            "actual filters ${formatFilters(actualFilters)}."
    }
}

fun expectedTagLibEntries(abi: String?, supportedAbis: List<String>): Set<String> =
    (abi?.let(::listOf) ?: supportedAbis)
        .mapTo(linkedSetOf()) { "lib/$it/librhythhaus_taglib.so" }

fun readTagLibEntries(apk: Path): Set<String> = ZipFile(apk.toFile()).use { zip ->
    val entries = zip.entries().asSequence()
        .map { it.name }
        .filter { entry ->
            entry.startsWith("lib/") &&
                entry.count { it == '/' } == 2 &&
                entry.endsWith("/librhythhaus_taglib.so")
        }
        .toList()
    val duplicates = entries.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
    require(duplicates.isEmpty()) { "Release APK contains duplicate TagLib entries: $duplicates." }
    entries.toCollection(linkedSetOf())
}

fun validateApkTagLibEntries(
    descriptor: ReleaseApkDescriptor,
    supportedAbis: List<String>,
): Set<String> {
    val expected = expectedTagLibEntries(descriptor.abi, supportedAbis)
    val actual = readTagLibEntries(descriptor.path)
    require(actual == expected) {
        "Release APK TagLib entries mismatch for ${descriptor.abi ?: "unfiltered"}: " +
            "expected $expected, actual $actual."
    }
    return actual
}

fun parseApkAnalyzerIdentity(
    applicationIdOutput: String,
    versionNameOutput: String,
    versionCodeOutput: String,
): ReleaseArtifactIdentity {
    val applicationId = applicationIdOutput.trim()
    val versionName = versionNameOutput.trim()
    val versionCodeText = versionCodeOutput.trim()
    require(applicationId.isNotEmpty()) { "apkanalyzer returned an empty application ID." }
    require(versionName.isNotEmpty()) { "apkanalyzer returned an empty version name." }
    val versionCode = versionCodeText.toIntOrNull()
        ?: throw IllegalArgumentException("apkanalyzer returned a noninteger version code.")
    return ReleaseArtifactIdentity(applicationId, versionName, versionCode)
}

fun validateReleaseArtifactIdentity(
    actual: ReleaseArtifactIdentity,
    expected: ReleaseArtifactIdentity,
) {
    require(actual == expected) { "Release APK identity mismatch: expected $expected, actual $actual." }
}

fun releaseSigningStatus(
    signingConfigured: Boolean,
    toolAvailable: Boolean,
    verificationSucceeded: Boolean,
): String {
    if (toolAvailable && verificationSucceeded) return "verified"
    if (!signingConfigured) return "unsigned local release"
    require(toolAvailable) { "Release signing is configured but apksigner is unavailable." }
    require(verificationSucceeded) { "Release signing is configured but apksigner verification failed." }
    return "verified"
}

fun sanitizedToolFailure(toolName: String, ignoredOutput: String): Nothing {
    ignoredOutput.length
    throw IllegalArgumentException("$toolName failed; rerun the tool locally for diagnostic output.")
}

private fun formatFilters(filters: List<String?>): String =
    filters.joinToString(prefix = "[", postfix = "]") { it ?: "unfiltered" }
