package com.eterocell.gradle.android

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidReleaseVerificationTest {
    private val supportedAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")

    @Test
    fun splitModeAcceptsExactlyThreeAbiOutputsAndOneUnfilteredOutput() {
        validateReleaseApkSet(
            descriptors = splitDescriptors(),
            supportedAbis = supportedAbis,
            splitApkEnabled = true,
        )
    }

    @Test
    fun ordinaryModeAcceptsExactlyOneUnfilteredOutput() {
        validateReleaseApkSet(
            descriptors = listOf(descriptor(null)),
            supportedAbis = supportedAbis,
            splitApkEnabled = false,
        )
    }

    @Test
    fun outputMatrixRejectsMissingDuplicateUnsupportedAndUnexpectedFilters() {
        val invalidSets = listOf(
            splitDescriptors().dropLast(1),
            splitDescriptors() + descriptor("arm64-v8a"),
            splitDescriptors().drop(1) + descriptor("x86"),
            listOf(descriptor("arm64-v8a")),
        )

        invalidSets.forEach { descriptors ->
            val error = assertFailsWith<IllegalArgumentException> {
                validateReleaseApkSet(descriptors, supportedAbis, descriptors.size != 1)
            }
            assertTrue(error.message.orEmpty().contains("expected filters"))
            assertTrue(error.message.orEmpty().contains("actual filters"))
        }
    }

    @Test
    fun metadataFilterConversionRejectsDensityUnknownAndMultipleFilters() {
        assertFailsWith<IllegalArgumentException> {
            releaseApkAbiFilter(listOf(ReleaseApkFilter("DENSITY", "xxhdpi")), supportedAbis)
        }
        assertFailsWith<IllegalArgumentException> {
            releaseApkAbiFilter(listOf(ReleaseApkFilter("ABI", "x86")), supportedAbis)
        }
        assertFailsWith<IllegalArgumentException> {
            releaseApkAbiFilter(
                listOf(
                    ReleaseApkFilter("ABI", "arm64-v8a"),
                    ReleaseApkFilter("ABI", "x86_64"),
                ),
                supportedAbis,
            )
        }
        assertEquals(null, releaseApkAbiFilter(emptyList(), supportedAbis))
    }

    @Test
    fun expectedTagLibEntriesAreExactForAbiAndUniversalOutputs() {
        assertEquals(
            setOf("lib/arm64-v8a/librhythhaus_taglib.so"),
            expectedTagLibEntries("arm64-v8a", supportedAbis),
        )
        assertEquals(
            supportedAbis.mapTo(linkedSetOf()) { "lib/$it/librhythhaus_taglib.so" },
            expectedTagLibEntries(null, supportedAbis),
        )
    }

    @Test
    fun zipInspectionReadsOnlyExactTagLibEntriesAndIgnoresOtherNativeLibraries() {
        val apk = zip(
            "lib/arm64-v8a/librhythhaus_taglib.so",
            "lib/arm64-v8a/libunrelated.so",
            "lib/x86_64/not-librhythhaus_taglib.so",
            "assets/librhythhaus_taglib.so",
        )

        assertEquals(
            setOf("lib/arm64-v8a/librhythhaus_taglib.so"),
            readTagLibEntries(apk),
        )
    }

    @Test
    fun nativeSliceValidationRejectsMissingAndExtraTagLibSlices() {
        val missing = zip("lib/arm64-v8a/libunrelated.so")
        val extra = zip(
            "lib/arm64-v8a/librhythhaus_taglib.so",
            "lib/x86_64/librhythhaus_taglib.so",
        )

        assertFailsWith<IllegalArgumentException> {
            validateApkTagLibEntries(descriptor("arm64-v8a", missing), supportedAbis)
        }
        assertFailsWith<IllegalArgumentException> {
            validateApkTagLibEntries(descriptor("arm64-v8a", extra), supportedAbis)
        }
    }

    @Test
    fun zipInspectionRejectsDuplicateExactTagLibEntries() {
        val apk = zip(
            "lib/arm64-v8a/librhythhaus_taglib.so",
            "lib/arm64-v8b/librhythhaus_taglib.so",
        )
        val bytes = Files.readAllBytes(apk)
        val placeholder = "lib/arm64-v8b/librhythhaus_taglib.so".encodeToByteArray()
        val duplicate = "lib/arm64-v8a/librhythhaus_taglib.so".encodeToByteArray()
        replaceAll(bytes, placeholder, duplicate)
        Files.write(apk, bytes)

        assertFailsWith<IllegalArgumentException> {
            readTagLibEntries(apk)
        }
    }

    @Test
    fun analyzerIdentityIsTrimmedAndCanonicalNumericVersionCodeIsCompared() {
        val identity = parseApkAnalyzerIdentity(
            applicationIdOutput = "  com.eterocell.rhythhaus\n",
            versionNameOutput = " 0.1.0 \n",
            versionCodeOutput = " 000100 \n",
        )

        assertEquals(
            ReleaseArtifactIdentity("com.eterocell.rhythhaus", "0.1.0", 100),
            identity,
        )
        validateReleaseArtifactIdentity(
            actual = identity,
            expected = ReleaseArtifactIdentity("com.eterocell.rhythhaus", "0.1.0", 100),
        )
    }

    @Test
    fun analyzerIdentityRejectsNonintegerVersionCodeAndMetadataMismatches() {
        assertFailsWith<IllegalArgumentException> {
            parseApkAnalyzerIdentity("com.eterocell.rhythhaus", "0.1.0", "one hundred")
        }

        val expected = ReleaseArtifactIdentity("com.eterocell.rhythhaus", "0.1.0", 100)
        listOf(
            expected.copy(applicationId = "example.invalid"),
            expected.copy(versionName = "9.9.9"),
            expected.copy(versionCode = 101),
        ).forEach { actual ->
            assertFailsWith<IllegalArgumentException> {
                validateReleaseArtifactIdentity(actual, expected)
            }
        }
    }

    @Test
    fun signingMatrixRequiresSuccessfulApksignerOnlyWhenSigningIsConfigured() {
        assertEquals("verified", releaseSigningStatus(true, toolAvailable = true, verificationSucceeded = true))
        assertEquals("verified", releaseSigningStatus(false, toolAvailable = true, verificationSucceeded = true))
        assertFailsWith<IllegalArgumentException> {
            releaseSigningStatus(true, toolAvailable = true, verificationSucceeded = false)
        }
        assertFailsWith<IllegalArgumentException> {
            releaseSigningStatus(true, toolAvailable = false, verificationSucceeded = false)
        }
        assertEquals(
            "unsigned local release",
            releaseSigningStatus(false, toolAvailable = true, verificationSucceeded = false),
        )
        assertEquals(
            "unsigned local release",
            releaseSigningStatus(false, toolAvailable = false, verificationSucceeded = false),
        )
    }

    @Test
    fun validatorFailuresDoNotContainEnvironmentCredentialsOrCertificateOutput() {
        val marker = "DO_NOT_LEAK_SECRET_MARKER"
        val error = assertFailsWith<IllegalArgumentException> {
            sanitizedToolFailure("apkanalyzer", marker)
        }

        assertFalse(error.message.orEmpty().contains(marker))
        assertFalse(error.message.orEmpty().contains("environment", ignoreCase = true))
        assertFalse(error.message.orEmpty().contains("certificate", ignoreCase = true))
        assertTrue(error.message.orEmpty().contains("apkanalyzer"))
    }

    private fun splitDescriptors(): List<ReleaseApkDescriptor> =
        supportedAbis.map(::descriptor) + descriptor(null)

    private fun descriptor(abi: String?, path: Path = Path.of("release.apk")) =
        ReleaseApkDescriptor(path = path, abi = abi)

    private fun zip(vararg entries: String): Path {
        val path = Files.createTempFile("release-verification", ".apk")
        ZipOutputStream(path.outputStream()).use { output ->
            entries.forEach { name ->
                output.putNextEntry(ZipEntry(name))
                output.write(byteArrayOf(1))
                output.closeEntry()
            }
        }
        return path
    }

    private fun replaceAll(bytes: ByteArray, old: ByteArray, new: ByteArray) {
        require(old.size == new.size)
        for (index in 0..bytes.size - old.size) {
            if (old.indices.all { offset -> bytes[index + offset] == old[offset] }) {
                new.copyInto(bytes, destinationOffset = index)
            }
        }
    }
}
