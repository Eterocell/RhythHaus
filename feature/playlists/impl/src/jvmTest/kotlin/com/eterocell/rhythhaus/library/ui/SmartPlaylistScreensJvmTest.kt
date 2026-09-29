package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.SmartPlaylistRule
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class SmartPlaylistScreensJvmTest {
    @Test
    fun allSixRulesRequireAvailableOptionsAndValidRecencyCounts() {
        val source = PlaylistSummary("source", "Mix", 1, 1)
        val snapshot = PlaylistSnapshot(playlists = listOf(source))
        val tracks =
            listOf(
                LibraryTrack(
                    "a",
                    "source",
                    "a",
                    AudioSource.FilePath("a.mp3"),
                    "a.mp3",
                    "Song",
                    "Artist",
                    "Album",
                    1000,
                    100,
                    null,
                    null,
                    1,
                    1))
        listOf(
                SmartPlaylistRule.Favorites,
                SmartPlaylistRule.RecentlyPlayed(10),
                SmartPlaylistRule.RecentlyPlayed(25),
                SmartPlaylistRule.RecentlyAdded(50),
                SmartPlaylistRule.Artist("Artist"),
                SmartPlaylistRule.Album("Artist", "Album"),
                SmartPlaylistRule.SavedPlaylist("source"),
            )
            .forEach { rule ->
                kotlin.test.assertTrue(
                    validSmartDraft(
                        SmartDraft(
                            PlaylistDismissalAppearance("draft"),
                            "Named",
                            rule),
                        snapshot,
                        tracks),
                    "$rule")
            }
        kotlin.test.assertFalse(
            validSmartDraft(
                SmartDraft(
                    PlaylistDismissalAppearance("draft"),
                    "Named",
                    SmartPlaylistRule.SavedPlaylist("gone")),
                snapshot,
                tracks))
        kotlin.test.assertFalse(
            validSmartDraft(
                SmartDraft(
                    PlaylistDismissalAppearance("draft"),
                    "Named",
                    SmartPlaylistRule.RecentlyAdded(11)),
                snapshot,
                tracks))
    }

    @Test
    fun existingArtistOrAlbumCanBeRenamedAfterLastMatchingTrackDisappears() {
        for (original in
            listOf(
                SmartPlaylistRule.Artist("Former artist"),
                SmartPlaylistRule.Album("Former artist", "Former album"),
                SmartPlaylistRule.SavedPlaylist("deleted-static-source"),
            )) {
            val renamed =
                SmartDraft(
                    PlaylistDismissalAppearance("edit"), "New name", original)
            kotlin.test.assertTrue(
                validSmartDraft(
                    renamed, PlaylistSnapshot(), emptyList(), original))
            kotlin.test.assertFalse(
                validSmartDraft(renamed, PlaylistSnapshot(), emptyList()))
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun smartCreateRetriesFailureWithoutDuplicatingPendingWritesAndIgnoresDismissedCallback() =
        runComposeUiTest {
            val locale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                val destination = PlaylistFeatureDestination("smart-hub")
                setContent {
                    Box(Modifier.size(600.dp, 400.dp)) {
                        PlaylistHubScreen(
                            state = PlaylistState(hasConfirmedSnapshot = true),
                            playbackState = PlaybackState(),
                            destination = destination,
                            appearanceSource =
                                rememberPlaylistFeatureAppearanceSource(
                                    destination),
                            dismissalPublisher = noDismissalPublisher(),
                            playlistsLabel = "Playlists",
                            loadingLabel = "Loading",
                            loadFailedLabel = "Failed",
                            retryLabel = "Retry",
                            mutationFailedLabel = "Failed",
                            onBack = {},
                            onOpenPlaylist = {},
                            onSelectTab = {},
                            onCreate = { _, _ -> },
                            libraryTracks = emptyList(),
                            onOpenSmartPlaylist = {},
                            onCreateSmartPlaylist = { _, _, done ->
                                completions += done
                            },
                            onSaveQueueAsPlaylist = { _, _, _ -> },
                            onRetry = {},
                            onReorderUpcoming = { _, _ -> error("unused") },
                            onRemoveUpcoming = { error("unused") },
                            onClearUpcoming = { error("unused") },
                        )
                    }
                }
                onAllNodes(hasText("Create smart playlist"))[0].performClick()
                onAllNodes(hasContentDescription("Playlist name"))
                    .assertCountEquals(1)
                onAllNodes(hasTestTag("smart-playlist-name"))[0]
                    .performTextInput("Favorites mix")
                onAllNodes(hasText("Save"))[0].performClick()
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(1, completions.size)
                completions[0](PlaylistStateAction.MutationFailed("db"))
                waitForIdle()
                onAllNodes(hasText("Could not save playlist changes"))
                    .assertCountEquals(1)
                onAllNodes(hasText("Save"))[0].performClick()
                assertEquals(2, completions.size)
                onAllNodes(hasText("Cancel"))[0].performClick()
                onAllNodes(hasText("Create smart playlist"))[0].performClick()
                completions[1](
                    PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
                waitForIdle()
                onAllNodes(hasText("Save")).assertCountEquals(1)
            } finally {
                Locale.setDefault(locale)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun duplicateSourceRowsRetainDistinctSelectionAndDoNotExposeStaticEntryActions() =
        runComposeUiTest {
            val destination = PlaylistFeatureDestination("smart-detail")
            var selected: SavedPlaylistPlaybackRequest? = null
            val smart =
                SmartPlaylistSummary(
                    "smart", "Duplicates", SmartPlaylistRule.Favorites, 1, 1)
            val track =
                PlayableTrack(
                    "a",
                    "Song",
                    "Artist",
                    "Album",
                    1000,
                    AudioSource.FilePath("a.mp3"))
            setContent {
                SmartPlaylistDetailScreen(
                    playlist = smart,
                    projection =
                        SmartPlaylistProjection(
                            listOf(
                                SmartPlaylistRow("smart:1", "a"),
                                SmartPlaylistRow("smart:2", "a"))),
                    playableTracksById = mapOf("a" to track),
                    libraryTracks = emptyList(),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(
                                    smartPlaylists = listOf(smart)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = noDismissalPublisher(),
                    mutationFailedLabel = "Failed",
                    onBack = {},
                    onRetry = {},
                    onUpdate = { _, _, _ -> },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onPlayEntry = { selected = it },
                )
            }
            onAllNodes(hasText("Song")).assertCountEquals(2)
            onAllNodes(hasText("Song"))[1].performClick()
            assertEquals("smart:2", selected?.selectedOccurrenceId)
            assertEquals(
                listOf("smart:1", "smart:2"),
                selected?.occurrences?.map { it.id })
            onAllNodes(hasText("Add tracks")).assertCountEquals(0)
            onAllNodes(hasText("Reorder")).assertCountEquals(0)
        }

    private fun noDismissalPublisher() =
        object : PlaylistFeatureDismissalPublisher {
            override fun publish(
                dismissal: PlaylistFeatureDismissal?,
                dispatch:
                    (
                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch
            ): () -> Unit = {}
        }
}
