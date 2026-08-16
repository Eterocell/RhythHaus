package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackArtwork
import com.eterocell.rhythhaus.library.TrackUpsertResult
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentLauncher
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentOpenResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentSaveResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuard
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuardResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.createPlaylistBackupController
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

class AppScanCancellationTest {
    @Test
    fun admittedMutationRunsAndReturnsCoordinatorToIdleAfterSuccess() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val orchestrator =
                AppLibraryOrchestrator(coordinator, publishError = {})
            var ran = false

            orchestrator.launch(LibraryOperationKind.Clear) {
                ran = true
            }

            assertTrue(ran)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun failedMutationPublishesThrowableMessageAndReturnsCoordinatorToIdle() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launch(LibraryOperationKind.Clear) {
                error("mutation failed")
            }

            assertEquals(listOf("mutation failed"), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun cancelledMutationRethrowsAndReturnsCoordinatorToIdle() = runBlocking {
        val coordinator = AppLibraryOperationCoordinator {}
        val errors = mutableListOf<String>()
        val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

        assertFailsWith<CancellationException> {
            orchestrator.launch(LibraryOperationKind.Clear) {
                throw CancellationException("cancelled")
            }
        }

        assertEquals(emptyList(), errors)
        assertEquals(LibraryOperationState.Idle, coordinator.state.value)
    }

    @Test
    fun admittedScanRunsThroughOrchestratorAndReturnsCoordinatorToIdle() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val orchestrator =
                AppLibraryOrchestrator(coordinator, publishError = {})
            var ran = false

            orchestrator.launchScan {
                ran = true
            }

            assertTrue(ran)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun scanOperationFailurePublishesThrowableMessageAndReturnsCoordinatorToIdle() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launchScan {
                error("scan post-processing failed")
            }

            assertEquals(listOf("scan post-processing failed"), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun scanReadFailedPlaylistPublicationReachesOrchestratorErrorPath() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launchScan { token ->
                publishScanContentAfterReconcile(
                    reconciler =
                        PlaybackSessionReconciler {
                            PlaybackSessionReconcileResult.Applied
                        },
                    playlistStateOwner =
                        PlaylistStateOwner(
                            FailingPlaylistRepository, Dispatchers.Default),
                    content = LibraryContentState(emptyList(), emptyList()),
                    session = testScanSession(ScanStatus.Completed),
                    ownerIsActive = { true },
                    publish = { publication ->
                        orchestrator.publishIfCurrent(token) {
                            publication.playlists
                                ?.requireSuccessfulPublication()
                        }
                    },
                )
            }

            assertEquals(listOf("playlist_load_failed"), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun mutationReadFailedPlaylistPublicationReachesOrchestratorErrorPath() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launch(LibraryOperationKind.RemoveSource) { token ->
                orchestrator.publishIfCurrent(token) {
                    PlaylistStateAction.ReadFailed("playlist_load_failed")
                        .requireSuccessfulPublication()
                }
            }

            assertEquals(listOf("playlist_load_failed"), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun cancelledScanRethrowsWithoutPublishingErrorAndReturnsCoordinatorToIdle() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            assertFailsWith<CancellationException> {
                orchestrator.launchScan {
                    throw CancellationException("cancelled")
                }
            }

            assertEquals(emptyList(), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun activeMutationRejectsSecondMutationAndStaleTokenCannotPublishAfterReplacement() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val orchestrator =
                AppLibraryOrchestrator(coordinator, publishError = {})
            val started = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            var firstToken: LibraryOperationToken? = null
            var secondRan = false

            coroutineScope {
                val first = async {
                    orchestrator.launch(LibraryOperationKind.Clear) { token ->
                        firstToken = token
                        started.complete(Unit)
                        release.await()
                    }
                }
                started.await()

                orchestrator.launch(LibraryOperationKind.RemoveSource) {
                    secondRan = true
                }
                assertEquals(false, secondRan)

                release.complete(Unit)
                first.await()
            }

            val replacement =
                coordinator.admitMutation(LibraryOperationKind.RemoveSource)
                    as LibraryOperationAdmission.Admitted
            var stalePublished = false

            assertEquals(
                null,
                orchestrator.publishIfCurrent(requireNotNull(firstToken)) {
                    stalePublished = true
                },
            )
            assertEquals(false, stalePublished)

            coordinator.complete(replacement.token)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun repeatedScanAdmissionIsRejectedUntilCurrentOperationCompletes() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}

            val first = coordinator.admitScan()
            assertTrue(first is LibraryOperationAdmission.Admitted)
            assertEquals(
                LibraryOperationAdmission.Rejected, coordinator.admitScan())

            coordinator.complete(
                (first as LibraryOperationAdmission.Admitted).token)
            assertTrue(
                coordinator.admitScan() is LibraryOperationAdmission.Admitted)
        }

    @Test
    fun mutationCancellationRequestAndJoinCompleteBeforeMutationAdmission() =
        runBlocking {
            val events = mutableListOf<String>()
            val coordinator = AppLibraryOperationCoordinator {
                events += "cancel"
                yield()
                events += "joined"
            }
            val scan =
                coordinator.admitScan() as LibraryOperationAdmission.Admitted

            val mutation = coordinator.admitMutation(LibraryOperationKind.Clear)

            assertEquals(listOf("cancel", "joined"), events)
            assertTrue(mutation is LibraryOperationAdmission.Admitted)
            assertEquals(
                LibraryOperationKind.Clear,
                (mutation as LibraryOperationAdmission.Admitted).token.kind)
        }

    @Test
    fun mutationWriteWaitsForCancelledScanTerminalPersistenceBarrier() =
        runBlocking {
            val terminalPersistenceBarrier = CompletableDeferred<Unit>()
            val scanStarted = CompletableDeferred<Unit>()
            val cancellationJoinStarted = CompletableDeferred<Unit>()
            val repository = BarrierRecordingLibraryRepository()
            val coordinator = AppLibraryOperationCoordinator {
                cancellationJoinStarted.complete(Unit)
                terminalPersistenceBarrier.await()
            }
            val orchestrator =
                AppLibraryOrchestrator(coordinator, publishError = {})

            coroutineScope {
                val scan = async {
                    orchestrator.launchScan {
                        scanStarted.complete(Unit)
                        terminalPersistenceBarrier.await()
                    }
                }
                scanStarted.await()
                val mutation = async {
                    orchestrator.launch(LibraryOperationKind.RemoveSource) {
                        token ->
                        removeSourceInBackground(
                            sourceId = "source",
                            repository = repository,
                            platformAccess = EmptyPlatformSourceAccess,
                            reconciler =
                                PlaybackSessionReconciler {
                                    PlaybackSessionReconcileResult.Applied
                                },
                            ioDispatcher = Dispatchers.Default,
                            playlistStateOwner =
                                PlaylistStateOwner(
                                    EmptyPlaylistRepository,
                                    Dispatchers.Default),
                            publish = {
                                orchestrator.publishIfCurrent(token) { Unit }
                            },
                        )
                    }
                }

                cancellationJoinStarted.await()
                assertEquals(false, repository.removeStarted.isCompleted)
                terminalPersistenceBarrier.complete(Unit)
                scan.await()
                mutation.await()
            }

            assertEquals(1, repository.removeCalls)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun mutationPreemptionLetsScannerPersistTerminalStateBeforeAdmission() =
        runBlocking {
            var cancellationRequested = false
            var scannerObservedCancellation = false
            val coordinator = AppLibraryOperationCoordinator {
                cancellationRequested = true
                yield()
                scannerObservedCancellation = cancellationRequested
            }

            coordinator.admitScan()
            val mutation = coordinator.admitMutation(LibraryOperationKind.Clear)

            assertTrue(scannerObservedCancellation)
            assertTrue(mutation is LibraryOperationAdmission.Admitted)
        }

    @Test
    fun staleScanPublicationIsRejectedAfterMutationPreemptsScan() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val scan =
                coordinator.admitScan() as LibraryOperationAdmission.Admitted
            val mutation =
                coordinator.admitMutation(LibraryOperationKind.RemoveSource)
                    as LibraryOperationAdmission.Admitted
            var published = false

            coordinator.publishIfCurrent(scan.token) { published = true }

            assertEquals(false, published)
            coordinator.complete(mutation.token)
        }

    @Test
    fun destructiveMutationsAreRejectedUntilTheCurrentMutationCompletes() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val removeMissing =
                coordinator.admitMutation(
                    LibraryOperationKind.RemoveMissingTracks)
                    as LibraryOperationAdmission.Admitted

            assertEquals(
                LibraryOperationAdmission.Rejected,
                coordinator.admitMutation(LibraryOperationKind.RemoveSource),
            )
            assertEquals(
                LibraryOperationAdmission.Rejected,
                coordinator.admitMutation(LibraryOperationKind.Clear),
            )

            coordinator.complete(removeMissing.token)
            val next =
                coordinator.admitMutation(LibraryOperationKind.RemoveSource)
                    as LibraryOperationAdmission.Admitted
            coordinator.complete(next.token)
        }

    @Test
    fun queuedScanProgressCallbacksPreserveOrderForTerminalPublication() =
        runBlocking {
            val published = mutableListOf<ScanStatus>()
            val callbacks =
                OrderedScanProgressCallbacks(this) { progress ->
                    published += requireNotNull(progress.session).status
                }

            callbacks.offer(ScanProgress(testScanSession(ScanStatus.Scanning)))
            callbacks.offer(ScanProgress(testScanSession(ScanStatus.Completed)))
            callbacks.awaitPublished()

            assertEquals(
                listOf(ScanStatus.Scanning, ScanStatus.Completed), published)
        }

    @Test
    fun terminalScanRestorationRequiresAnExistingSource() {
        val source =
            com.eterocell.rhythhaus.library.LibrarySource(
                id = "source",
                platformKind =
                    com.eterocell.rhythhaus.library.LibraryPlatformKind
                        .JvmFolder,
                displayName = "Music",
                handle = "/music",
                createdAtEpochMillis = 1L,
            )
        val completed = testScanSession(ScanStatus.Completed)

        assertEquals(
            completed,
            restoredTerminalScanProgress(completed, listOf(source))?.session)
        assertEquals(null, restoredTerminalScanProgress(completed, emptyList()))
        assertEquals(
            null,
            restoredTerminalScanProgress(
                testScanSession(ScanStatus.Cancelling), listOf(source)),
        )
    }

    @Test
    fun authoritativeLibraryRevisionAdvancesExactlyOncePerAcceptedPublication() {
        val owner = AuthoritativeLibraryPublicationOwner()
        val first = LibraryContentState(emptyList(), listOf(testTrack("one")))
        val second = LibraryContentState(emptyList(), listOf(testTrack("two")))

        runBlocking {
            assertEquals(0, owner.revision)
            assertEquals(1, owner.publish(first).revision)
            assertEquals(1, owner.revision)
            assertEquals(2, owner.publish(second).revision)
            assertEquals(2, owner.revision)
        }
    }

    @Test
    fun authoritativePublicationCannotInterleaveBetweenRevisionCheckAndMutation() =
        runBlocking {
            val owner = AuthoritativeLibraryPublicationOwner()
            owner.publish(
                LibraryContentState(emptyList(), listOf(testTrack("one"))))
            val mutationStarted = CompletableDeferred<Unit>()
            val releaseMutation = CompletableDeferred<Unit>()
            var mutationCalls = 0

            coroutineScope {
                val mutation = async {
                    owner.withCurrentRevision(expectedRevision = 1) {
                        mutationCalls++
                        mutationStarted.complete(Unit)
                        releaseMutation.await()
                        "mutated"
                    }
                }
                mutationStarted.await()
                val publication = launch {
                    owner.publish(
                        LibraryContentState(
                            emptyList(), listOf(testTrack("two"))))
                }
                assertEquals(false, publication.isCompleted)
                releaseMutation.complete(Unit)
                assertEquals(
                    AuthoritativeRevisionResult.Current("mutated"),
                    mutation.await())
                publication.join()
            }

            assertEquals(1, mutationCalls)
            assertEquals(2, owner.revision)
            assertEquals(
                AuthoritativeRevisionResult.Stale,
                owner.withCurrentRevision(1) { error("must not mutate") })
        }

    @Test
    fun backupOrchestrationPublishesIdleRetainedPreviewBeforeRethrowingCancellation() =
        runBlocking {
            val launcher = RecordingPlaylistBackupDocumentLauncher()
            val controller =
                createPlaylistBackupController(
                    owner =
                        PlaylistStateOwner(
                            EmptyPlaylistRepository, Dispatchers.Default),
                    dispatcher = Dispatchers.Default,
                    launcher = launcher,
                    revisionGuard =
                        object : PlaylistBackupRevisionGuard {
                            override suspend fun <T> withCurrentRevision(
                                expectedRevision: Long,
                                block: suspend () -> T,
                            ): PlaylistBackupRevisionGuardResult<T> =
                                PlaylistBackupRevisionGuardResult.Current(
                                    block())
                        },
                )
            val saving =
                controller.beginExport(
                    state = PlaylistBackupUiState(),
                    snapshot = PlaylistSnapshot(),
                    authoritativeTracks = emptyList(),
                    exportedAtEpochMillis = 0L,
                )
            val opening =
                controller.beginOpen(
                    controller.receiveSave(
                        saving,
                        PlaylistBackupDocumentSaveResult.Success,
                    ),
                )
            val preview =
                checkNotNull(
                    controller
                        .receiveOpen(
                            state = opening,
                            result =
                                PlaylistBackupDocumentOpenResult.Success(
                                    checkNotNull(launcher.savedBytes)),
                            destinationTracks = emptyList(),
                            existingPlaylistNames = emptyList(),
                            importedSuffix = " imported",
                            libraryRevision = 1L,
                        )
                        .preview,
                )
            val states =
                mutableListOf(
                    PlaylistBackupUiState(
                        operation = PlaylistBackupOperation.Importing,
                        preview = preview,
                    ),
                )

            val cancellation = CancellationException("gone")
            val thrown =
                assertFailsWith<CancellationException> {
                    runPlaylistBackupOperation(
                        currentState = { states.last() },
                        publishState = { state -> states.add(state) },
                        reduce = controller::reduce,
                    ) {
                        throw cancellation
                    }
                }

            assertSame(cancellation, thrown)
            assertEquals(PlaylistBackupOperation.Idle, states.last().operation)
            assertSame(preview, states.last().preview)
        }

    @Test
    fun requestScanCancellationMarksActiveScanAsCancellingImmediately() {
        val progress =
            ScanProgress(
                session =
                    ScanSession(
                        id = "scan-1",
                        sourceId = "source-1",
                        status = ScanStatus.Scanning,
                        startedAtEpochMillis = 1L,
                        filesVisited = 42,
                    ),
            )

        val result = progress.requestScanCancellation()

        assertEquals(ScanStatus.Cancelling, result?.session?.status)
        assertEquals(42, result?.session?.filesVisited)
    }

    @Test
    fun requestScanCancellationLeavesTerminalScanUntouched() {
        val progress =
            ScanProgress(
                session =
                    ScanSession(
                        id = "scan-1",
                        sourceId = "source-1",
                        status = ScanStatus.Completed,
                        startedAtEpochMillis = 1L,
                    ),
            )

        val result = progress.requestScanCancellation()

        assertSame(progress, result)
    }

    @Test
    fun initialLibraryAvailabilityRestoresBeforePublishing() = runBlocking {
        val events = mutableListOf<String>()
        val content =
            LibraryContentState(sources = emptyList(), tracks = emptyList())

        publishInitialLibraryContent(
            lifecycle = PlaybackSessionRestorer { events += "restore" },
            reconciler =
                PlaybackSessionReconciler {
                    events += "reconcile"
                    PlaybackSessionReconcileResult.Applied
                },
            content = content,
            updateState = { events += "publish" },
        )

        assertEquals(listOf("restore", "reconcile", "publish"), events)
    }

    @Test
    fun playbackMutationsStayDisabledUntilInitialRestoreCompletes() =
        runBlocking {
            val restoreStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
            val allowRestore = kotlinx.coroutines.CompletableDeferred<Unit>()
            val publication = kotlinx.coroutines.CompletableDeferred<Unit>()

            coroutineScope {
                val job = async {
                    publishInitialLibraryContent(
                        lifecycle =
                            PlaybackSessionRestorer {
                                restoreStarted.complete(Unit)
                                allowRestore.await()
                            },
                        reconciler =
                            PlaybackSessionReconciler {
                                PlaybackSessionReconcileResult.Applied
                            },
                        content = LibraryContentState(emptyList(), emptyList()),
                        updateState = { publication.complete(Unit) },
                    )
                }

                restoreStarted.await()
                assertEquals(false, publication.isCompleted)
                allowRestore.complete(Unit)
                job.await()
                assertEquals(true, publication.isCompleted)
            }
        }

    @Test
    fun initialPublicationPolicyBlocksMutationsUntilTerminalOutcome() {
        val content = LibraryContentState(emptyList(), emptyList())
        val pending = InitialLibraryPublicationState()

        assertEquals(false, pending.isReady)
        assertEquals(false, pending.mutationsAllowed)
        assertEquals(null, pending.content)

        val succeeded = pending.complete(content)
        assertEquals(true, succeeded.isReady)
        assertEquals(true, succeeded.mutationsAllowed)
        assertSame(content, succeeded.content)
        assertEquals(null, succeeded.errorMessage)

        val failed =
            pending.failSafe(content, IllegalStateException("restore failed"))
        assertEquals(true, failed.isReady)
        assertEquals(true, failed.mutationsAllowed)
        assertSame(content, failed.content)
        assertEquals("restore failed", failed.errorMessage)
    }

    @Test
    fun restoreFailurePublishesAuthoritativeContentOnceAndReleasesGates() =
        runBlocking {
            val content =
                LibraryContentState(emptyList(), listOf(testTrack("one")))
            val states = mutableListOf<InitialLibraryPublicationState>()

            publishInitialLibraryContent(
                lifecycle =
                    PlaybackSessionRestorer {
                        throw IllegalStateException("restore failed")
                    },
                reconciler =
                    PlaybackSessionReconciler { error("must not reconcile") },
                content = content,
                updateState = states::add,
            )

            assertEquals(1, states.size)
            assertEquals(true, states.single().isReady)
            assertEquals(true, states.single().mutationsAllowed)
            assertSame(content, states.single().content)
            assertEquals("restore failed", states.single().errorMessage)
        }

    @Test
    fun reconcileFailureAfterRestorePublishesAuthoritativeContentOnceAndError() =
        runBlocking {
            val content =
                LibraryContentState(emptyList(), listOf(testTrack("one")))
            val states = mutableListOf<InitialLibraryPublicationState>()

            publishInitialLibraryContent(
                lifecycle = PlaybackSessionRestorer {},
                reconciler =
                    PlaybackSessionReconciler {
                        throw IllegalStateException("reconcile failed")
                    },
                content = content,
                updateState = states::add,
            )

            assertEquals(1, states.size)
            assertSame(content, states.single().content)
            assertEquals("reconcile failed", states.single().errorMessage)
        }

    @Test
    fun initialPublicationCancellationDoesNotPublishErrorState() = runBlocking {
        val states = mutableListOf<InitialLibraryPublicationState>()

        assertFailsWith<CancellationException> {
            publishInitialLibraryContent(
                lifecycle =
                    PlaybackSessionRestorer {
                        throw CancellationException("gone")
                    },
                reconciler =
                    PlaybackSessionReconciler {
                        PlaybackSessionReconcileResult.Applied
                    },
                content = LibraryContentState(emptyList(), emptyList()),
                updateState = states::add,
            )
        }

        assertEquals(emptyList(), states)
    }

    @Test
    fun scanReconcileFailureRethrowsForOrchestratorErrorConversion() =
        runBlocking {
            val content =
                LibraryContentState(emptyList(), listOf(testTrack("one")))
            val session = testScanSession(ScanStatus.Completed)
            val publications = mutableListOf<ScanPublicationState>()

            assertFailsWith<IllegalStateException> {
                publishScanContentAfterReconcile(
                    reconciler =
                        PlaybackSessionReconciler {
                            throw IllegalStateException("scan reconcile failed")
                        },
                    playlistStateOwner =
                        PlaylistStateOwner(
                            EmptyPlaylistRepository, Dispatchers.Default),
                    content = content,
                    session = session,
                    ownerIsActive = { true },
                    publish = publications::add,
                )
            }

            assertEquals(emptyList(), publications)
        }

    @Test
    fun scanCancellationAfterMutationCleansActiveProgressThenRethrows() =
        runBlocking {
            val content =
                LibraryContentState(emptyList(), listOf(testTrack("one")))
            val publications = mutableListOf<ScanPublicationState>()

            assertFailsWith<CancellationException> {
                publishScanContentAfterReconcile(
                    reconciler =
                        PlaybackSessionReconciler {
                            throw CancellationException("cancelled")
                        },
                    playlistStateOwner =
                        PlaylistStateOwner(
                            EmptyPlaylistRepository, Dispatchers.Default),
                    content = content,
                    session = testScanSession(ScanStatus.Scanning),
                    ownerIsActive = { true },
                    publish = publications::add,
                )
            }

            assertEquals(1, publications.size)
            assertSame(content, publications.single().content)
            assertEquals(
                ScanStatus.Cancelled,
                publications.single().progress.session?.status)
            assertEquals(false, publications.single().progress.isActive)
            assertEquals(null, publications.single().errorMessage)
        }

    @Test
    fun scanCancellationWithGoneOwnerDoesNotPublishCleanup() = runBlocking {
        val publications = mutableListOf<ScanPublicationState>()

        assertFailsWith<CancellationException> {
            publishScanContentAfterReconcile(
                reconciler =
                    PlaybackSessionReconciler {
                        throw CancellationException("cancelled")
                    },
                playlistStateOwner =
                    PlaylistStateOwner(
                        EmptyPlaylistRepository, Dispatchers.Default),
                content = LibraryContentState(emptyList(), emptyList()),
                session = testScanSession(ScanStatus.Scanning),
                ownerIsActive = { false },
                publish = publications::add,
            )
        }

        assertEquals(emptyList(), publications)
    }

    @Test
    fun scanCancellationPreservesOriginalCancellationWhenLoadingErrorsFails() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)
            val cancellation = CancellationException("original cancellation")
            val publications = mutableListOf<ScanPublicationState>()

            val thrown =
                assertFailsWith<CancellationException> {
                    orchestrator.launchScan { token ->
                        publishScanContentAfterReconcile(
                            reconciler =
                                PlaybackSessionReconciler {
                                    throw cancellation
                                },
                            playlistStateOwner =
                                PlaylistStateOwner(
                                    FailingPlaylistRepository,
                                    Dispatchers.Default),
                            content =
                                LibraryContentState(emptyList(), emptyList()),
                            session = testScanSession(ScanStatus.Scanning),
                            loadScanErrors = { error("error lookup failed") },
                            ownerIsActive = { true },
                            publish = { publication ->
                                publications += publication
                                orchestrator.publishIfCurrent(token) { Unit }
                            },
                        )
                    }
                }

            assertSame(cancellation, thrown)
            assertEquals(emptyList(), errors)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
            assertEquals(1, publications.size)
            assertEquals(emptyList(), publications.single().scanErrors)
            assertEquals(
                ScanStatus.Cancelled,
                publications.single().progress.session?.status)
        }

