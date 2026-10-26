package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import java.util.Base64
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistArtworkJvmTest {
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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailRowResolvesLazyArtworkByTrackIdAndRendersImage() =
        runComposeUiTest {
            var loadCalls = 0
            setContent {
                Box(Modifier.size(420.dp, 900.dp)) {
                    PlaylistDetailScreen(
                        playlist = PlaylistSummary("playlist", "Saved", 1, 1),
                        entries =
                            listOf(
                                PlaylistEntry(
                                    "entry", "playlist", "t-1", 0, 1)),
                        playableTracksById =
                            mapOf("t-1" to playableTrack(artworkBytes = null)),
                        artworkLoader = { id ->
                            loadCalls++
                            if (id == "t-1") artworkBytes() else null
                        },
                        state = detailState(),
                        destination =
                            PlaylistFeatureDestination("detail-artwork"),
                        appearanceSource = appearanceSource(),
                        dismissalPublisher = silentPublisher(),
                        mutationFailedLabel = "Could not save playlist changes",
                        onBack = {},
                        onRetry = {},
                        onRename = { _, _ -> },
                        onDelete = {},
                        onDeleteConfirmed = {},
                        onOpenBrowser = {},
                        onPlayEntry = {},
                        onRemoveEntry = {},
                        onReorder = {},
                    )
                }
            }
            waitForIdle()

            waitUntil(timeoutMillis = 5_000) {
                onAllNodes(hasText("P")).fetchSemanticsNodes().isEmpty()
            }
            assertEquals(1, loadCalls)
            onAllNodes(hasText("P")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailRowKeepsFallbackWhenArtworkUnavailable() = runComposeUiTest {
        var loadCalls = 0
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                PlaylistDetailScreen(
                    playlist = PlaylistSummary("playlist", "Saved", 1, 1),
                    entries =
                        listOf(PlaylistEntry("entry", "playlist", "t-1", 0, 1)),
                    playableTracksById =
                        mapOf("t-1" to playableTrack(artworkBytes = null)),
                    artworkLoader = { _ ->
                        loadCalls++
                        null
                    },
                    state = detailState(),
                    destination = PlaylistFeatureDestination("detail-artwork"),
                    appearanceSource = appearanceSource(),
                    dismissalPublisher = silentPublisher(),
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) { loadCalls == 1 }
        waitForIdle()
        onAllNodes(hasText("P")).assertCountEquals(1)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailRowUsesEagerArtworkWithoutCallingLoader() = runComposeUiTest {
        var loadCalls = 0
        setContent {
            Box(Modifier.size(420.dp, 900.dp)) {
                PlaylistDetailScreen(
                    playlist = PlaylistSummary("playlist", "Saved", 1, 1),
                    entries =
                        listOf(PlaylistEntry("entry", "playlist", "t-1", 0, 1)),
                    playableTracksById =
                        mapOf("t-1" to playableTrack(artworkBytes())),
                    artworkLoader = { _ ->
                        loadCalls++
                        error("loader must not run with eager artwork")
                    },
                    state = detailState(),
                    destination = PlaylistFeatureDestination("detail-artwork"),
                    appearanceSource = appearanceSource(),
                    dismissalPublisher = silentPublisher(),
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }
        }
        waitForIdle()

        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasText("P")).fetchSemanticsNodes().isEmpty()
        }
        assertEquals(0, loadCalls)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queueRowResolvesLazyArtworkByTrackId() = runComposeUiTest {
        var loadCalls = 0
        val queueTrack =
            PlayableTrack(
                id = "queue-track",
                title = "Queue track",
                artist = "Artist",
                album = "Album",
                durationMillis = 1_000,
                source = AudioSource.FilePath("/queue-track"),
            )
        val playbackState =
            PlaybackState(
                currentOccurrenceId = "current",
                queue = listOf(QueueOccurrence("current", queueTrack)),
            )
        val destination = PlaylistFeatureDestination("queue-artwork")
        setContent {
            PlaybackQueueContent(
                playbackState = playbackState,
                destination = destination,
                appearanceSource =
                    rememberPlaylistFeatureAppearanceSource(destination),
                dismissalPublisher = silentPublisher(),
                modifier = Modifier.testTag("queue-artwork-surface"),
                onClose = {},
                onSaveQueueAsPlaylist = { _, _, _ -> },
                artworkLoader = { id ->
                    loadCalls++
                    if (id == "queue-track") artworkBytes() else null
                },
                onReorderUpcoming = { _, _ ->
                    QueueMutationFeedback(playbackState, false)
                },
                onRemoveUpcoming = {
                    QueueMutationFeedback(playbackState, false)
                },
                onClearUpcoming = {
                    QueueMutationFeedback(playbackState, false)
                },
            )
        }
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasContentDescription("Queue track"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        assertEquals(1, loadCalls)
    }

    private fun detailState(): PlaylistState =
        PlaylistState(
            confirmedSnapshot =
                PlaylistSnapshot(
                    playlists =
                        listOf(PlaylistSummary("playlist", "Saved", 1, 1))),
            hasConfirmedSnapshot = true,
        )

    @Composable
    private fun appearanceSource() =
        rememberPlaylistFeatureAppearanceSource(
            PlaylistFeatureDestination("artwork-appearance"))

    private fun silentPublisher(): PlaylistFeatureDismissalPublisher =
        object : PlaylistFeatureDismissalPublisher {
            override fun publish(
                dismissal: PlaylistFeatureDismissal?,
                dispatch:
                    (
                        PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
            ): () -> Unit = {}
        }

    private fun playableTrack(artworkBytes: ByteArray?): PlayableTrack =
        PlayableTrack(
            id = "t-1",
            title = "Playlist Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000,
            source = AudioSource.FilePath("/t-1"),
            artworkBytes = artworkBytes,
        )

    private fun artworkBytes(): ByteArray =
        Base64.getDecoder()
            .decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
}
