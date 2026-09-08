package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for the shared iOS Files import status mapping and the pure
 * managed-destination filename/collision policy.
 *
 * The ABI statuses and counters are supplied by the Swift import provider and
 * mapped here without ever exposing external URLs; the destination policy is
 * pure so the native copy path can mirror it deterministically. The policy
 * lives in the Shared facade so the generated framework and the Swift mirror
 * rely on the same rules.
 */
class PlatformSourceAccessTest {
    @Test
    fun iosLauncherRejectsReentryUntilTerminalCompletion() {
        assertTrue(iosImportLaunchAllowed(importActive = false))
        assertFalse(iosImportLaunchAllowed(importActive = true))
    }
    private val managedFolder = "/managed/RhythHaus Music"

    private fun mappedResult(
        status: Int,
        imported: Int = 0,
        duplicates: Int = 0,
        unsupported: Int = 0,
        failed: Int = 0,
        message: String? = null,
    ) = iosLibraryImportPickResult(
        destinationFolderPath = managedFolder,
        status = status,
        imported = imported,
        duplicates = duplicates,
        unsupported = unsupported,
        failed = failed,
        message = message,
    )

    @Test
    fun successCompletionReturnsTheAppLocalSource() {
        val success = assertIs<PlatformFolderPickResult.Success>(
            mappedResult(
                status = IOSLibraryImportStatus.SUCCESS,
                imported = 3,
                duplicates = 1,
                unsupported = 2,
                failed = 1,
            ),
        )
        assertEquals("ios-app-local", success.source.id)
        assertEquals(
            LibraryPlatformKind.IosAppLocal,
            success.source.platformKind,
        )
        assertEquals("RhythHaus", success.source.displayName)
        assertEquals(managedFolder, success.source.handle)
    }

    @Test
    fun duplicateOnlyCompletionReturnsTheAppLocalSource() {
        val success = assertIs<PlatformFolderPickResult.Success>(
            mappedResult(
                status = IOSLibraryImportStatus.SUCCESS,
                duplicates = 4,
                unsupported = 1,
            ),
        )
        assertEquals(managedFolder, success.source.handle)
    }

    @Test
    fun cancellationReturnsDistinctSilentResult() {
        // Cancellation is a distinct no-error terminal outcome: a concrete
        // result with no source and no message, never a scan trigger.
        assertIs<PlatformFolderPickResult.Cancelled>(
            mappedResult(status = IOSLibraryImportStatus.CANCELLED),
        )
    }

    @Test
    fun unavailableCompletionReturnsRecoverableUnavailable() {
        val unavailable = assertIs<PlatformFolderPickResult.Unavailable>(
            mappedResult(
                status = IOSLibraryImportStatus.UNAVAILABLE,
                message = "Files picker is unavailable",
            ),
        )
        assertEquals("Files picker is unavailable", unavailable.message)
        assertTrue(
            assertIs<PlatformFolderPickResult.Unavailable>(
                mappedResult(status = IOSLibraryImportStatus.UNAVAILABLE),
            ).message.isNotBlank(),
        )
    }

    @Test
    fun overlapCompletionReturnsRecoverableFailure() {
        val failure = assertIs<PlatformFolderPickResult.Failure>(
            mappedResult(
                status = IOSLibraryImportStatus.OVERLAP,
                message = "Another import is already active",
            ),
        )
        assertEquals("Another import is already active", failure.message)
        assertTrue(
            assertIs<PlatformFolderPickResult.Failure>(
                mappedResult(status = IOSLibraryImportStatus.OVERLAP),
            ).message.isNotBlank(),
        )
    }

    @Test
    fun failureCompletionReturnsRecoverableFailure() {
        val failure = assertIs<PlatformFolderPickResult.Failure>(
            mappedResult(
                status = IOSLibraryImportStatus.FAILURE,
                failed = 2,
                message = "Copy failed",
            ),
        )
        assertEquals("Copy failed", failure.message)
        assertTrue(
            assertIs<PlatformFolderPickResult.Failure>(
                mappedResult(status = IOSLibraryImportStatus.FAILURE),
            ).message.isNotBlank(),
        )
    }

    @Test
    fun unknownStatusReturnsRecoverableFailure() {
        assertIs<PlatformFolderPickResult.Failure>(
            mappedResult(status = 99),
        )
    }