    @Test
    fun scanPublicationLoadsErrorsAsPartOfTerminalPublication() = runBlocking {
        val errors = listOf(testScanError())
        val publications = mutableListOf<ScanPublicationState>()

        publishScanContentAfterReconcile(
            reconciler =
                PlaybackSessionReconciler {
                    PlaybackSessionReconcileResult.Applied
                },
            playlistStateOwner =
                PlaylistStateOwner(
                    EmptyPlaylistRepository, Dispatchers.Default),
            content = LibraryContentState(emptyList(), emptyList()),
            session = testScanSession(ScanStatus.Completed),
            loadScanErrors = { errors },
            ownerIsActive = { true },
            publish = publications::add,
        )

        assertEquals(errors, publications.single().scanErrors)
    }

    @Test
    fun completedScanErrorLookupFailurePublishesOneConvertedErrorWithoutTerminalPublication() =
        runBlocking {
            val coordinator = AppLibraryOperationCoordinator {}
            val errors = mutableListOf<String>()
            val publications = mutableListOf<ScanPublicationState>()
            val orchestrator = AppLibraryOrchestrator(coordinator, errors::add)

            orchestrator.launchScan { token ->
                publishScanContentAfterReconcile(
                    reconciler =
                        PlaybackSessionReconciler {
                            PlaybackSessionReconcileResult.Applied
                        },
                    playlistStateOwner =
                        PlaylistStateOwner(
                            EmptyPlaylistRepository, Dispatchers.Default),
                    content = LibraryContentState(emptyList(), emptyList()),
                    session = testScanSession(ScanStatus.Completed),
                    loadScanErrors = { error("scan error lookup failed") },
                    ownerIsActive = { true },
                    publish = { publication ->
                        publications += publication
                        orchestrator.publishIfCurrent(token) { Unit }
                    },
                )
            }

            assertEquals(listOf("scan error lookup failed"), errors)
            assertEquals(emptyList(), publications)
            assertEquals(LibraryOperationState.Idle, coordinator.state.value)
        }

