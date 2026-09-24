package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking

class PlaybackHistoryAdmissionTest {
    @Test
    fun admittedPlaybackRecordsIndexedTrackAndRepublishesRecentHistory() =
        runBlocking {
            val repository =
                InMemoryLibraryRepository().apply {
                    upsertSource(historySource())
                    upsertTrack(historyTrack("history"))
                }
            val owner = AuthoritativeLibraryPublicationOwner()
            val initial =
                owner.publish(
                    loadLibraryContent(repository, HistoryPlatformSourceAccess))
            val publications = mutableListOf<AuthoritativeLibraryPublication>()

            recordPlaybackHistoryAndPublish(
                event = PlaybackStarted(1L, "history-occurrence", "history"),
                publicationOwner = owner,
                repository = repository,
                platformAccess = HistoryPlatformSourceAccess,
                playedAtEpochMillis = 100L,
                ioDispatcher = Dispatchers.Default,
                publish = publications::add,
            )

            val expectedHistory =
                mapOf("history" to TrackPlayHistory("history", 1L, 100L))
            assertEquals(expectedHistory, repository.playHistory())
            assertEquals(1, publications.size)
            assertEquals(initial.revision + 1L, publications.single().revision)
            assertEquals(
                expectedHistory, publications.single().content.playHistory)
            assertEquals(
                mapOf("history" to 10L),
                publications.single().content.createdAtByTrackId,
            )
        }

    @Test
    fun playbackHistoryDoesNotCancelActiveScan() = runBlocking {
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(historySource())
                upsertTrack(historyTrack("history"))
            }
        val owner = AuthoritativeLibraryPublicationOwner()
        owner.publish(
            loadLibraryContent(repository, HistoryPlatformSourceAccess))
        var scanCancellationRequests = 0
        val coordinator = AppLibraryOperationCoordinator {
            scanCancellationRequests++
        }
        val scan = coordinator.admitScan() as LibraryOperationAdmission.Admitted
        val publications = mutableListOf<AuthoritativeLibraryPublication>()

        recordPlaybackHistoryAndPublish(
            event = PlaybackStarted(1L, "history-occurrence", "history"),
            publicationOwner = owner,
            repository = repository,
            platformAccess = HistoryPlatformSourceAccess,
            playedAtEpochMillis = 100L,
            ioDispatcher = Dispatchers.Default,
            publish = publications::add,
        )

