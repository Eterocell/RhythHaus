package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlin.collections.AbstractList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SearchRouteAdapterJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun orderedQueueAndSelectedTrackUseRealPlaybackSelection() {
        val controller = PlaybackController(FakePlaybackEngine())
        val results = listOf(track("first"), track("second"), track("first"))

        playSearchTrack(controller, results, results[1]) {}

        assertEquals(
            listOf("first", "second", "first"),
            controller.state.value.queue.map { it.track.id })
        assertEquals("second", controller.state.value.currentTrack?.id)
    }

    @Test
    fun currentTrackRestartsBeforeDismissal() {
        val controller = PlaybackController(FakePlaybackEngine())
        val selected = track("current")
        playSearchTrack(controller, listOf(selected), selected) {}
        controller.seekTo(42)
        var positionAtDismiss = -1L

        playSearchTrack(controller, listOf(selected), selected) {
            positionAtDismiss = controller.state.value.positionMillis
        }

        assertEquals(0L, positionAtDismiss)
    }

    @Test
    fun dismissesOnlyAfterSuccessfulSelection() {
        val controller = PlaybackController(FakePlaybackEngine())
        var dismisses = 0

        playSearchTrack(controller, listOf(track("one")), track("one")) {
            dismisses++
        }

        assertEquals(1, dismisses)
        assertEquals("one", controller.state.value.currentTrack?.id)
    }

    @Test
    fun sentinelFailurePropagatesAndDoesNotDismiss() {
        val sentinel = IllegalStateException("search mapping sentinel")
        val failingOrderedResults =
            object : AbstractList<LibraryTrack>() {
                override val size: Int = 1

                override fun get(index: Int): LibraryTrack {
                    check(index == 0)
                    throw sentinel
                }
            }
        var dismisses = 0
        val thrown = runCatching {
            playSearchTrack(
                PlaybackController(FakePlaybackEngine()),
                failingOrderedResults,
                track("selected")) {
                    dismisses++
                }
        }
            .exceptionOrNull()

        assertSame(sentinel, thrown)
        assertEquals(0, dismisses)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedLabelsUseStructuredFormatting() = runComposeUiTest {
        setContent { searchOverlay(TrackSelectionState(), mutableListOf()) }
        onNode(hasSetTextAction()).performTextInput("selected")
        waitForIdle()

        onNode(searchRow("song selected"), useUnmergedTree = true)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("选择曲目 song selected"),
                ),
            )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun equalizerSlotIsSharedOwned() = runComposeUiTest {
        val playbackState = mutableStateOf(playingState("selected"))
        setContent {
            searchOverlay(
                TrackSelectionState(),
                mutableListOf(),
                playbackState = playbackState.value)
        }
        onNode(hasSetTextAction()).performTextInput("selected")
        waitForIdle()
        onNodeWithTag("shared-search-equalizer", useUnmergedTree = true)
            .assertExists()

        playbackState.value =
            playbackState.value.copy(status = PlaybackStatus.Paused)
        waitForIdle()
        onNodeWithTag("shared-search-equalizer", useUnmergedTree = true)
            .assertDoesNotExist()

        playbackState.value = playingState("not-a-search-result")
        waitForIdle()
        onNodeWithTag("shared-search-equalizer", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun adaptsSelectionAndScrollFromProductionSearchContent() =
        runComposeUiTest {
            val selectionActions = mutableListOf<TrackSelectionAction>()
            val scrolls = mutableListOf<LibraryScrollPosition>()
            val state = mutableStateOf(TrackSelectionState())
            val unrelatedRecomposition = mutableStateOf(0)
            setContent {
                unrelatedRecomposition.value
                searchOverlay(
                    state.value,
                    selectionActions,
                    scrolls,
                    tracks = searchTracks())
            }
            waitForIdle()
            selectionActions.clear()
            onNode(hasSetTextAction()).performTextInput("song")
            waitForIdle()

            val visibleIds = searchTracks().map(LibraryTrack::id)
            assertEquals(
                listOf<TrackSelectionAction>(
                    TrackSelectionAction.ReconcileVisible(
                        TrackSelectionPageKey.Search, visibleIds),
                ),
                selectionActions,
            )
            unrelatedRecomposition.value++
            waitForIdle()
            assertEquals(
                listOf<TrackSelectionAction>(
                    TrackSelectionAction.ReconcileVisible(
                        TrackSelectionPageKey.Search, visibleIds),
                ),
                selectionActions,
            )

            onNode(searchRow("song selected"), useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()
            assertEquals(
                listOf(
                    TrackSelectionAction.ReconcileVisible(
                        TrackSelectionPageKey.Search, visibleIds),
                    TrackSelectionAction.Start(
                        TrackSelectionPageKey.Search, "selected"),
                ),
                selectionActions,
            )

            state.value =
                TrackSelectionState(
                    TrackSelectionPageKey.Search, setOf("selected"))
            waitForIdle()
            onNode(searchRow("song other"), useUnmergedTree = true)
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ToggleableState,
                        ToggleableState.Off,
                    ),
                )
            onNode(searchRow("song selected"), useUnmergedTree = true)
                .assertIsOn()
                .performClick()
            waitForIdle()
            assertEquals(
                TrackSelectionAction.Toggle(
                    TrackSelectionPageKey.Search, "selected"),
                selectionActions.last())

            onNode(hasScrollToIndexAction()).performScrollToIndex(20)
            waitForIdle()
            assertTrue(scrolls.contains(LibraryScrollPosition(20, 0)))
            onNode(hasScrollToIndexAction()).performTouchInput {
                swipe(center, center - Offset(0f, 20f))
            }
            waitForIdle()
            val partialScroll = scrolls.last { it.firstVisibleItemIndex == 20 }
            assertTrue(partialScroll.firstVisibleItemScrollOffset > 0)

            val actionCountBeforeInactiveInteraction = selectionActions.size
            onNode(hasScrollToIndexAction()).performScrollToIndex(0)
            state.value =
                TrackSelectionState(
                    TrackSelectionPageKey.HomeSongs, setOf("selected"))
            waitForIdle()
            onNode(searchRow("song selected"), useUnmergedTree = true)
                .assert(
                    SemanticsMatcher(
                        "Search row is not selectable outside Search state") {
                            node ->
                            node.config.getOrNull(
                                SemanticsProperties.ToggleableState) == null
                        },
                )
                .performClick()
            waitForIdle()
            assertEquals(
                actionCountBeforeInactiveInteraction, selectionActions.size)
        }

    @androidx.compose.runtime.Composable
    private fun searchOverlay(
        trackSelectionState: TrackSelectionState,
        actions: MutableList<TrackSelectionAction>,
        scrolls: MutableList<LibraryScrollPosition> = mutableListOf(),
        tracks: List<LibraryTrack> =
            listOf(track("selected"), track("duplicate"), track("duplicate")),
        playbackState: PlaybackState = PlaybackState(),
    ) {
        LibraryRouteOverlays(
            route = LibraryRoute.Search,
            snapshot = LibrarySnapshot("Library", "", emptyList(), null),
            libraryTracks = tracks,
            playbackController = PlaybackController(FakePlaybackEngine()),
            playbackState = playbackState,
            playlistRepository = EmptyPlaylistRepository,
            playlistState = PlaylistState(),
            playlistBackupState = PlaylistBackupUiState(),
            backupDocumentAvailable = false,
            destinationId = LibraryDestinationId(LibraryRoute.Search, "search"),
            playlistAppearanceSource =
                rememberPlaylistFeatureAppearanceSource(
                    PlaylistFeatureDestination("search")),
            registerBackSurface = { {} },
            onPlaylistStateAction = {},
            onRefreshPlaylists = {},
            onPlaylistMutation = { _, _ -> },
            onExportPlaylists = {},
            onOpenPlaylistBackup = {},
            onConfirmPlaylistBackup = {},
            onPlaylistBackupAction = {},
            sources = emptyList(),
            folderPickerLauncher = UnavailablePicker,
            sourcePickerActionVisible = false,
            importMessage = null,
            scanProgress = null,
            scanJob = null,
            currentThemeMode = RhythHausThemeMode.System,
            onThemeModeSelected = {},
            onClearLibrary = {},
            onRescanSource = {},
            onRemoveSource = {},
            onCancelScan = {},
            onShowSettingsAbout = {},
            onShowOpenSourceLibraries = {},
            onDismiss = {},
            onScrollPositionChanged = { scrolls += it },
            trackSelectionState = trackSelectionState,
            onTrackSelectionAction = { actions += it },
        )
    }

    private fun searchTracks() =
        listOf(track("selected"), track("other")) +
            List(30) { track("duplicate-$it").copy(title = "song duplicate") } +
            listOf(
                track("duplicate").copy(title = "song duplicate"),
                track("duplicate").copy(title = "song duplicate"))

    private fun playingState(trackId: String) =
        PlaybackState(
            currentOccurrenceId = "current",
            queue =
                listOf(
                    QueueOccurrence(
                        "current", track(trackId).toPlayableTrack())),
            status = PlaybackStatus.Playing,
        )

    private fun searchRow(title: String) =
        SemanticsMatcher.expectValue(
            SemanticsProperties.ContentDescription,
            listOf("选择曲目 $title"),
        )

    private fun track(id: String) =
        LibraryTrack(
            id,
            "source",
            id,
            AudioSource.FilePath(id),
            id,
            "song $id",
            "Artist",
            "Album",
            1,
            null,
            null,
            "scan",
            1,
            1)

    private object UnavailablePicker : PlatformFolderPickerLauncher {
        override val isAvailable = false
        override val supportsAdditionalSources = false

        override fun launch() = Unit
    }

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