    @Test
    fun rejectedRemoveMissingTracksUsesDeterministicErrorMessage() {
        assertEquals(
            "Unable to remove missing tracks: StaleCompletedScan",
            removeMissingTracksRejectionMessage(
                com.eterocell.rhythhaus.library
                    .RemoveMissingTracksRejectionReason
                    .StaleCompletedScan,
            ),
        )
    }

    @Test
    fun removeMissingTracksCallbackBoundaryRetainsRejectedErrorsAndClearsAcceptedErrors() =
        runBlocking {
            val existingErrors = listOf(testScanError())
            var scanErrors = existingErrors
            val publish = { publication: LibraryMutationPublication ->
                scanErrors = resolveMutationScanErrors(scanErrors, publication)
            }

            val rejectedPublication =
                removeMissingTracksPublication(
                    result =
                        com.eterocell.rhythhaus.library
                            .RemoveMissingTracksResult
                            .Rejected(
                                com.eterocell.rhythhaus.library
                                    .RemoveMissingTracksRejectionReason
                                    .StaleCompletedScan,
                            ),
                    content =
                        LibraryContentState(
                            emptyList(), listOf(testTrack("existing"))),
                )

            assertEquals(null, rejectedPublication.content)
            assertEquals(null, rejectedPublication.playlists)
            assertEquals(
                "Unable to remove missing tracks: StaleCompletedScan",
                rejectedPublication.errorMessage,
            )
            publish(rejectedPublication)
            assertEquals(existingErrors, scanErrors)

            val acceptedPublication =
                removeMissingTracksPublication(
                    result =
                        com.eterocell.rhythhaus.library
                            .RemoveMissingTracksResult
                            .Removed(count = 1),
                    content = LibraryContentState(emptyList(), emptyList()),
                )
            publish(acceptedPublication)
            assertEquals(emptyList(), scanErrors)
        }