    @Test
    fun unsupportedOnlyCompletionDefaultNamesUnsupportedSelection() {
        val failure = assertIs<PlatformFolderPickResult.Failure>(
            mappedResult(
                status = IOSLibraryImportStatus.SUCCESS,
                unsupported = 5,
            ),
        )
        assertEquals(
            "No supported audio files were imported",
            failure.message,
        )
    }

    @Test
    fun failedOnlyCompletionDefaultDoesNotClaimFilesWereUnsupported() {
        // A terminal success with nothing imported or duplicated because every
        // copy failed must not reuse the unsupported-only default message.
        val failure = assertIs<PlatformFolderPickResult.Failure>(
            mappedResult(
                status = IOSLibraryImportStatus.SUCCESS,
                unsupported = 2,
                failed = 3,
            ),
        )
        assertEquals(
            "Selected audio files could not be copied",
            failure.message,
        )
    }

    @Test
    fun successCompletionCarriesTerminalCountSummary() {
        val summary = assertNotNull(
            assertIs<PlatformFolderPickResult.Success>(
                mappedResult(
                    status = IOSLibraryImportStatus.SUCCESS,
                    imported = 3,
                    duplicates = 1,
                    unsupported = 2,
                    failed = 1,
                ),
            ).importSummary,
        )
        assertEquals(3, summary.imported)
        assertEquals(1, summary.duplicates)
        assertEquals(2, summary.unsupported)
        assertEquals(1, summary.failed)
    }

    @Test
    fun duplicateOnlySuccessCarriesTerminalCountSummary() {
        val summary = assertNotNull(
            assertIs<PlatformFolderPickResult.Success>(
                mappedResult(
                    status = IOSLibraryImportStatus.SUCCESS,
                    duplicates = 4,
                    unsupported = 1,
                ),
            ).importSummary,
        )
        assertEquals(0, summary.imported)
        assertEquals(4, summary.duplicates)
        assertEquals(1, summary.unsupported)
        assertEquals(0, summary.failed)
    }

    @Test
    fun importSummaryMessageIncludesAllFourCounts() {
        assertEquals(
            "Imported 3, duplicates 1, unsupported 2, failed 1",
            iosImportSummaryMessage(
                LibraryImportSummary(
                    imported = 3,
                    duplicates = 1,
                    unsupported = 2,
                    failed = 1,
                ),
                summaryFormat = "Imported %1\$d, duplicates %2\$d, unsupported %3\$d, failed %4\$d",
            ),
        )
    }

    @Test
    fun statusConstantsAreStableAbiValues() {
        assertEquals(0, IOSLibraryImportStatus.SUCCESS)
        assertEquals(1, IOSLibraryImportStatus.CANCELLED)
        assertEquals(2, IOSLibraryImportStatus.UNAVAILABLE)
        assertEquals(3, IOSLibraryImportStatus.OVERLAP)
        assertEquals(4, IOSLibraryImportStatus.FAILURE)
    }

    @Test
    fun managedFileNameKeepsPlainNamesAndExtensions() {
        assertEquals("Track 01.mp3", managedImportFileName("Track 01.mp3"))
        assertEquals("Live Session (2024).flac",
            managedImportFileName("Live Session (2024).flac"))
        assertEquals("song", managedImportFileName("song"))
    }

    @Test
    fun managedFileNameStripsSeparatorsAndTraversal() {
        assertEquals("song.mp3",
            managedImportFileName("/folder/song.mp3"))
        assertEquals("song.mp3",
            managedImportFileName("..\\folder\\song.mp3"))
        assertEquals("song.mp3",
            managedImportFileName("../../music/song.mp3"))
    }

    @Test
    fun managedFileNameFallsBackForBlankOrTraversalNames() {
        val expected = ManagedImportFallbackFileName
        assertEquals(expected, managedImportFileName(""))
        assertEquals(expected, managedImportFileName("   "))
        assertEquals(expected, managedImportFileName("."))
        assertEquals(expected, managedImportFileName(".."))
        assertEquals(expected, managedImportFileName("folder/"))
        assertEquals(expected, managedImportFileName("folder\\"))
    }

    @Test
    fun freeDestinationNameIsFresh() {
        val plan = managedImportDestinationPlan(
            sourceFileName = "song.mp3",
            sourceContent = "fresh-bytes".encodeToByteArray(),
            managedFiles = emptyMap(),
        )
        assertEquals(ManagedImportDestinationPlan.Fresh("song.mp3"), plan)
    }

