package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlin.test.assertNotNull
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
    fun registeredEditClearChangesProductionRowsAndUnregisters() =
        runComposeUiTest {
            var clear: (() -> Unit)? = null
            var unregisterCount = 0
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
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                    registerPlaylistEditMode = { _, callback ->
                        clear = callback;
                        { unregisterCount++ }
                    },
                )
            }
            onNode(
                    hasContentDescription("Song A, Artist A, Album A, 3:12"),
                    useUnmergedTree = true)
                .assertExists()
            onAllNodes(hasText("×"), useUnmergedTree = true)
                .onFirst()
                .assertExists()
            assertNotNull(clear).invoke()
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertDoesNotExist()
            assertEquals(1, unregisterCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun registeredModalDismissDoesNotTriggerPlaybackOrRouteCallbackWhenNoModalIsOwned() =
        runComposeUiTest {
            var registeredDismiss: (() -> Unit)? = null
            var playCount = 0
            var backCount = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = { backCount++ },
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = { playCount++ },
                    onRemoveEntry = {},
                    onReorder = {},
                    registerPlaylistModalDismiss = { _, callback ->
                        registeredDismiss = callback;
                        {}
                    },
                )
            }
            waitForIdle()
            assertEquals(null, registeredDismiss)
            assertEquals(0, playCount)
            assertEquals(0, backCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun realRenameModalDismissesThroughRegisteredToolbarBackWithoutSideEffects() =
        runComposeUiTest {
            var dismiss: (() -> Unit)? = null
            var playCount = 0
            var routeBackCount = 0
            val registration = PlaylistBackRegistrationState()
            val dispatcher =
                PlaylistBackDispatchController(
                    registration = registration,
                    selectionState = { TrackSelectionState() },
                    isNowPlayingExpanded = { false },
                    canPopRoute = { true },
                    cancelSelection = {},
                    hideNowPlaying = {},
                    directPopRoute = { routeBackCount++ },
                )
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = dispatcher::dispatch,
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = { playCount++ },
                    onRemoveEntry = {},
                    onReorder = {},
                    registerPlaylistModalDismiss = { owner, callback ->
                        dismiss = callback
                        registration.registerModal(owner) {
                            callback?.invoke()
                            dismiss = null
                        }
                    },
                )
            }
            onAllNodes(hasContentDescription("重命名播放列表"), useUnmergedTree = true)
                .onFirst()
                .performClick()
            waitForIdle()
            assertNotNull(dismiss)
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            onNode(hasText("Saved"), useUnmergedTree = true).assertExists()
            assertEquals(0, playCount)
            assertEquals(0, routeBackCount)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun realToolbarBackDismissesModalThenLeavesEditModeAndLaterBranchesUseShippingCallbacks() =
        runComposeUiTest {
            val registration = PlaylistBackRegistrationState()
            var selection = TrackSelectionState()
            var nowPlaying = false
            var playCount = 0
            var routePops = 0
            var ordinaryBackCalls = 0
            val callbacks =
                libraryBackCallbacks(
                    ordinaryBack = {
                        ordinaryBackCalls++
                        PlaylistBackDispatchController(
                                registration = registration,
                                selectionState = { selection },
                                isNowPlayingExpanded = { nowPlaying },
                                canPopRoute = { true },
                                cancelSelection = {
                                    selection = TrackSelectionState()
                                },
                                hideNowPlaying = { nowPlaying = false },
                                directPopRoute = { routePops++ },
                            )
                            .dispatch()
                    },
                    decision = {
                        registration.decision(selection, nowPlaying, true)
                    },
                    transitionProgress = { null },
                    setCompletionProgress = {},
                    navigationPop = { LibraryNavigationStack() },
                    completePredictivePop = {},
                    clearSelection = { selection = TrackSelectionState() },
                    popRoute = { routePops++ },
                )
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = callbacks.ordinaryBack,
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = { playCount++ },
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                    registerPlaylistEditMode = { owner, clear ->
                        registration.registerEdit(owner, clear)
                    },
                    registerPlaylistModalDismiss = { owner, dismiss ->
                        dismiss?.let { registration.registerModal(owner, it) }
                            ?: {}
                    },
                )
            }
            onNode(hasText("×"), useUnmergedTree = true).assertExists()
            onNode(
                    hasContentDescription("从播放列表中移除 Song A"),
                    useUnmergedTree = true)
                .performClick()
            waitForIdle()
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertExists()
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertDoesNotExist()

            selection =
                TrackSelectionState(
                    TrackSelectionPageKey.HomeSongs, setOf("track-a"))
            val ordinaryBackBeforeBranches = ordinaryBackCalls
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            assertEquals(0, playCount)
            assertEquals(0, routePops)
            nowPlaying = true
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            assertEquals(0, routePops)
            onNode(hasTestTag("playlist-back"), useUnmergedTree = true)
                .performClick()
            waitForIdle()
            assertEquals(1, routePops)
            assertEquals(3, ordinaryBackCalls - ordinaryBackBeforeBranches)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun productionSystemBackCallbackDismissesRealModalBeforeRoutePop() =
        runComposeUiTest {
            var routePops = 0
            var dismissals = 0
            val registration = PlaylistBackRegistrationState()
            var ordinaryBackCalls = 0
            val callbacks =
                libraryBackCallbacks(
                    ordinaryBack = {
                        ordinaryBackCalls++
                        PlaylistBackDispatchController(
                                registration = registration,
                                selectionState = { TrackSelectionState() },
                                isNowPlayingExpanded = { false },
                                canPopRoute = { true },
                                cancelSelection = {},
                                hideNowPlaying = {},
                                directPopRoute = { routePops++ },
                            )
                            .dispatch()
                    },
                    decision = {
                        registration.decision(
                            TrackSelectionState(), false, true)
                    },
                    transitionProgress = {
                        error("modal/edit back must not be predictive")
                    },
                    setCompletionProgress = {
                        error("modal/edit back must not be predictive")
                    },
                    navigationPop = {
                        error("modal/edit back must not pop predictively")
                    },
                    completePredictivePop = {
                        error("modal/edit back must not complete predictively")
                    },
                    clearSelection = {},
                    popRoute = { routePops++ },
                )
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = callbacks.ordinaryBack,
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    rowMode = PlaylistDetailRowMode.Edit,
                    registerPlaylistEditMode = { owner, clear ->
                        registration.registerEdit(owner, clear)
                    },
                    registerPlaylistModalDismiss = { owner, callback ->
                        callback?.let {
                            registration.registerModal(owner) {
                                dismissals++
                                it()
                            }
                        } ?: {}
                    },
                )
            }
            onNode(
                    hasContentDescription("从播放列表中移除 Song A"),
                    useUnmergedTree = true)
                .performClick()
            waitForIdle()
            callbacks.systemBackCompleted()
            waitForIdle()
            assertEquals(1, dismissals)
            assertEquals(0, routePops)
            onNode(hasText("×"), useUnmergedTree = true).assertExists()
            callbacks.systemBackCompleted()
            waitForIdle()
            onNode(hasText("×"), useUnmergedTree = true).assertDoesNotExist()
            assertEquals(2, ordinaryBackCalls)
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
    fun successfulDeleteUsesShippingCompletionWithModalPrecedenceAndDirectPop() =
        runComposeUiTest {
            var completionCount = 0
            var deleteCount = 0
            var clearCount = 0
            var popCount = 0
            var ordinaryBackCount = 0
            val registration = PlaylistBackRegistrationState()
            val callbacks =
                libraryBackCallbacks(
                    ordinaryBack = { ordinaryBackCount++ },
                    decision = { registration.decision() },
                    transitionProgress = { null },
                    setCompletionProgress = {},
                    navigationPop = { LibraryNavigationStack() },
                    completePredictivePop = {},
                    clearSelection = { clearCount++ },
                    popRoute = { popCount++ },
                )
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist("playlist-1", "Saved"),
                    entries = listOf(entry("entry-a", "track-a", 0)),
                    libraryTracks =
                        listOf(
                            libraryTrack(
                                "track-a", "Song A", "Artist A", "Album A")),
                    state = PlaylistState(),
                    onBack = callbacks.ordinaryBack,
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = { callback ->
                        deleteCount++
                        callback(
                            PlaylistStateAction.SnapshotConfirmed(
                                PlaylistSnapshot()))
                    },
                    onDeleteCompleted = {
                        completionCount++
                        callbacks.deleteCompleted()
                    },
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                    registerPlaylistModalDismiss = { owner, dismiss ->
                        dismiss?.let { registration.registerModal(owner, it) }
                            ?: {}
                    },
                )
            }
            onNode(hasText("删除播放列表"), useUnmergedTree = true).performClick()
            waitForIdle()
            onAllNodes(hasText("删除播放列表"), useUnmergedTree = true)
                .onLast()
                .performClick()
            waitForIdle()
            assertEquals(1, deleteCount)
            assertEquals(1, completionCount)
            assertEquals(1, clearCount)
            assertEquals(1, popCount)
            assertEquals(0, ordinaryBackCount)
            onNode(
                    hasText("Delete Saved? This cannot be undone."),
                    useUnmergedTree = true)
                .assertDoesNotExist()
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