    @Test
    fun restoredTerminalScanStateLoadsErrorsFromTheRestoredSession() {
        val session = testScanSession(ScanStatus.Completed)
        val errors = listOf(testScanError())

        val restored =
            restoredTerminalScanState(session, listOf(testSource()), errors)

        assertEquals(session, restored.progress?.session)
        assertEquals(errors, restored.errors)
    }

    @Test
    fun restoredTerminalScanStateSuppressesErrorsWhenProgressCannotBeRestored() {
        val errors = listOf(testScanError())

        assertEquals(
            RestoredTerminalScanState(progress = null, errors = emptyList()),
            restoredTerminalScanState(
                session = testScanSession(ScanStatus.Completed),
                sources = emptyList(),
                errors = errors,
            ),
        )
        assertEquals(
            RestoredTerminalScanState(progress = null, errors = emptyList()),
            restoredTerminalScanState(
                session = testScanSession(ScanStatus.Scanning),
                sources = listOf(testSource()),
                errors = errors,
            ),
        )
        assertEquals(
            RestoredTerminalScanState(progress = null, errors = emptyList()),
            restoredTerminalScanState(
                session = testScanSession(ScanStatus.Cancelling),
                sources = listOf(testSource()),
                errors = errors,
            ),
        )
    }
}

private object EmptyPlaylistRepository : PlaylistRepository {
    override fun playlists(): List<PlaylistSummary> = emptyList()

