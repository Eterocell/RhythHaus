package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryImportSummary
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.registerMissingDefaultLibrarySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * App-boundary contract tests for the iOS Files import terminal handling.
 *
 * These cover the pure App-owned picker orchestration: a terminal picker result
 * resolves into at most one source-scan request and one transient message,
 * import is kept sequential with the follow-up scan, and the iOS import-active
 * window folds into the existing source-mutation gate.
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
    ) =
        LibraryImportSummary(
            imported = imported,
            duplicates = duplicates,
            unsupported = unsupported,
            failed = failed,
        )

    @Test
    fun missingDefaultSourceIsPersistedAndReturnedForInitialScan() {
        val repository = InMemoryLibraryRepository()

        val created =
            registerMissingDefaultLibrarySource(
                repository = repository,
                defaultSource = pickedIosSource,
            )

        assertEquals(pickedIosSource, created)
        assertEquals(listOf(pickedIosSource), repository.sources())
    }

    @Test
    fun existingDefaultSourceIsPreservedWithoutCreatingAnotherSource() {
        val existing = pickedIosSource.copy(createdAtEpochMillis = 42L)
        val repository =
            InMemoryLibraryRepository().apply { upsertSource(existing) }

        val created =
            registerMissingDefaultLibrarySource(
                repository = repository,
                defaultSource = pickedIosSource,
            )

        assertNull(created)
        assertEquals(listOf(existing), repository.sources())
    }

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
                importSummaryFormat = "失败 %4\$d；不支持 %3\$d；重复 %2\$d；已导入 %1\$d",
            )

        // Exactly one scan request for the normalized app-local source.
        assertEquals(pickedIosSource, action.scanSource)
        val message = assertNotNull(action.message)
        assertEquals("失败 1；不支持 2；重复 1；已导入 3", message)
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

    @Test
    fun successfulImportKeepsMutationsExcludedUntilFollowUpScanIsAdmitted() =
        runBlocking {
            // The terminal callback may clear the platform import flag before
            // the
            // asynchronous follow-up scan gets admitted. The pure gate must
            // retain
            // the exclusion during that handoff window.
            assertFalse(
                appLibraryMutationsEnabled(
                    publicationMutationsAllowed = true,
                    coordinatorIdle = true,
                    importActive = false,
                    followUpScanPending = true,
                ),
            )
            val coordinator = AppLibraryOperationCoordinator {}
            val admission = coordinator.admitScan()
            assertTrue(admission is LibraryOperationAdmission.Admitted)
            assertFalse(
                appLibraryMutationsEnabled(
                    publicationMutationsAllowed = true,
                    coordinatorIdle =
                        coordinator.state.value is LibraryOperationState.Idle,
                    importActive = false,
                    followUpScanPending = false,
                ),
            )
            coordinator.complete(admission.token)
            assertTrue(
                appLibraryMutationsEnabled(
                    publicationMutationsAllowed = true,
                    coordinatorIdle = true,
                    importActive = false,
                    followUpScanPending = false,
                ),
            )
        }

    @Test
    fun cancelledFollowUpScanReleasesPendingGateAndStaysSilent() = runBlocking {
        // An admitted follow-up scan that is cancelled makes the coordinator
        // complete its token and the launch then rethrows the cancellation.
        // The pending exclusion must still be released (non-cancellable
        // finally), the coordinator must end idle, and the cancellation must
        // propagate silently instead of being retried or published.
        val coordinator = AppLibraryOperationCoordinator {}
        val orchestrator =
            AppLibraryOrchestrator(coordinator) { message ->
                error(
                    "a cancelled follow-up scan must stay silent, got: $message")
            }
        var pending = true
        val job =
            launch(Dispatchers.Default) {
                settleFollowUpScanPending(
                    scan = {
                        orchestrator.launchScan { _ ->
                            throw CancellationException(
                                "follow-up scan cancelled")
                        }
                    },
                    releasePending = { pending = false },
                )
            }
        job.join()

        assertTrue(job.isCancelled, "the cancellation must keep propagating")
        assertFalse(
            pending,
            "the pending gate must be released even after cancellation")
        assertTrue(
            coordinator.state.value is LibraryOperationState.Idle,
            "the coordinator must complete the cancelled scan before release",
        )
        assertTrue(
            appLibraryMutationsEnabled(
                publicationMutationsAllowed = true,
                coordinatorIdle =
                    coordinator.state.value is LibraryOperationState.Idle,
                importActive = false,
                followUpScanPending = pending,
            ),
        )
    }
}