    @Test
    fun byteIdenticalDestinationIsDuplicate() {
        val content = "same-bytes".encodeToByteArray()
        val plan = managedImportDestinationPlan(
            sourceFileName = "song.mp3",
            sourceContent = content,
            managedFiles = mapOf("song.mp3" to content),
        )
        assertEquals(ManagedImportDestinationPlan.Duplicate("song.mp3"), plan)
    }

    @Test
    fun byteIdenticalContentUnderAnotherNameIsFresh() {
        val content = "same-bytes".encodeToByteArray()
        val plan = managedImportDestinationPlan(
            sourceFileName = "copy.mp3",
            sourceContent = content,
            managedFiles = mapOf("song.mp3" to content),
        )
        assertEquals(ManagedImportDestinationPlan.Fresh("copy.mp3"), plan)
    }

    @Test
    fun differentContentSelectsDeterministicSuffixedDestination() {
        val managedFiles = mapOf("song.mp3" to "original".encodeToByteArray())
        val sourceContent = "different".encodeToByteArray()
        val expected = ManagedImportDestinationPlan.Suffixed("song-2.mp3")
        assertEquals(
            expected,
            managedImportDestinationPlan(
                sourceFileName = "song.mp3",
                sourceContent = sourceContent,
                managedFiles = managedFiles,
            ),
        )
        // Same managed state and content always choose the same destination.
        assertEquals(
            expected,
            managedImportDestinationPlan(
                sourceFileName = "song.mp3",
                sourceContent = sourceContent,
                managedFiles = managedFiles,
            ),
        )
    }

    @Test
    fun suffixSkipsOccupiedCandidateNamesAndPreservesExtension() {
        val managedFiles =
            mapOf(
                "song.mp3" to "original".encodeToByteArray(),
                "song-2.mp3" to "occupied".encodeToByteArray(),
                "song-3.mp3" to "occupied".encodeToByteArray(),
            )
        assertEquals(
            ManagedImportDestinationPlan.Suffixed("song-4.mp3"),
            managedImportDestinationPlan(
                sourceFileName = "song.mp3",
                sourceContent = "different".encodeToByteArray(),
                managedFiles = managedFiles,
            ),
        )
    }

    @Test
    fun suffixWorksWithoutAnExtension() {
        assertEquals(
            ManagedImportDestinationPlan.Suffixed("podcast-2"),
            managedImportDestinationPlan(
                sourceFileName = "podcast",
                sourceContent = "different".encodeToByteArray(),
                managedFiles =
                    mapOf("podcast" to "original".encodeToByteArray()),
            ),
        )
    }

    @Test
    fun caseDifferingByteIdenticalDestinationIsDuplicate() {
        // APFS destinations are case-insensitive: Song.mp3 and song.mp3 are
        // the same managed file, so identical bytes are a duplicate of the
        // managed file identity.
        val content = "same-bytes".encodeToByteArray()
        val plan = managedImportDestinationPlan(
            sourceFileName = "Song.mp3",
            sourceContent = content,
            managedFiles = mapOf("song.mp3" to content),
        )
        assertEquals(ManagedImportDestinationPlan.Duplicate("song.mp3"), plan)
    }

    @Test
    fun caseDifferingDifferentContentSelectsSuffixedOriginalCasing() {
        // Different bytes under a case-differing name collide on APFS and
        // must receive a suffixed destination that keeps the incoming casing.
        val managedFiles = mapOf("song.mp3" to "original".encodeToByteArray())
        val sourceContent = "different".encodeToByteArray()
        val expected = ManagedImportDestinationPlan.Suffixed("Song-2.mp3")
        assertEquals(
            expected,
            managedImportDestinationPlan(
                sourceFileName = "Song.mp3",
                sourceContent = sourceContent,
                managedFiles = managedFiles,
            ),
        )
        // The chosen suffix must also skip case-differing occupied names.
        assertEquals(
            ManagedImportDestinationPlan.Suffixed("Song-3.mp3"),
            managedImportDestinationPlan(
                sourceFileName = "Song.mp3",
                sourceContent = sourceContent,
                managedFiles =
                    mapOf(
                        "song.mp3" to "original".encodeToByteArray(),
                        "SONG-2.mp3" to "occupied".encodeToByteArray(),
                    ),
            ),
        )
    }
}
