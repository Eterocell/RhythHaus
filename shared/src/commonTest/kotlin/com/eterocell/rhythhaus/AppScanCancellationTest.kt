package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.session.PlaybackSessionReconcileResult
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class AppScanCancellationTest {
    @Test
    fun requestScanCancellationMarksActiveScanAsCancellingImmediately() {
        val progress = ScanProgress(
            session = ScanSession(
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
        val progress = ScanProgress(
            session = ScanSession(
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
        val content = LibraryContentState(sources = emptyList(), tracks = emptyList())

        publishInitialLibraryContent(
            lifecycle = PlaybackSessionRestorer { events += "restore" },
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            content = content,
            updateState = { events += "publish" },
        )

        assertEquals(listOf("restore", "reconcile", "publish"), events)
    }

    @Test
    fun playbackMutationsStayDisabledUntilInitialRestoreCompletes() = runBlocking {
        val restoreStarted = kotlinx.coroutines.CompletableDeferred<Unit>()
        val allowRestore = kotlinx.coroutines.CompletableDeferred<Unit>()
        val publication = kotlinx.coroutines.CompletableDeferred<Unit>()

        coroutineScope {
            val job = async {
                publishInitialLibraryContent(
                    lifecycle = PlaybackSessionRestorer {
                        restoreStarted.complete(Unit)
                        allowRestore.await()
                    },
                    reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
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
    fun scanRefreshPublishesAfterReconcileCompletes() = runBlocking {
        val events = mutableListOf<String>()

        publishLibraryContentAfterReconcile(
            reconciler = PlaybackSessionReconciler {
                events += "reconcile"
                PlaybackSessionReconcileResult.Applied
            },
            content = LibraryContentState(emptyList(), emptyList()),
            updateLibrary = { events += "publish" },
        )

        assertEquals(listOf("reconcile", "publish"), events)
    }

    @Test
    fun failedSafeAppliedContentStillPublishes() = runBlocking {
        var published = false

        publishLibraryContentAfterReconcile(
            reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.FailedSafeApplied },
            content = LibraryContentState(emptyList(), emptyList()),
            updateLibrary = { published = true },
        )

        assertEquals(true, published)
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

        val failed = pending.failSafe(content, IllegalStateException("restore failed"))
        assertEquals(true, failed.isReady)
        assertEquals(true, failed.mutationsAllowed)
        assertSame(content, failed.content)
        assertEquals("restore failed", failed.errorMessage)
    }

    @Test
    fun restoreFailurePublishesAuthoritativeContentOnceAndReleasesGates() = runBlocking {
        val content = LibraryContentState(emptyList(), listOf(testTrack("one")))
        val states = mutableListOf<InitialLibraryPublicationState>()

        publishInitialLibraryContent(
            lifecycle = PlaybackSessionRestorer { throw IllegalStateException("restore failed") },
            reconciler = PlaybackSessionReconciler { error("must not reconcile") },
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
    fun reconcileFailureAfterRestorePublishesAuthoritativeContentOnceAndError() = runBlocking {
        val content = LibraryContentState(emptyList(), listOf(testTrack("one")))
        val states = mutableListOf<InitialLibraryPublicationState>()

        publishInitialLibraryContent(
            lifecycle = PlaybackSessionRestorer { },
            reconciler = PlaybackSessionReconciler { throw IllegalStateException("reconcile failed") },
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
                lifecycle = PlaybackSessionRestorer { throw CancellationException("gone") },
                reconciler = PlaybackSessionReconciler { PlaybackSessionReconcileResult.Applied },
                content = LibraryContentState(emptyList(), emptyList()),
                updateState = states::add,
            )
        }

        assertEquals(emptyList(), states)
    }

    @Test
    fun scanReconcileFailurePublishesAuthoritativeContentAndTerminalError() = runBlocking {
        val content = LibraryContentState(emptyList(), listOf(testTrack("one")))
        val session = testScanSession(ScanStatus.Completed)
        val publications = mutableListOf<ScanPublicationState>()

        publishScanContentAfterReconcile(
            reconciler = PlaybackSessionReconciler { throw IllegalStateException("scan reconcile failed") },
            content = content,
            session = session,
            ownerIsActive = { true },
            publish = publications::add,
        )

        assertEquals(1, publications.size)
        assertSame(content, publications.single().content)
        assertSame(session, publications.single().progress.session)
        assertEquals(false, publications.single().progress.isActive)
        assertEquals("scan reconcile failed", publications.single().errorMessage)
    }

    @Test
    fun scanCancellationAfterMutationCleansActiveProgressThenRethrows() = runBlocking {
        val content = LibraryContentState(emptyList(), listOf(testTrack("one")))
        val publications = mutableListOf<ScanPublicationState>()

        assertFailsWith<CancellationException> {
            publishScanContentAfterReconcile(
                reconciler = PlaybackSessionReconciler { throw CancellationException("cancelled") },
                content = content,
                session = testScanSession(ScanStatus.Scanning),
                ownerIsActive = { true },
                publish = publications::add,
            )
        }

        assertEquals(1, publications.size)
        assertSame(content, publications.single().content)
        assertEquals(ScanStatus.Cancelled, publications.single().progress.session?.status)
        assertEquals(false, publications.single().progress.isActive)
        assertEquals(null, publications.single().errorMessage)
    }

    @Test
    fun scanCancellationWithGoneOwnerDoesNotPublishCleanup() = runBlocking {
        val publications = mutableListOf<ScanPublicationState>()

        assertFailsWith<CancellationException> {
            publishScanContentAfterReconcile(
                reconciler = PlaybackSessionReconciler { throw CancellationException("cancelled") },
                content = LibraryContentState(emptyList(), emptyList()),
                session = testScanSession(ScanStatus.Scanning),
                ownerIsActive = { false },
                publish = publications::add,
            )
        }

        assertEquals(emptyList(), publications)
    }
}

private fun testScanSession(status: ScanStatus) = ScanSession(
    id = "scan",
    sourceId = "source",
    status = status,
    startedAtEpochMillis = 1L,
)

private fun testTrack(id: String) = LibraryTrack(
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
