package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
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
            assertEquals(expectedHistory, publications.single().content.playHistory)
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
        owner.publish(loadLibraryContent(repository, HistoryPlatformSourceAccess))
        var scanCancellationRequests = 0
        val coordinator =
            AppLibraryOperationCoordinator { scanCancellationRequests++ }
        val scan =
            coordinator.admitScan() as LibraryOperationAdmission.Admitted
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
        assertEquals(LibraryOperationState.Running(scan.token), coordinator.state.value)
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
