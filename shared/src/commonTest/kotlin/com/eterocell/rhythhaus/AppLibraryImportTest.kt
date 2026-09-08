package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryImportSummary
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * App-boundary contract tests for the iOS Files import terminal handling.
 *
 * These cover the pure App-owned picker orchestration: a terminal picker
 * result resolves into at most one source-scan request and one transient
 * message, import is kept sequential with the follow-up scan, and the iOS
 * import-active window folds into the existing source-mutation gate.
 */
class AppLibraryImportTest {
    private val managedFolder = "/managed/RhythHaus Music"

    private val pickedIosSource =
        LibrarySource(
            id = "ios-app-local",
            platformKind = LibraryPlatformKind.IosAppLocal,
            displayName = "RhythHaus",
            handle = managedFolder,
            createdAtEpochMillis = 0L,
        )

    private fun summary(
        imported: Int = 0,
        duplicates: Int = 0,
        unsupported: Int = 0,
        failed: Int = 0,
    ) = LibraryImportSummary(
        imported = imported,
        duplicates = duplicates,
        unsupported = unsupported,
        failed = failed,
    )

    @Test
    fun successfulImportTerminalRequestsOneScanAndCountSummary() {
        val action =
            resolveLibraryPickerTerminal(
                result =
                    PlatformFolderPickResult.Success(
                        pickedIosSource,
                        summary(
                            imported = 3,
                            duplicates = 1,
                            unsupported = 2,
                            failed = 1,
                        ),
                    ),
                existingSources = emptyList(),
            )

        // Exactly one scan request for the normalized app-local source.
        assertEquals(pickedIosSource, action.scanSource)
        val message = assertNotNull(action.message)
        assertTrue(message.contains("imported 3"))
        assertTrue(message.contains("duplicates 1"))
        assertTrue(message.contains("unsupported 2"))
        assertTrue(message.contains("failed 1"))
    }

    @Test
    fun duplicateOnlySuccessTerminalStillRequestsScanWithCounts() {
        val action =
            resolveLibraryPickerTerminal(
                result =
                    PlatformFolderPickResult.Success(
                        pickedIosSource,
                        summary(duplicates = 4, unsupported = 1),
                    ),
                existingSources = emptyList(),
            )

        assertEquals(pickedIosSource, action.scanSource)
        val message = assertNotNull(action.message)
        assertTrue(message.contains("duplicates 4"))
        assertTrue(message.contains("unsupported 1"))
    }

    @Test
    fun plainFolderPickSuccessWithoutSummaryKeepsLegacyBehavior() {
        // Android/JVM folder picks carry no import summary: no transient
        // message is published and the scan request is unchanged.
        val action =
            resolveLibraryPickerTerminal(
                result = PlatformFolderPickResult.Success(pickedIosSource),
                existingSources = emptyList(),
            )

        assertEquals(pickedIosSource, action.scanSource)
        assertNull(action.message)
    }

    @Test
    fun cancelledTerminalRequestsNoScanAndStaysSilent() {
        val action =
            resolveLibraryPickerTerminal(
                result = PlatformFolderPickResult.Cancelled,
                existingSources = emptyList(),
            )

        assertNull(action.scanSource)
        assertNull(action.message)
    }

    @Test
    fun failedTerminalRequestsNoScanAndPublishesFailureMessage() {
        val action =
            resolveLibraryPickerTerminal(
                result =
                    PlatformFolderPickResult.Failure(
                        message = "Copy failed",
                    ),
                existingSources = emptyList(),
            )

        assertNull(action.scanSource)
        assertEquals("Copy failed", action.message)
    }

    @Test
    fun unavailableTerminalRequestsNoScanAndPublishesUnavailableMessage() {
        val action =
            resolveLibraryPickerTerminal(
                result =
                    PlatformFolderPickResult.Unavailable(
                        message = "Files picker is unavailable",
                    ),
                existingSources = emptyList(),
            )

        assertNull(action.scanSource)
        assertEquals("Files picker is unavailable", action.message)
    }

    @Test
    fun successScanTargetNormalizesToExistingMatchingSource() {
        val existing =
            LibrarySource(
                id = "ios-app-local",
                platformKind = LibraryPlatformKind.IosAppLocal,
                displayName = "RhythHaus",
                handle = managedFolder,
                createdAtEpochMillis = 42L,
            )

        val action =
            resolveLibraryPickerTerminal(
                result =
                    PlatformFolderPickResult.Success(
                        pickedIosSource,
                        summary(imported = 2),
                    ),
                existingSources = listOf(existing),
            )

        val scanSource = assertNotNull(action.scanSource)
        assertEquals(existing.id, scanSource.id)
        assertEquals(42L, scanSource.createdAtEpochMillis)
    }

    @Test
    fun activeImportDisablesMutationsThroughExistingGate() {
        assertFalse(
            appLibraryMutationsEnabled(
                publicationMutationsAllowed = true,
                coordinatorIdle = true,
                importActive = true,
            ),
        )
    }

    @Test
    fun importActiveKeepsOtherGatingContributionsIntact() {
        assertFalse(
            appLibraryMutationsEnabled(
                publicationMutationsAllowed = false,
                coordinatorIdle = true,
                importActive = false,
            ),
        )
        assertFalse(
            appLibraryMutationsEnabled(
                publicationMutationsAllowed = true,
                coordinatorIdle = false,
                importActive = false,
            ),
        )
        assertTrue(
            appLibraryMutationsEnabled(
                publicationMutationsAllowed = true,
                coordinatorIdle = true,
                importActive = false,
            ),
        )
    }
}