    override fun playlist(id: String): PlaylistSummary? = null

    override fun entries(playlistId: String): List<PlaylistEntry> = emptyList()

    override fun create(name: String): PlaylistSummary =
        error("Not used by this test")

    override fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): PlaylistSummary = error("Not used by this test")

    override fun importPlaylists(
        playlists: List<PlaylistImportMutation>
    ): List<PlaylistSummary> = error("Not used by this test")

    override fun rename(id: String, name: String) =
        error("Not used by this test")

    override fun delete(id: String) = error("Not used by this test")

    override fun append(playlistId: String, trackIds: List<String>) =
        error("Not used by this test")

    override fun removeEntry(entryId: String) = error("Not used by this test")

    override fun reorder(playlistId: String, entryIds: List<String>) =
        error("Not used by this test")
}

private object FailingPlaylistRepository : PlaylistRepository {
    override fun playlists(): List<PlaylistSummary> =
        error("playlist read failed")

    override fun playlist(id: String): PlaylistSummary? =
        error("Not used by this test")

    override fun entries(playlistId: String): List<PlaylistEntry> =
        error("Not used by this test")

    override fun create(name: String): PlaylistSummary =
        error("Not used by this test")

    override fun createWithEntries(
        name: String,
        trackIds: List<String>
    ): PlaylistSummary = error("Not used by this test")

