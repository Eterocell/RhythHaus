package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Consumer regressions for the playlist rows after they moved onto the shared
 * `HausSongRow` primitive: occurrence identity, queue state, and the
 * narrow-width action rail.
 */
class PlaylistRowAdaptersJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun trackPickerLabelsMetadataAndTogglesOnceFromRowOrCheckboxInVisibleOrder() =
        runComposeUiTest {
            var state by mutableStateOf(PlaylistTrackBrowserState("saved"))
            var stateChanges = 0
            var appended: PlaylistAppendRequest? = null
            val tracks =
                listOf("one", "two").map { id ->
                    LibraryTrack(
                        id,
                        "source",
                        id,
                        AudioSource.FilePath("$id.mp3"),
                        "$id.mp3",
                        "Track $id",
                        "Artist",
                        "Album",
                        1000,
                        1,
                        null,
                        null,
                        1,
                        1)
                }
            setContent {
                Box(Modifier.size(600.dp, 700.dp)) {
                    PlaylistTrackBrowser(
                        playlistName = "Saved",
                        libraryTracks = tracks,
                        state = state,
                        onStateChange = {
                            state = it
                            stateChanges++
                        },
                        onDismiss = {},
                        onConfirm = { appended = it },
                    )
                }
            }
            val first = hasContentDescription("Track one, Artist, Album")
            val second = hasContentDescription("Track two, Artist, Album")
            onAllNodes(first)[0].assertIsOff().performClick()
            onAllNodes(first)[0].assertIsOn()
            onAllNodes(
                    isToggleable() and hasAnyAncestor(first),
                    useUnmergedTree = true)[0]
                .performClick()
            onAllNodes(first)[0].assertIsOff()
            assertEquals(2, stateChanges)
            onAllNodes(second)[0].performClick()
            onAllNodes(
                    isToggleable() and hasAnyAncestor(first),
                    useUnmergedTree = true)[0]
                .performClick()
            assertEquals(4, stateChanges)
            onAllNodes(hasText("Add selected tracks"))[0].performClick()
            assertEquals(listOf("one", "two"), appended?.trackIds)
        }

    private var originalLocale: Locale? = null

    @BeforeTest
    fun setEnglishLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        originalLocale?.let(Locale::setDefault)
    }

    @Test
    fun rowActionTargetsFollowShellDensity() {
        assertEquals(
            48.dp,
            playlistDetailRowLayoutPolicy(
                    800.dp,
                    isEditable = true,
                    desktopDensity = false,
                )
                .minimumInteractiveTarget,
        )
        assertEquals(
            44.dp,
            playlistDetailRowLayoutPolicy(
                    360.dp,
                    isEditable = true,
                    desktopDensity = true,
                )
                .minimumInteractiveTarget,
        )
        assertEquals(
            48.dp,
            queueRowLayoutPolicy(
                    800.dp,
                    isEditable = true,
                    desktopDensity = false,
                )
                .minimumInteractiveTarget,
        )
        assertEquals(
            44.dp,
            queueRowLayoutPolicy(
                    360.dp,
                    isEditable = true,
                    desktopDensity = true,
                )
                .minimumInteractiveTarget,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queueRowsKeepDuplicateOccurrencesAndTheirState() = runComposeUiTest {
        val track = playableTrack()
        val playbackState =
            PlaybackState(
                currentOccurrenceId = "current",
                queue =
                    listOf(
                        QueueOccurrence("played", track),
                        QueueOccurrence("current", track),
                        QueueOccurrence("up-1", track),
                        QueueOccurrence("up-2", track),
                    ),
            )
        var removedOccurrenceId: String? = null
        var reordered: Pair<String, Int>? = null
        var closeCalls = 0
        val destination = PlaylistFeatureDestination("queue-rows")
        setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                PlaybackQueueContent(
                    playbackState = playbackState,
                    destination = destination,
                    modifier = Modifier.fillMaxSize().testTag("queue-rows"),
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = silentPublisher(),
                    onClose = { closeCalls++ },
                    onSaveQueueAsPlaylist = { _, _, _ -> },
                    onReorderUpcoming = { occurrenceId, targetIndex ->
                        reordered = occurrenceId to targetIndex
                        QueueMutationFeedback(playbackState, false)
                    },
                    onRemoveUpcoming = { occurrenceId ->
                        removedOccurrenceId = occurrenceId
                        QueueMutationFeedback(playbackState, false)
                    },
                    onClearUpcoming = {
                        QueueMutationFeedback(playbackState, false)
                    },
                    artworkLoader = { null },
                )
            }
        }
        waitForIdle()

        onNodeWithTag("queue-rows").assertExists()
        onAllNodes(hasText("Track")).assertCountEquals(4)
        onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Current queue track",
                ),
            )
            .assertCountEquals(1)
        onAllNodes(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Previously played queue track",
                ),
            )
            .assertCountEquals(1)
        val removeActions =
            onAllNodes(hasContentDescription("Remove Track from queue"))
        removeActions.assertCountEquals(2)
        removeActions[0]
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        val reorderActions =
            onAllNodes(hasContentDescription("Reorder Track in queue"))
        reorderActions.assertCountEquals(2)
        reorderActions[0]
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        onAllNodes(hasContentDescription("Move Track down in queue"))[0]
            .performClick()
        waitForIdle()
        assertEquals("up-1" to 1, reordered)

        onAllNodes(hasContentDescription("Remove Track from queue"))[1]
            .performClick()
        waitForIdle()
        assertEquals("up-2", removedOccurrenceId)

        onAllNodes(hasContentDescription("Back to player"))[0].performClick()
        assertEquals(1, closeCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun duplicateSavedEntriesKeepTheirOwnOccurrenceIdentity() =
        runComposeUiTest {
            var played: SavedPlaylistPlaybackRequest? = null
            setContent {
                Box(Modifier.size(360.dp, 640.dp)) {
                    SavedPlaylistDetail(
                        entries = duplicateEntries(),
                        initialEditMode = false,
                        onPlayEntry = { played = it },
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))
                .assertCountEquals(2)
            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))[1]
                .performClick()
            waitForIdle()
            assertEquals("entry-2", played?.selectedOccurrenceId)
            assertEquals(
                listOf("entry-1", "entry-2"),
                played?.occurrences?.map(QueueOccurrence::id))
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun savedRowsOmitMissingAlbumFromVisibleAndAccessibleMetadata() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(360.dp, 640.dp)) {
                    SavedPlaylistDetail(
                        entries = duplicateEntries().take(1),
                        initialEditMode = false,
                        track = playableTrack().copy(album = null),
                        onPlayEntry = {},
                    )
                }
            }
            waitForIdle()

            onAllNodes(hasText("Artist", substring = true)).assertCountEquals(1)
            onAllNodes(hasText("Artist · null", substring = true))
                .assertCountEquals(0)
            onAllNodes(hasContentDescription("Track, Artist, 0:01"))
                .assertCountEquals(1)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun savedRowsOmitBlankArtistWithoutALeadingSeparator() = runComposeUiTest {
        setContent {
            Box(Modifier.size(360.dp, 640.dp)) {
                SavedPlaylistDetail(
                    entries = duplicateEntries().take(1),
                    initialEditMode = false,
                    track = playableTrack().copy(artist = "", album = "Album"),
                    onPlayEntry = {},
                )
            }
        }
        waitForIdle()

        onAllNodes(hasText("Album", substring = false)).assertCountEquals(1)
        onAllNodes(hasText(" · Album", substring = false)).assertCountEquals(0)
        onAllNodes(hasContentDescription("Track, Album, 0:01"))
            .assertCountEquals(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun narrowSavedRowsKeepAccessibleDragAndMutationActions() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(360.dp, 640.dp)) {
                    SavedPlaylistDetail(
                        entries = duplicateEntries(),
                        initialEditMode = true,
                        onPlayEntry = {},
                    )
                }
            }
            waitForIdle()

            onNodeWithTag(
                    "playlist-entry-action-rail-entry-2",
                    useUnmergedTree = true,
                )
                .assertExists()
            onAllNodes(hasContentDescription("Reorder Track"))
                .assertCountEquals(2)
            onAllNodes(hasContentDescription("Reorder Track"))[0]
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
            val removeActions =
                onAllNodes(hasContentDescription("Remove Track from playlist"))
            removeActions.assertCountEquals(2)
            removeActions[0]
                .assertWidthIsAtLeast(48.dp)
                .assertHeightIsAtLeast(48.dp)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun wideSavedRowsUseDesktopDensity() = runComposeUiTest {
        setContent {
            SavedPlaylistDetail(
                entries = duplicateEntries().take(1),
                initialEditMode = false,
                width = 1_280.dp,
                height = 900.dp,
                desktopDensity = true,
                onPlayEntry = {},
            )
        }
        waitForIdle()

        val row =
            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))
                .fetchSemanticsNodes()
                .single()
        val rowHeightDp =
            row.boundsInRoot.height / row.layoutInfo.density.density
        assertEquals(56f, rowHeightDp, 0.5f)
    }

    @Composable
    private fun SavedPlaylistDetail(
        entries: List<PlaylistEntry>,
        initialEditMode: Boolean,
        width: androidx.compose.ui.unit.Dp = 360.dp,
        height: androidx.compose.ui.unit.Dp = 640.dp,
        desktopDensity: Boolean = false,
        track: PlayableTrack = playableTrack(),
        onPlayEntry: (SavedPlaylistPlaybackRequest) -> Unit,
    ) {
        val playlist =
            PlaylistSummary("playlist", "Saved", entries.size.toLong(), 1L)
        val destination = PlaylistFeatureDestination("detail-adapters")
        Box(Modifier.size(width, height)) {
            PlaylistDetailScreen(
                playlist = playlist,
                entries = entries,
                playableTracksById = mapOf(track.id to track),
                artworkLoader = { null },
                state =
                    PlaylistState(
                        confirmedSnapshot =
                            PlaylistSnapshot(playlists = listOf(playlist)),
                        hasConfirmedSnapshot = true,
                    ),
                destination = destination,
                appearanceSource =
                    rememberPlaylistFeatureAppearanceSource(destination),
                dismissalPublisher = silentPublisher(),
                mutationFailedLabel = "Could not save playlist changes",
                onBack = {},
                onRetry = {},
                onRename = { _, _ -> },
                onDelete = {},
                onDeleteConfirmed = {},
                onOpenBrowser = {},
                onPlayEntry = onPlayEntry,
                onRemoveEntry = {},
                onReorder = {},
                initialEditMode = initialEditMode,
                desktopDensity = desktopDensity,
            )
        }
    }

    private fun duplicateEntries(): List<PlaylistEntry> =
        listOf(
            PlaylistEntry("entry-1", "playlist", "t-1", 0, 1),
            PlaylistEntry("entry-2", "playlist", "t-1", 1, 2),
        )

    private fun playableTrack(): PlayableTrack =
        PlayableTrack(
            id = "t-1",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000,
            source = AudioSource.FilePath("/t-1"),
        )

    private fun silentPublisher(): PlaylistFeatureDismissalPublisher =
        object : PlaylistFeatureDismissalPublisher {
            override fun publish(
                dismissal: PlaylistFeatureDismissal?,
                dispatch:
                    (
                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
            ): () -> Unit = {}
        }
}
