package com.eterocell.rhythhaus.library.ui

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PlaylistScreensTest {
    @Test
    fun playlistNameDraftTrimsValidNamesAndRejectsBlankWithoutDiscardingText() {
        val valid = PlaylistNameDraft("  Road trip  ")
        val blank = PlaylistNameDraft("   ")

        assertEquals("Road trip", valid.confirmedName())
        assertNull(blank.confirmedName())
        assertEquals("   ", blank.enteredText)
    }

    @Test
    fun failedNameMutationRetainsEnteredTextAndConfirmedSnapshot() {
        val draft = PlaylistNameDraft("  同名歌单  ")
        val failed = draft.mutationFailed()

        assertEquals("  同名歌单  ", failed.enteredText)
        assertTrue(failed.showFailure)
    }

    @Test
    fun duplicateEntriesMoveAndRemoveByEntryId() {
        val ids = listOf("entry-a", "entry-b", "entry-c")

        assertEquals(listOf("entry-b", "entry-a", "entry-c"), movedPlaylistEntryIds(ids, "entry-b", -1))
        assertEquals(listOf("entry-a", "entry-c"), ids.filterNot { it == "entry-b" })
        assertEquals(ids, movedPlaylistEntryIds(ids, "missing", 1))
    }

    @Test
    fun accessibleMoveAvailabilityMatchesVisibleEntryPosition() {
        val ids = listOf("entry-a", "entry-b", "entry-c")

        assertEquals(PlaylistMoveAvailability(canMoveUp = false, canMoveDown = true), playlistMoveAvailability(ids, "entry-a"))
        assertEquals(PlaylistMoveAvailability(canMoveUp = true, canMoveDown = true), playlistMoveAvailability(ids, "entry-b"))
        assertEquals(PlaylistMoveAvailability(canMoveUp = true, canMoveDown = false), playlistMoveAvailability(ids, "entry-c"))
    }

    @Test
    fun savedPlaybackUsesExactVisibleEntryOrderAndSelectedOccurrence() {
        val track = playableTrack("track-a")
        val entries = listOf(
            entry("entry-first", track.id, 0),
            entry("entry-second", track.id, 1),
        )

        val request = savedPlaylistPlaybackRequest(
            visibleEntries = entries,
            tracksById = mapOf(track.id to track),
            selectedEntryId = "entry-second",
        )

        assertEquals(listOf("entry-first", "entry-second"), request?.occurrences?.map { it.id })
        assertEquals("entry-second", request?.selectedOccurrenceId)
    }

    @Test
    fun pickerConfirmationAppendsOneIndependentOccurrenceEveryTime() {
        val picker = AddToPlaylistPickerState(trackId = "track-a", selectedPlaylistId = "playlist-1")

        assertEquals(PlaylistAppendRequest("playlist-1", listOf("track-a")), picker.confirmedAppend())
        assertEquals(PlaylistAppendRequest("playlist-1", listOf("track-a")), picker.confirmedAppend())
    }

    @Test
    fun pickerInlineCreationTrimsNameAndRetainsInvalidText() {
        val valid = AddToPlaylistPickerState(trackId = "track-a", enteredName = "  New list ")
        val blank = AddToPlaylistPickerState(trackId = "track-a", enteredName = "   ")

        assertEquals(PlaylistInlineCreateRequest("New list", "track-a"), valid.confirmedInlineCreate())
        assertNull(blank.confirmedInlineCreate())
        assertEquals("   ", blank.enteredName)
    }

    @Test
    fun detailBrowserAppendsSelectedTracksInVisibleOrder() {
        val state = PlaylistTrackBrowserState(
            playlistId = "playlist-1",
            visibleTrackIds = listOf("b", "a", "c"),
            selectedTrackIds = setOf("a", "b"),
        )

        assertEquals(listOf("b", "a"), state.confirmedTrackIds())
        assertEquals(PlaylistAppendRequest("playlist-1", listOf("b", "a")), state.confirmedAppend())
    }

    @Test
    fun detailBrowserSelectionIsKeyedByTrackIdAndSurvivesFiltering() {
        val selected = PlaylistTrackBrowserState(
            playlistId = "playlist-1",
            visibleTrackIds = listOf("a", "b"),
        ).toggle("b")
        val filtered = selected.copy(visibleTrackIds = listOf("b"))

        assertTrue("b" in filtered.selectedTrackIds)
        assertFalse("a" in filtered.selectedTrackIds)
        assertEquals(listOf("b"), filtered.confirmedTrackIds())
    }

    @Test
    fun removingFinalEntryRetainsEmptyPlaylistDetailModel() {
        val model = playlistDetailModel(
            playlistId = "playlist-1",
            playlistName = "Keep me",
            entries = listOf(entry("only", "track-a", 0)),
            tracksById = mapOf("track-a" to playableTrack("track-a")),
        ).withoutEntry("only")

        assertEquals("playlist-1", model.playlistId)
        assertEquals("Keep me", model.playlistName)
        assertTrue(model.rows.isEmpty())
    }

    @Test
    fun rowOverflowOpensPickerForExactTrackId() {
        assertEquals(
            PlaylistStateAction.OpenPicker(PlaylistPickerState(trackId = "track-b")),
            openAddToPlaylistPickerAction("track-b"),
        )
    }

    @Test
    fun pickerInlineCreationPlansCreateThenAppendWithoutCollapsingDuplicates() {
        val request = PlaylistInlineCreateRequest(name = "New", trackId = "track-a")

        assertEquals(
            PlaylistInlineMutationPlan(name = "New", trackIds = listOf("track-a")),
            request.mutationPlan(),
        )
    }

    @Test
    fun browserFilteringUsesAuthoritativeOrderAcrossTitleArtistAndAlbum() {
        val tracks = listOf(
            browserTrack("b", title = "Beta", artist = "二号", album = "Night"),
            browserTrack("a", title = "Alpha", artist = "One", album = "晨光"),
            browserTrack("c", title = "Gamma", artist = "Three", album = "Day"),
        )

        assertEquals(listOf("b"), filteredPlaylistTrackIds(tracks, "二号"))
        assertEquals(listOf("a"), filteredPlaylistTrackIds(tracks, "晨光"))
        assertEquals(listOf("b", "a", "c"), filteredPlaylistTrackIds(tracks, ""))
    }

    private fun entry(id: String, trackId: String, position: Int) = PlaylistEntry(
        id = id,
        playlistId = "playlist-1",
        trackId = trackId,
        position = position,
        createdAtEpochMillis = 1L,
    )

    private fun playableTrack(id: String) = PlayableTrack(
        id = id,
        source = AudioSource.FilePath("/$id.mp3"),
        title = "Title $id",
        artist = "Artist",
        album = "Album",
        durationMillis = 180_000L,
    )

    private fun browserTrack(id: String, title: String, artist: String, album: String) =
        com.eterocell.rhythhaus.library.LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = title,
            title = title,
            artist = artist,
            album = album,
            durationMillis = 180_000L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
}