    override fun importPlaylists(
        playlists: List<PlaylistImportMutation>
    ): List<PlaylistSummary> = error("Not used by this test")

    override fun rename(id: String, name: String) =
        error("Not used by this test")

    override fun delete(id: String) = error("Not used by this test")

    override fun append(playlistId: String, trackIds: List<String>) =
        error("Not used by this test")

    override fun removeEntry(entryId: String) = error("Not used by this test")

    override fun reorder(playlistId: String, entryIds: List<String>) =
        error("Not used by this test")
}

private class BarrierRecordingLibraryRepository : LibraryRepository {
    private var sources = listOf(testSource())
    val removeStarted = CompletableDeferred<Unit>()
    var removeCalls = 0
        private set

    override fun upsertSource(source: LibrarySource) = Unit

    override fun sources(): List<LibrarySource> = sources

    override fun upsertTrack(track: LibraryTrack): TrackUpsertResult =
        error("Not used by this test")

    override fun tracks(): List<LibraryTrack> = emptyList()

    override fun tracksForSource(sourceId: String): List<LibraryTrack> =
        emptyList()

    override fun artworkForTrack(trackId: String): TrackArtwork? = null

    override fun insertScanSession(session: ScanSession) = Unit

    override fun updateScanSession(session: ScanSession) = Unit

