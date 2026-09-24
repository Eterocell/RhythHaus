package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.library.InMemoryLibraryRepository
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.impl.PlatformScanEvent
import com.eterocell.rhythhaus.loadLibraryContent
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryBrowseQueryStateTest {
    @Test
    fun newLibraryShellStartsWithTheDefaultBrowseQuery() {
        val state = LibraryAppState(initialSelectedTrackId = null)

        assertEquals(LibraryBrowseQuery(), state.browseQuery)
    }

    @Test
    fun browseQuerySettersPreserveIndependentSortAndFilterChoices() {
        val state = LibraryAppState(initialSelectedTrackId = null)

        state.setBrowseSort(LibrarySort.Modified)
        assertEquals(
            LibraryBrowseQuery(sort = LibrarySort.Modified),
            state.browseQuery,
        )

        state.setBrowseSortDirection(LibrarySortDirection.Descending)
        assertEquals(
            LibraryBrowseQuery(
                sort = LibrarySort.Modified,
                direction = LibrarySortDirection.Descending,
            ),
            state.browseQuery,
        )

        state.setBrowseFavoriteOnly(true)
        state.setBrowseArtworkOnly(true)
        state.setBrowseSourceId("source-b")
        assertEquals(
            LibraryBrowseQuery(
                sort = LibrarySort.Modified,
                direction = LibrarySortDirection.Descending,
                favoriteOnly = true,
                artworkOnly = true,
                sourceId = "source-b",
            ),
            state.browseQuery,
        )

        state.setBrowseFavoriteOnly(false)
        assertEquals(
            LibraryBrowseQuery(
                sort = LibrarySort.Modified,
                direction = LibrarySortDirection.Descending,
                artworkOnly = true,
                sourceId = "source-b",
            ),
            state.browseQuery,
        )
    }

    @Test
    fun aNewLibraryShellDoesNotReuseThePreviousShellQuery() {
        val oldShell = LibraryAppState(initialSelectedTrackId = null)
        oldShell.setBrowseSort(LibrarySort.PlayCount)
        oldShell.setBrowseArtworkOnly(true)
        oldShell.setBrowseSourceId("source-a")

        val newShell = LibraryAppState(initialSelectedTrackId = null)

        assertEquals(LibraryBrowseQuery(), newShell.browseQuery)
    }

    @Test
    fun libraryContentPublishesAuthoritativeSourceAndModifiedProjections() {
        val repository =
            InMemoryLibraryRepository().apply {
                upsertSource(source("source-a"))
                upsertSource(source("source-b"))
                upsertTrack(
                    track(
                        id = "known-modified",
                        sourceId = "source-a",
                        modifiedAtEpochMillis = 40L,
                    ),
                )
                upsertTrack(
                    track(
                        id = "unknown-modified",
                        sourceId = "source-b",
                        modifiedAtEpochMillis = null,
                    ),
                )
            }

        val content = loadLibraryContent(repository, AvailableSourceAccess)

        assertEquals(
            mapOf(
                "known-modified" to "source-a",
                "unknown-modified" to "source-b",
            ),
            content.sourceIdByTrackId,
        )
        assertEquals(
            mapOf(
                "known-modified" to 40L,
                "unknown-modified" to null,
            ),
            content.modifiedAtByTrackId,
        )
    }

    @Test
    fun browseQueryUpdatesDoNotMutatePlaybackState() {
        val controller = PlaybackController(FakePlaybackEngine())
        controller.setRepeatMode(RepeatMode.RepeatOne)
        controller.setShuffleMode(ShuffleMode.On)
        val before = controller.state.value
        val state = LibraryAppState(initialSelectedTrackId = null)

        state.setBrowseSort(LibrarySort.Album)
        state.setBrowseSortDirection(LibrarySortDirection.Descending)
        state.setBrowseFavoriteOnly(true)
        state.setBrowseArtworkOnly(true)
        state.setBrowseSourceId("source-a")

        assertEquals(before, controller.state.value)
    }

    private fun source(id: String): LibrarySource =
        LibrarySource(
            id = id,
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = id,
            handle = "/$id",
            createdAtEpochMillis = 1L,
        )

    private fun track(
        id: String,
        sourceId: String,
        modifiedAtEpochMillis: Long?,
    ): LibraryTrack =
        LibraryTrack(
            id = id,
            sourceId = sourceId,
            sourceLocalKey = "$id.mp3",
            audioSource = AudioSource.FilePath("/$sourceId/$id.mp3"),
            displayName = "$id.mp3",
            title = id,
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = modifiedAtEpochMillis,
            lastSeenScanId = null,
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 10L,
        )

    private object AvailableSourceAccess : PlatformSourceAccess {
        override fun scan(source: LibrarySource): Sequence<PlatformScanEvent> =
            emptySequence()
    }
}
