package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.Playlist
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.nowplaying.NowPlayingBarRootTestTag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlaylistEditModeSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailReportsActualLazyListScrollPositionAndUsesMeasuredFinalClearanceWithoutLocalBar() =
        runComposeUiTest {
            lateinit var listState: LazyListState
            lateinit var scope: CoroutineScope
            var expectedClearance = 0.dp
            val reportedPositions = mutableListOf<LibraryScrollPosition>()
            setContent {
                listState = rememberLazyListState()
                scope = rememberCoroutineScope()
                expectedClearance =
                    with(LocalDensity.current) {
                        activeBottomBarClearancePx(
                                LibraryBottomBarContent.NowPlaying,
                                LibraryBottomBarMeasurement(
                                    LibraryBottomBarContent.NowPlaying, 73),
                            )
                            .toDp()
                    }
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries =
                        List(12) { index ->
                            entry("entry-$index", "track-$index", index)
                        },
                    libraryTracks =
                        List(12) { index ->
                            libraryTrack(
                                "track-$index",
                                "Song $index",
                                "Artist $index",
                                "Album $index")
                        },
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    bottomContentPadding = expectedClearance,
                    listState = listState,
                    onScrollPositionChanged = { reportedPositions += it },
                )
            }
            scope.launch { listState.scrollToItem(6, 17) }
            waitUntil {
                reportedPositions.lastOrNull() ==
                    listState.toLibraryScrollPosition() &&
                    listState.firstVisibleItemIndex > 0
            }

            assertEquals(
                listState.toLibraryScrollPosition(), reportedPositions.last())
            scope.launch { listState.scrollToItem(13) }
            waitForIdle()
            onNode(
                    hasTestTag("playlist-bottom-clearance"),
                    useUnmergedTree = true)
                .assertHeightIsEqualTo(expectedClearance)
            onNode(hasTestTag(NowPlayingBarRootTestTag), useUnmergedTree = true)
                .assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun defaultPlaylistRowMatchesTrackContentAndHidesMutationActions() =
        runComposeUiTest {
            var playCount = 0
            var selectedOccurrence: String? = null
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a",
                                title = "Song A",
                                artist = "Artist A",
                                album = "Album A")),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {
                        playCount++
                        selectedOccurrence = it.selectedOccurrenceId
                    },
                    onRemoveEntry = {},
                    onReorder = {},
                    bottomContentPadding = 0.dp,
                )
            }

            onNode(
                    hasContentDescription("Song A, Artist A, Album A, 3:12"),
                    useUnmergedTree = true)
                .assertExists()
                .performClick()
            assertEquals(1, playCount)
            assertEquals("entry-a", selectedOccurrence)
            onNode(hasContentDescription("Move up Song A")).assertDoesNotExist()
            onNode(hasContentDescription("Move down Song A"))
                .assertDoesNotExist()
            onNode(hasContentDescription("Remove Song A")).assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun longClickEntersPageWideEditAndRowsConsumeClicksWithAccessibleBoundaryControls() =
        runComposeUiTest {
            var playCount = 0
            val reorderCalls = mutableListOf<List<String>>()
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries =
                        listOf(
                            entry("entry-a", "track-a", 0),
                            entry("entry-b", "track-b", 1)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A"),
                            libraryTrack(
                                "track-b", "Song B", "Artist B", "Album B"),
                        ),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = { playCount++ },
                    onRemoveEntry = {},
                    onReorder = { reorderCalls += it },
                )
            }

            val firstRow =
                onNode(
                    hasContentDescription("Song A, Artist A, Album A, 3:12"),
                    useUnmergedTree = true)
            firstRow.performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()

            onNode(
                    hasContentDescription("从播放列表中移除 Song A"),
                    useUnmergedTree = true)
                .assertWidthIsAtLeast(44.dp)
                .assertHeightIsAtLeast(44.dp)
            onNode(
                    hasContentDescription("将 Song A 上移") and hasClickAction(),
                    useUnmergedTree = true)
                .assertDoesNotExist()
            onNode(
                    hasContentDescription("将 Song B 下移") and hasClickAction(),
                    useUnmergedTree = true)
                .assertDoesNotExist()
            onNode(hasContentDescription("将 Song A 下移"), useUnmergedTree = true)
                .assertWidthIsAtLeast(44.dp)
                .assertHeightIsAtLeast(44.dp)
                .performClick()
            waitForIdle()
            assertEquals(listOf(listOf("entry-b", "entry-a")), reorderCalls)

            firstRow.performClick()
            assertEquals(0, playCount)
            onNode(
                    hasContentDescription("从播放列表中移除 Song B"),
                    useUnmergedTree = true)
                .assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun removeControlTargetsExactDuplicateOnceAndConfirmationKeepsEditActive() =
        runComposeUiTest {
            val removed = mutableListOf<String>()
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries =
                        listOf(
                            entry("entry-a", "track-a", 0),
                            entry("entry-b", "track-a", 1)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = { removed += it },
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                )
            }

            onAllNodes(
                    hasContentDescription("从播放列表中移除 Song A"),
                    useUnmergedTree = true)
                .onLast()
                .performClick()
            waitForIdle()
            onAllNodes(hasText("从播放列表中移除 Song A"), useUnmergedTree = true)
                .onLast()
                .performClick()
            waitForIdle()

            assertEquals(listOf("entry-b"), removed)
            onAllNodes(hasText("×"), useUnmergedTree = true)
                .onFirst()
                .assertExists()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun actionHeaderFirstPhysicalTapClearsAndConsumesThenNextTapActsWhileBlankViewportDoesNotClear() =
        runComposeUiTest {
            var browserOpens = 0
            var routeBacks = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = { routeBacks++ },
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = { browserOpens++ },
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                )
            }

            onNode(hasTestTag("playlist-list-viewport"), useUnmergedTree = true)
                .performTouchInput {
                    click(Offset(8f, height - 8f))
                }
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertExists()

            onNode(hasTestTag("playlist-action-header"), useUnmergedTree = true)
                .performTouchInput { click() }
            waitForIdle()
            assertEquals(0, browserOpens)
            onNode(hasText("×"), useUnmergedTree = true).assertDoesNotExist()

            onAllNodes(hasContentDescription("添加曲目"), useUnmergedTree = true)
                .onFirst()
                .performTouchInput { click() }
            waitForIdle()
            assertEquals(1, browserOpens)

            onNode(
                    hasContentDescription("Song A, Artist A, Album A, 3:12"),
                    useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            assertEquals(1, routeBacks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolbarTitleTapExitsEditWithoutNavigatingWhileBackStillUsesShellDispatcher() =
        runComposeUiTest {
            var routeBacks = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = { routeBacks++ },
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }

            val row =
                onNode(
                    hasContentDescription("Song A, Artist A, Album A, 3:12"),
                    useUnmergedTree = true)
            row.performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertExists()

            onNode(hasTestTag("playlist-toolbar-title"), useUnmergedTree = true)
                .performTouchInput { click() }
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertDoesNotExist()
            assertEquals(0, routeBacks)

            row.performSemanticsAction(SemanticsActions.OnLongClick)
            waitForIdle()
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performTouchInput { click() }
            assertEquals(1, routeBacks)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactEditRowKeepsMetadataWideWhileMovingMutationControlsToASeparateRail() =
        runComposeUiTest {
            setContent {
                Box(Modifier.size(360.dp, 700.dp)) {
                    PlaylistDetailScreen(
                        playlist = playlist("playlist-1", "Saved"),
                        entries = listOf(entry("entry-a", "track-a", 0)),
                        libraryTracks =
                            listOf(
                                libraryTrack(
                                    "track-a",
                                    "A deliberately long song title",
                                    "A fully visible artist",
                                    "A readable album",
                                ),
                            ),
                        state = PlaylistState(),
                        onBack = {},
                        onRetry = {},
                        onRename = { _, _ -> },
                        onDelete = {},
                        onOpenBrowser = {},
                        onPlayEntry = {},
                        onRemoveEntry = {},
                        onReorder = {},
                        rowMode = PlaylistDetailRowMode.Edit,
                    )
                }
            }

            onNode(
                    hasTestTag("playlist-entry-metadata-entry-a"),
                    useUnmergedTree = true)
                .assertWidthIsAtLeast(100.dp)
            onNode(
                    hasTestTag("playlist-entry-action-rail-entry-a"),
                    useUnmergedTree = true)
                .assertWidthIsAtLeast(132.dp)
            onNode(
                    hasContentDescription(
                        "从播放列表中移除 A deliberately long song title"),
                    useUnmergedTree = true)
                .assertWidthIsAtLeast(44.dp)
                .assertHeightIsAtLeast(44.dp)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun editHeaderExposesOnlyExitEditingSemanticsUntilItsFirstActivationIsConsumed() =
        runComposeUiTest {
            var browserOpens = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = { browserOpens++ },
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                )
            }

            onNode(hasContentDescription("添加曲目")).assertDoesNotExist()
            onNode(hasContentDescription("退出播放列表编辑"))
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            assertEquals(0, browserOpens)

            onAllNodes(hasContentDescription("添加曲目")).onFirst().performClick()
            waitForIdle()
            assertEquals(1, browserOpens)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun successfulDeleteUsesExactDisplayedDestinationInvalidationWithoutStaleRecovery() =
        runComposeUiTest {
            val appState = LibraryAppState(null)
            appState.pushRoute(LibraryRoute.PlaylistHub)
            val originalHubEntry = appState.navigation.currentEntry
            appState.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
            val entry = appState.navigation.currentEntry
            var deleteCount = 0
            var recoverableMessages = 0
            var clearSelectionCalls = 0
            val orchestrator =
                PlaylistDetailRouteOrchestrator(
                    appState = appState,
                    clearSelection = { clearSelectionCalls++ },
                    onPlaylistStateAction = { action ->
                        if (action
                            is PlaylistStateAction.ShowRecoverableMessage) {
                            recoverableMessages++
                        }
                    },
                )
            setContent {
                PlaylistDetailRouteContent(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = emptyList(),
                    libraryTracks = emptyList(),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDeleteMutation = { completion ->
                        deleteCount++
                        completion(
                            PlaylistStateAction.SnapshotConfirmed(
                                PlaylistSnapshot()))
                    },
                    onDisplayedPlaylistDeleteConfirmed =
                        orchestrator.displayedPlaylistDeleteCompletion(entry),
                    destinationId = entry.destinationId,
                    registerBackSurface = appState::registerBackSurface,
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }

            onNode(hasText("删除播放列表"), useUnmergedTree = true).performClick()
            waitForIdle()
            onAllNodes(hasText("删除播放列表"), useUnmergedTree = true)
                .onLast()
                .performClick()
            waitForIdle()

            assertEquals(1, deleteCount)
            assertEquals(0, recoverableMessages)
            assertEquals(0, clearSelectionCalls)
            assertEquals(LibraryRoute.PlaylistHub, appState.navigation.current)
            assertEquals(
                listOf(LibraryRoute.Home, LibraryRoute.PlaylistHub),
                appState.navigation.routes,
            )
            assertEquals(originalHubEntry, appState.navigation.currentEntry)
            assertEquals(
                LibraryNavigationTransition.Pop,
                appState.lastNavigationTransition)
            // Replaying the finished route callback cannot create a second
            // departure.
            orchestrator.displayedPlaylistDeleteCompletion(entry)(
                PlaylistSnapshot())
            assertEquals(
                listOf(LibraryRoute.Home, LibraryRoute.PlaylistHub),
                appState.navigation.routes,
            )
        }

    @Test
    fun displayedDeletionPreservesSelectionWhileStaleRecoveryClearsTheSameSelection() {
        val appState = LibraryAppState(null)
        appState.pushRoute(LibraryRoute.PlaylistHub)
        appState.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
        val deletedEntry = appState.navigation.currentEntry
        var selection =
            TrackSelectionState(
                pageKey = TrackSelectionPageKey.HomeSongs,
                selectedTrackIds = setOf("unrelated-track"),
            )
        var recoverableMessages = 0
        val orchestrator =
            PlaylistDetailRouteOrchestrator(
                appState = appState,
                clearSelection = { selection = TrackSelectionState() },
                onPlaylistStateAction = { action ->
                    if (action is PlaylistStateAction.ShowRecoverableMessage) {
                        recoverableMessages++
                    }
                },
            )

        orchestrator.completeDisplayedPlaylistDeletion(
            entry = deletedEntry,
            confirmedSnapshot = PlaylistSnapshot(),
        )

        assertEquals(
            TrackSelectionState(
                pageKey = TrackSelectionPageKey.HomeSongs,
                selectedTrackIds = setOf("unrelated-track"),
            ),
            selection,
        )
        assertEquals(LibraryRoute.PlaylistHub, appState.navigation.current)

        appState.pushRoute(LibraryRoute.PlaylistDetail("missing-playlist"))
        orchestrator.recoverStalePlaylistDetail(
            "Playlist is no longer available")

        assertEquals(TrackSelectionState(), selection)
        assertEquals(1, recoverableMessages)
        assertEquals(LibraryRoute.PlaylistHub, appState.navigation.current)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun confirmedDisplayedDeletionLeavesTheActualRouteResolutionSeamBeforeStaleRecovery() =
        runComposeUiTest {
            val appState = LibraryAppState(null)
            appState.pushRoute(LibraryRoute.PlaylistHub)
            appState.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
            val entry = appState.navigation.currentEntry
            var recoveries = 0
            var messages = 0
            var hubEntries = 0
            val orchestrator =
                PlaylistDetailRouteOrchestrator(
                    appState = appState,
                    clearSelection = {
                        error(
                            "exact invalidation must preserve unrelated selection")
                    },
                    onPlaylistStateAction = { action ->
                        if (action
                            is PlaylistStateAction.ShowRecoverableMessage)
                            messages++
                    },
                )
            var publishedState by
                mutableStateOf(
                    PlaylistState(
                        confirmedSnapshot =
                            PlaylistSnapshot(
                                playlists =
                                    listOf(playlist("playlist-1", "Saved"))),
                        hasConfirmedSnapshot = true,
                    ),
                )
            setContent {
                when (val route = appState.navigation.current) {
                    is LibraryRoute.PlaylistDetail ->
                        PlaylistDetailRouteResolutionEffect(
                            route = route,
                            state = publishedState,
                            onRecoverStalePlaylistDetail = {
                                recoveries++
                                orchestrator.recoverStalePlaylistDetail(it)
                            },
                        )

                    LibraryRoute.PlaylistHub -> hubEntries++
                    else -> Unit
                }
            }
            waitForIdle()

            publishedState =
                publishedState.copy(confirmedSnapshot = PlaylistSnapshot())
            orchestrator.completeDisplayedPlaylistDeletion(
                entry, publishedState.confirmedSnapshot)
            waitForIdle()

            assertEquals(LibraryRoute.PlaylistHub, appState.navigation.current)
            assertEquals(1, hubEntries)
            assertEquals(0, recoveries)
            assertEquals(0, messages)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun failedDeleteRetainsTheDisplayedDetailAndItsConfirmation() =
        runComposeUiTest {
            val appState = LibraryAppState(null)
            appState.pushRoute(LibraryRoute.PlaylistDetail("playlist-1"))
            var deleteCount = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = emptyList(),
                    libraryTracks = emptyList(),
                    state = PlaylistState(),
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = { completion ->
                        deleteCount++
                        completion(
                            PlaylistStateAction.MutationFailed("failure"))
                    },
                    onDeleteConfirmed = {
                        error(
                            "failed deletion must not invalidate the displayed route")
                    },
                    destinationId = appState.activeDestinationId,
                    registerBackSurface = appState::registerBackSurface,
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }

            onNode(hasText("删除播放列表"), useUnmergedTree = true).performClick()
            waitForIdle()
            onAllNodes(hasText("删除播放列表"), useUnmergedTree = true)
                .onLast()
                .performClick()
            waitForIdle()

            assertEquals(1, deleteCount)
            assertEquals(
                LibraryRoute.PlaylistDetail("playlist-1"),
                appState.navigation.current)
            onAllNodes(hasText("删除播放列表"), useUnmergedTree = true)
                .onLast()
                .assertExists()
        }

    private fun playlist(id: String, name: String) = Playlist(id, name, 1L, 1L)

    private fun entry(id: String, trackId: String, position: Int) =
        PlaylistEntry(id, "playlist-1", trackId, position, 1L)

    private fun libraryTrack(
        id: String,
        title: String,
        artist: String,
        album: String
    ) =
        LibraryTrack(
            id = id,
            sourceId = "source-1",
            sourceLocalKey = "$id.mp3",
            audioSource = AudioSource.FilePath("/$id.mp3"),
            displayName = title,
            title = title,
            artist = artist,
            album = album,
            durationMillis = 192_000L,
            sizeBytes = 1L,
            modifiedAtEpochMillis = 1L,
            lastSeenScanId = null,
            createdAtEpochMillis = 1L,
            updatedAtEpochMillis = 1L,
        )
}
