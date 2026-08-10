package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Drives the real Shared playlist-detail route projection and play-entry path:
 * `LibraryRouteContent` builds the ID-keyed `playableTracksById` projection
 * from authoritative library tracks and settles playback through the
 * `onPlayEntry` wiring.
 */
class LibraryRouteAdapterJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeProjectionPreservesPlayableFieldsAndArtworkBytes() = runComposeUiTest {
        val controller = PlaybackController(FakePlaybackEngine())
        val artwork = byteArrayOf(9, 8, 7)
        setContent {
            playlistDetailRoute(
                controller,
                libraryTracks =
                    listOf(
                        libraryTrack("t-1", "Projected", artworkBytes = artwork)),
                entries = entries("pl-1", "e-1" to "t-1"),
            )
        }
        waitForIdle()

        onAllNodes(hasContentDescription("Projected, Artist, Album, 3:03"))[0]
            .performClick()
        waitForIdle()

        val queue = controller.state.value.queue
        assertEquals(listOf("e-1"), queue.map { it.id })
        val projected = queue.single().track
        assertEquals("t-1", projected.id)
        assertEquals("Projected", projected.title)
        assertEquals("Artist", projected.artist)
        assertEquals("Album", projected.album)
        assertEquals(183_000L, projected.durationMillis)
        assertEquals(AudioSource.FilePath("t-1.mp3"), projected.source)
        assertContentEquals(artwork, projected.artworkBytes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeProjectionUsesIdMapWithoutChangingOccurrenceOrder() = runComposeUiTest {
        val controller = PlaybackController(FakePlaybackEngine())
        setContent {
            playlistDetailRoute(
                controller,
                libraryTracks =
                    listOf(
                        libraryTrack("t-1", "First"),
                        libraryTrack("t-2", "Middle"),
                        libraryTrack("t-1", "Last"),
                    ),
                entries =
                    entries(
                        "pl-1",
                        "e-1" to "t-1",
                        "e-2" to "t-2",
                        "e-3" to "t-1",
                    ),
            )
        }
        waitForIdle()

        onAllNodes(hasContentDescription("Middle, Artist, Album, 3:03"))[0]
            .performClick()
        waitForIdle()

        val queue = controller.state.value.queue
        assertEquals(
            listOf("e-1", "e-2", "e-3"), queue.map { it.id },
            "occurrence order follows the playlist entries")
        assertEquals(
            listOf("Last", "Middle", "Last"), queue.map { it.track.title },
            "the ID map resolves duplicates to the last projection")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeProjectionPreservesSelectedOccurrence() = runComposeUiTest {
        val controller = PlaybackController(FakePlaybackEngine())
        setContent {
            playlistDetailRoute(
                controller,
                libraryTracks =
                    listOf(
                        libraryTrack("t-1", "One"),
                        libraryTrack("t-2", "Two"),
                        libraryTrack("t-3", "Three"),
                    ),
                entries =
                    entries(
                        "pl-1",
                        "e-1" to "t-1",
                        "e-2" to "t-2",
                        "e-3" to "t-3",
                    ),
            )
        }
        waitForIdle()

        onAllNodes(hasContentDescription("Two, Artist, Album, 3:03"))[0]
            .performClick()
        waitForIdle()

        assertEquals("e-2", controller.state.value.currentOccurrenceId)
        assertEquals("t-2", controller.state.value.currentTrack?.id)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playEntryFailureDoesNotSettle() = runComposeUiTest {
        val controller = PlaybackController(FakePlaybackEngine())
        controller.setCommandsEnabled(false)
        setContent {
            playlistDetailRoute(
                controller,
                libraryTracks = listOf(libraryTrack("t-1", "One")),
                entries = entries("pl-1", "e-1" to "t-1"),
            )
        }
        waitForIdle()

        onAllNodes(hasContentDescription("One, Artist, Album, 3:03"))[0]
            .performClick()
        waitForIdle()

        assertTrue(
            controller.state.value.queue.isEmpty(),
            "disabled playback must not settle the queue")
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playEntrySettlementSettlesExactlyOnce() = runComposeUiTest {
        val controller = PlaybackController(FakePlaybackEngine())
        setContent {
            playlistDetailRoute(
                controller,
                libraryTracks =
                    listOf(libraryTrack("t-1", "One"), libraryTrack("t-2", "Two")),
                entries = entries("pl-1", "e-1" to "t-1", "e-2" to "t-2"),
            )
        }
        waitForIdle()

        val row = hasContentDescription("One, Artist, Album, 3:03")
        onAllNodes(row)[0].performClick()
        waitForIdle()
        assertEquals(listOf("e-1", "e-2"), controller.state.value.queue.map { it.id })

        onAllNodes(row)[0].performClick()
        waitForIdle()
        assertEquals(
            listOf("e-1", "e-2"), controller.state.value.queue.map { it.id },
            "a repeated click restarts the current occurrence instead of re-settling")
    }

    @Composable
    private fun playlistDetailRoute(
        controller: PlaybackController,
        libraryTracks: List<LibraryTrack>,
        entries: List<PlaylistEntry>,
    ) {
        val playbackState by controller.state.collectAsState()
        LibraryRouteContent(
            route = LibraryRoute.PlaylistDetail("pl-1"),
            tracks = emptyList(),
            snapshot = LibrarySnapshot("Library", "", emptyList(), null),
            libraryTracks = libraryTracks,
            playbackController = controller,
            playbackState = playbackState,
            playlistRepository = EmptyPlaylistRepository,
            playlistState =
                PlaylistState(
                    confirmedSnapshot =
                        PlaylistSnapshot(
                            playlists =
                                listOf(PlaylistSummary("pl-1", "Saved", 1, 1)),
                            entriesByPlaylistId = mapOf("pl-1" to entries),
                        ),
                    hasConfirmedSnapshot = true,
                ),
            onPlaylistStateAction = {},
            onRefreshPlaylists = {},
            onPlaylistMutation = { _, _ -> },
            onRecoverStalePlaylistDetail = {},
            onDisplayedPlaylistDeleteConfirmed = {},
            selectedTrackId = null,
            isNowPlayingBarVisible = true,
            onBack = {},
            destinationId =
                LibraryDestinationId(LibraryRoute.PlaylistDetail("pl-1"), "adapter"),
            playlistAppearanceSource =
                rememberPlaylistFeatureAppearanceSource(
                    PlaylistFeatureDestination("adapter")),
            registerBackSurface = { {} },
            onOpenDetailRoute = {},
            onTrackSelected = {},
            onTrackClickFromTracks = { _, _ -> },
            onExpandNowPlaying = {},
            onShowSettings = {},
            onShowSearch = {},
            onScrollPositionChanged = {},
            artworkLoader = { null },
            homeContent = { _ -> },
            trackSelectionState = TrackSelectionState(),
            onTrackSelectionAction = {},
            bottomContentPadding = 0.dp,
        )
    }

    private fun entries(
        playlistId: String,
        vararg idToTrack: Pair<String, String>,
    ): List<PlaylistEntry> =
        idToTrack.mapIndexed { index, (entryId, trackId) ->
            PlaylistEntry(
                id = entryId,
                playlistId = playlistId,
                trackId = trackId,
                position = index,
                createdAtEpochMillis = 1,
            )
        }

    private fun libraryTrack(
        id: String,
        title: String,
        artworkBytes: ByteArray? = null,
    ): LibraryTrack =
        LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("$id.mp3"),
            displayName = id,
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = 183_000L,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
            trackNumber = 1,
            discNumber = 1,
            artworkBytes = artworkBytes,
        )

    private object EmptyPlaylistRepository : PlaylistRepository {
        override fun playlists() = emptyList<PlaylistSummary>()

        override fun playlist(id: String) = null

        override fun entries(playlistId: String) = emptyList<PlaylistEntry>()

        override fun create(name: String) = error("unused")

        override fun createWithEntries(name: String, trackIds: List<String>) =
            error("unused")

        override fun importPlaylists(playlists: List<PlaylistImportMutation>) =
            error("unused")

        override fun rename(id: String, name: String) = error("unused")

        override fun delete(id: String) = error("unused")

        override fun append(playlistId: String, trackIds: List<String>) =
            error("unused")

        override fun removeEntry(entryId: String) = error("unused")

        override fun reorder(playlistId: String, entryIds: List<String>) =
            error("unused")
    }
}