        assertEquals(0, scanCancellationRequests)
        assertEquals(
            LibraryOperationState.Running(scan.token), coordinator.state.value)
        assertEquals(
            mapOf("history" to TrackPlayHistory("history", 1L, 100L)),
            publications.single().content.playHistory,
        )
        coordinator.complete(scan.token)
    }

    @Test
    fun removedTrackEventIsIgnoredWithoutStalePublication() = runBlocking {
        val source = historySource()
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(source)
                upsertTrack(historyTrack("history"))
            }
        val owner = AuthoritativeLibraryPublicationOwner()
        val initial =
            owner.publish(
                loadLibraryContent(repository, HistoryPlatformSourceAccess))
        var publicationCalls = 0

        repository.removeSource(source.id)
        recordPlaybackHistoryAndPublish(
            event = PlaybackStarted(1L, "history-occurrence", "history"),
            publicationOwner = owner,
            repository = repository,
            platformAccess = HistoryPlatformSourceAccess,
            playedAtEpochMillis = 100L,
            ioDispatcher = Dispatchers.Default,
            publish = { publicationCalls++ },
        )

        assertEquals(emptyMap(), repository.playHistory())
        assertEquals(0, publicationCalls)
        assertEquals(initial.revision, owner.revision)
    }

    @Test
    fun appLibraryContentStateReplacesHistoryProjectionsFromPublication() {
        val state = AppLibraryContentState()
        val initial =
            LibraryContentState(
                sources = listOf(historySource()),
                tracks = listOf(historyTrack("history")),
                favoriteTrackIds = setOf("history"),
                playHistory =
                    mapOf(
                        "history" to TrackPlayHistory("history", 1L, 100L),
                    ),
                createdAtByTrackId = mapOf("history" to 10L),
            )
        val current =
            initial.copy(
                playHistory =
                    mapOf(
                        "history" to TrackPlayHistory("history", 2L, 200L),
                    ),
                createdAtByTrackId = mapOf("history" to 20L),
            )

        state.apply(AuthoritativeLibraryPublication(initial, revision = 1L))
        state.apply(AuthoritativeLibraryPublication(current, revision = 2L))

        assertEquals(current.playHistory, state.content.playHistory)
        assertEquals(
            current.createdAtByTrackId, state.content.createdAtByTrackId)
    }

    @Test
    fun appLibraryContentStateRejectsOlderPublicationAfterNewerHistory() {
        val state = AppLibraryContentState()
        val older =
            LibraryContentState(
                sources = listOf(historySource()),
                tracks = listOf(historyTrack("history")),
                playHistory =
                    mapOf(
                        "history" to TrackPlayHistory("history", 1L, 100L),
                    ),
                createdAtByTrackId = mapOf("history" to 10L),
            )
        val newer =
            older.copy(
                playHistory =
                    mapOf(
                        "history" to TrackPlayHistory("history", 2L, 200L),
                    ),
                createdAtByTrackId = mapOf("history" to 20L),
            )

        state.apply(AuthoritativeLibraryPublication(newer, revision = 2L))
        state.apply(AuthoritativeLibraryPublication(older, revision = 1L))

        assertEquals(newer, state.content)
    }

    @Test
    fun historyCollectorReportsFailureAndContinuesWithLaterEvents() =
        runBlocking {
            val repository =
                FailFirstHistoryRecordRepository(
                    InMemoryLibraryRepository().apply {
                        upsertSource(historySource())
                        upsertTrack(historyTrack("history"))
                    },
                )
            val owner = AuthoritativeLibraryPublicationOwner()
            owner.publish(
                loadLibraryContent(repository, HistoryPlatformSourceAccess))
            val reports = mutableListOf<String>()
            val publications = mutableListOf<AuthoritativeLibraryPublication>()

            collectPlaybackHistoryAndPublish(
                playbackStarted =
                    flowOf(
                        PlaybackStarted(1L, "failed-occurrence", "history"),
                        PlaybackStarted(2L, "accepted-occurrence", "history"),
                    ),
                publicationOwner = owner,
                repository = repository,
                platformAccess = HistoryPlatformSourceAccess,
                playedAtEpochMillis = { 100L },
                ioDispatcher = Dispatchers.Default,
                reportFailure = reports::add,
                publish = publications::add,
            )

            assertEquals(listOf("history write failed"), reports)
            assertEquals(
                mapOf("history" to TrackPlayHistory("history", 1L, 100L)),
                repository.playHistory(),
            )
            assertEquals(1, publications.size)
        }

    @Test
    fun historyCollectorRethrowsCancellation() = runBlocking {
        val repository =
            CancellationHistoryRecordRepository(
                InMemoryLibraryRepository().apply {
                    upsertSource(historySource())
                    upsertTrack(historyTrack("history"))
                },
            )
        val reports = mutableListOf<String>()

        assertFailsWith<CancellationException> {
            collectPlaybackHistoryAndPublish(
                playbackStarted =
                    flowOf(
                        PlaybackStarted(1L, "cancelled-occurrence", "history")),
                publicationOwner = AuthoritativeLibraryPublicationOwner(),
                repository = repository,
                platformAccess = HistoryPlatformSourceAccess,
                playedAtEpochMillis = { 100L },
                ioDispatcher = Dispatchers.Default,
                reportFailure = reports::add,
                publish = {},
            )
        }

        assertEquals(emptyList(), reports)
    }
}

private class FailFirstHistoryRecordRepository(
    private val delegate: LibraryRepository,
) : LibraryRepository by delegate {
    private var shouldFail = true

    override fun recordTrackPlayed(
        trackId: String,
        playedAtEpochMillis: Long,
    ): Boolean =
        if (shouldFail) {
            shouldFail = false
            error("history write failed")
        } else {
            delegate.recordTrackPlayed(trackId, playedAtEpochMillis)
        }
}

private class CancellationHistoryRecordRepository(
    delegate: LibraryRepository,
) : LibraryRepository by delegate {
    override fun recordTrackPlayed(
        trackId: String,
        playedAtEpochMillis: Long,
    ): Boolean = throw CancellationException("history collection cancelled")
}

private object HistoryPlatformSourceAccess : PlatformSourceAccess {
    override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
        emptySequence()
}

private fun historySource() =
    LibrarySource(
        id = "source",
        platformKind = LibraryPlatformKind.JvmFolder,
        displayName = "History",
        handle = "/history",
        createdAtEpochMillis = 1L,
    )

private fun historyTrack(id: String) =
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
        createdAtEpochMillis = 10L,
        updatedAtEpochMillis = 10L,
    )