    override fun insertScanError(
        error: com.eterocell.rhythhaus.library.ScanError
    ) = Unit

    override fun scanErrors(
        scanId: String
    ): List<com.eterocell.rhythhaus.library.ScanError> = emptyList()

    override fun removeMissingTracks(
        sourceId: String,
        requestedScanId: String,
    ): RemoveMissingTracksResult = RemoveMissingTracksResult.Removed(0)

    override fun latestTerminalScanSession(): ScanSession? = null

    override fun removeSource(sourceId: String) {
        removeCalls++
        removeStarted.complete(Unit)
        sources = sources.filterNot { it.id == sourceId }
    }

    override fun clearAll() = Unit
}

private object EmptyPlatformSourceAccess : PlatformSourceAccess {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private class RecordingPlaylistBackupDocumentLauncher :
    PlaylistBackupDocumentLauncher {
    override val isAvailable: Boolean = true
    var savedBytes: ByteArray? = null

    override fun save(suggestedFileName: String, bytes: ByteArray) {
        savedBytes = bytes
    }

    override fun open() = Unit
}

private fun testScanSession(status: ScanStatus) =
    ScanSession(
        id = "scan",
        sourceId = "source",
        status = status,
        startedAtEpochMillis = 1L,
    )

private fun testSource() =
    com.eterocell.rhythhaus.library.LibrarySource(
        id = "source",
        platformKind =
            com.eterocell.rhythhaus.library.LibraryPlatformKind.JvmFolder,
        displayName = "Music",
        handle = "/music",
        createdAtEpochMillis = 1L,
    )

private fun testTrack(id: String) =
    LibraryTrack(
        id = id,
        sourceId = "source",
        sourceLocalKey = "$id.mp3",
        audioSource = AudioSource.FilePath("/$id.mp3"),
        displayName = "$id.mp3",
        title = id,
        artist = "Artist",
        album = "Album",
        durationMillis = null,
        sizeBytes = null,
        modifiedAtEpochMillis = null,
        lastSeenScanId = null,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

private fun testScanError() =
    com.eterocell.rhythhaus.library.ScanError(
        id = "error",
        scanId = "scan",
        sourceLocalKey = "missing.mp3",
        displayPath = "/missing.mp3",
        reason = "missing",
        recoverable = true,
        createdAtEpochMillis = 1L,
    )
