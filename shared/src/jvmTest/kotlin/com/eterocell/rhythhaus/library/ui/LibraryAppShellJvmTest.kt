package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.SleepTimerMode
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import com.eterocell.rhythhaus.library.TrackMetadataEditorData
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.onboarding.OnboardingBackTestTag
import com.eterocell.rhythhaus.onboarding.OnboardingCloseTestTag
import com.eterocell.rhythhaus.onboarding.OnboardingFinishTestTag
import com.eterocell.rhythhaus.onboarding.OnboardingNextTestTag
import com.eterocell.rhythhaus.onboarding.OnboardingRootTestTag
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupCounts
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentLauncher
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentOpenResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupDocumentSaveResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupImportConfirmation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupPreview
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuard
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupRevisionGuardResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiError
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.playlistbackup.PlaylistDocumentFormat
import com.eterocell.rhythhaus.playlistbackup.createPlaylistBackupController
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.toPlayableTrack
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class LibraryAppShellJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun routeTransitionInsetsRemainStableThroughPushPopAndCancellation() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest(
                effectContext = TestMotionDurationScale(scaleFactor = 1f),
            ) {
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 390.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "transition-geometry",
                                "source",
                                ScanStatus.Completed,
                                1),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                    )
                    val content = onNodeWithTag(AppShellContentRegionTestTag)
                    val settledBounds =
                        content.fetchSemanticsNode().boundsInRoot
                    val settingsAction = onNodeWithTag(AppSettingsTopBarTestTag)
                    val settledSettingsBounds =
                        settingsAction.fetchSemanticsNode().boundsInRoot
                    onNodeWithTag("AppShellHeaderTitle")
                        .assertTextContains("Library")
                    mainClock.autoAdvance = false
                    onNodeWithTag(AppNavigationSearchTestTag).performClick()
                    repeat(16) {
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                        assertEquals(
                            settledBounds,
                            content.fetchSemanticsNode().boundsInRoot,
                            "Push frame $it changed the shell content rectangle")
                        assertEquals(
                            settledSettingsBounds,
                            settingsAction.fetchSemanticsNode().boundsInRoot,
                            "Push frame $it changed global Settings chrome")
                    }
                    onNode(hasSetTextAction())
                        .performTextInput("Retained query")
                    input.backStarted(NavigationEvent())
                    mainClock.advanceTimeBy(64)
                    waitForIdle()
                    assertEquals(
                        settledBounds,
                        content.fetchSemanticsNode().boundsInRoot)
                    assertEquals(
                        settledSettingsBounds,
                        settingsAction.fetchSemanticsNode().boundsInRoot)
                    input.backCancelled()
                    mainClock.advanceTimeBy(256)
                    waitForIdle()
                    onNodeWithText("Retained query").assertIsDisplayed()
                    assertEquals(
                        settledBounds,
                        content.fetchSemanticsNode().boundsInRoot)
                    assertEquals(
                        settledSettingsBounds,
                        settingsAction.fetchSemanticsNode().boundsInRoot)
                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    repeat(16) {
                        mainClock.advanceTimeByFrame()
                        waitForIdle()
                        assertEquals(
                            settledBounds,
                            content.fetchSemanticsNode().boundsInRoot,
                            "Pop frame $it changed the shell content rectangle")
                        assertEquals(
                            settledSettingsBounds,
                            settingsAction.fetchSemanticsNode().boundsInRoot,
                            "Pop frame $it changed global Settings chrome")
                    }
                    onNodeWithTag("AppShellHeaderTitle")
                        .assertTextContains("Library")
                    onNodeWithTag(AppNavigationLibraryTestTag)
                        .assertIsSelected()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun controlsDismissalStartsOnArtworkAndUnusedSurface() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        "controls-drag",
                        "Controls drag",
                        "Artist",
                        "Album",
                        180,
                        TrackAccent(0xFF123456, 0xFF654321),
                        AudioSource.FilePath("/controls-drag.mp3"),
                        artworkBytes =
                            java.util.Base64.getDecoder()
                                .decode(
                                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jD1kAAAAASUVORK5CYII="),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                try {
                    runBlocking {
                        controller.setOccurrenceQueue(
                            listOf(
                                QueueOccurrence(
                                    "current", track.toPlayableTrack()),
                                QueueOccurrence(
                                    "duplicate", track.toPlayableTrack()),
                            ),
                            "current",
                        )
                    }
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    mount(
                        width = 390.dp,
                        height = 768.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "controls-drag-scan",
                                "source",
                                ScanStatus.Completed,
                                1),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )
                    onNodeWithTag(AppNavigationSearchTestTag).performClick()
                    onNode(hasSetTextAction()).performTextInput("Controls")
                    val before = controller.state.value
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()

                    fun dismissFrom(start: Offset) {
                        onNodeWithTag("NowPlayingContentRoot")
                            .performTouchInput {
                                swipe(
                                    start,
                                    Offset(start.x, height - 8f),
                                    durationMillis = 600,
                                )
                            }
                        waitForIdle()
                        onNodeWithTag("NowPlayingContentRoot")
                            .assertDoesNotExist()
                        onNodeWithTag("NowPlayingShellPlacement")
                            .assertIsDisplayed()
                        onNodeWithTag(AppNavigationSearchTestTag)
                            .assertIsSelected()
                        onNodeWithText("Controls", substring = false)
                            .assertIsDisplayed()
                        assertEquals(
                            before,
                            controller.state.value,
                            "Dismissing Controls must preserve the selected track, playback state, and duplicate queue occurrences",
                        )
                    }

                    val playerBounds =
                        onNodeWithTag("NowPlayingContentRoot")
                            .fetchSemanticsNode()
                            .boundsInRoot
                    val artworkBounds =
                        onNodeWithContentDescription("Album artwork")
                            .fetchSemanticsNode()
                            .boundsInRoot
                    dismissFrom(
                        artworkBounds.center - playerBounds.topLeft,
                    )

                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()
                    val reopenedPlayerBounds =
                        onNodeWithTag("NowPlayingContentRoot")
                            .fetchSemanticsNode()
                            .boundsInRoot
                    dismissFrom(
                        Offset(
                            x = reopenedPlayerBounds.width / 2f,
                            y = 8f,
                        ),
                    )
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()
                    onNodeWithContentDescription("Close player").performClick()
                    waitForIdle()
                    onNodeWithTag("NowPlayingContentRoot").assertDoesNotExist()
                    onNodeWithTag(AppNavigationSearchTestTag).assertIsSelected()
                    assertEquals(before, controller.state.value)
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun controlsDismissalPreservesTapSeekAndShortDrag() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "controls-input",
                        title = "Controls input",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/controls-input.mp3"),
                        artworkBytes =
                            java.util.Base64.getDecoder()
                                .decode(
                                    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jD1kAAAAASUVORK5CYII="),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                val expectedQueue = listOf("controls-input")
                try {
                    runBlocking {
                        controller.setOccurrenceQueue(
                            listOf(
                                QueueOccurrence(
                                    "controls-input",
                                    track.toPlayableTrack(),
                                ),
                            ),
                            "controls-input",
                        )
                    }
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    mount(
                        width = 390.dp,
                        height = 768.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "controls-input-scan",
                                "source",
                                ScanStatus.Completed,
                                1,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()

                    onNodeWithTag("NowPlayingPlayPause").performClick()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Playing
                    }
                    val playingQueue =
                        controller.state.value.queue.map { it.id }
                    onNodeWithTag("NowPlayingProgress").performScrollTo()
                    onNodeWithTag("NowPlayingProgress").performTouchInput {
                        click(center)
                    }
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.positionMillis > 0L
                    }
                    assertEquals(
                        PlaybackStatus.Playing,
                        controller.state.value.status,
                    )
                    assertEquals(expectedQueue, playingQueue)

                    onNodeWithTag("NowPlayingCompactLayout").performTouchInput {
                        swipe(
                            Offset(4f, height - 96f),
                            Offset(4f, 96f),
                            durationMillis = 400,
                        )
                    }
                    waitForIdle()
                    onNodeWithTag("NowPlayingCompactLayout")
                        .performScrollToNode(
                            hasContentDescription("Album artwork"))

                    val playerBounds =
                        onNodeWithTag("NowPlayingContentRoot")
                            .fetchSemanticsNode()
                            .boundsInRoot
                    val artworkBounds =
                        onNodeWithContentDescription("Album artwork")
                            .fetchSemanticsNode()
                            .boundsInRoot
                    onNodeWithContentDescription("Album artwork")
                        .performTouchInput {
                            val start =
                                artworkBounds.center - playerBounds.topLeft
                            swipe(
                                start,
                                start + Offset(0f, 100f),
                                durationMillis = 600,
                            )
                        }
                    waitForIdle()

                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    assertEquals(
                        "controls-input",
                        controller.state.value.currentTrack?.id,
                    )
                    assertEquals(
                        PlaybackStatus.Playing,
                        controller.state.value.status,
                    )
                    assertEquals(
                        expectedQueue,
                        controller.state.value.queue.map { it.id },
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun controlsDismissalDoesNotStealNestedPlayerInput() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "nested-controls",
                        title = "Nested controls",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/nested-controls.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                try {
                    runBlocking {
                        controller.setOccurrenceQueue(
                            listOf(
                                QueueOccurrence(
                                    "nested-controls",
                                    track.toPlayableTrack(),
                                ),
                            ),
                            "nested-controls",
                        )
                    }
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    mount(
                        width = 390.dp,
                        height = 480.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "nested-controls-scan",
                                "source",
                                ScanStatus.Completed,
                                1,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()

                    val player = onNodeWithTag("NowPlayingContentRoot")
                    val title = onNodeWithTag("NowPlayingTitle")
                    val titleTopBefore =
                        title.fetchSemanticsNode().boundsInRoot.top
                    player.performTouchInput {
                        swipe(
                            Offset(center.x, height - 24f),
                            Offset(center.x, 24f),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()

                    val titleTopAfter =
                        onNodeWithTag("NowPlayingTitle")
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .top
                    assertTrue(
                        titleTopAfter < titleTopBefore,
                        "An upward Controls drag must scroll content instead of being consumed by sheet dismissal",
                    )
                    onNodeWithTag("NowPlayingPlayPause").assertIsDisplayed()
                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    assertEquals(
                        "nested-controls",
                        controller.state.value.currentTrack?.id,
                    )
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    assertEquals(
                        listOf("nested-controls"),
                        controller.state.value.queue.map { it.id },
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun controlsDismissalDoesNotStartOnPlayControl() =
        withDefaultLocale(Locale.ENGLISH) {
            androidx.compose.ui.test.v2.runSkikoComposeUiTest(
                size = androidx.compose.ui.geometry.Size(1280f, 1100f),
            ) {
                val track =
                    Track(
                        id = "protected-play-control",
                        title = "Protected play control",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source =
                            AudioSource.FilePath("/protected-play-control.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                try {
                    runBlocking {
                        controller.setOccurrenceQueue(
                            listOf(
                                QueueOccurrence(
                                    "protected-play-control",
                                    track.toPlayableTrack(),
                                ),
                            ),
                            "protected-play-control",
                        )
                    }
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    mount(
                        width = 1280.dp,
                        height = 1100.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "protected-play-control-scan",
                                "source",
                                ScanStatus.Completed,
                                1,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()

                    val player = onNodeWithTag("NowPlayingContentRoot")
                    val playControl = onNodeWithTag("NowPlayingPlayPause")
                    val playerBounds = player.fetchSemanticsNode().boundsInRoot
                    val playBounds =
                        playControl.fetchSemanticsNode().boundsInRoot
                    val dragDistance =
                        playerBounds.bottom - playBounds.center.y - 2f
                    assertTrue(
                        dragDistance > playerBounds.height * 0.3f,
                        "The wide test surface must allow a full dismissal-length drag from the Play control: player=$playerBounds play=$playBounds distance=$dragDistance",
                    )
                    playControl.performTouchInput {
                        swipe(
                            center,
                            center + Offset(0f, dragDistance),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()

                    player.assertIsDisplayed()
                    playControl.assertIsDisplayed()
                    assertEquals(
                        "protected-play-control",
                        controller.state.value.currentTrack?.id,
                    )
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    assertEquals(
                        listOf("protected-play-control"),
                        controller.state.value.queue.map { it.id },
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun activeShellDoesNotReenableAnExitingRouteFocusTarget() =
        runComposeUiTest {
            val routeFocusRequester = FocusRequester()
            var routeItemFocused by mutableStateOf(false)
            var routeItemActivations = 0
            setContent {
                ForegroundInteractionBoundary(interactive = true) {
                    DestinationInteractionBoundary(interactive = false) {
                        Box(
                            Modifier.size(40.dp)
                                .focusRequester(routeFocusRequester)
                                .onFocusChanged {
                                    routeItemFocused = it.isFocused
                                }
                                .clickable { routeItemActivations++ },
                        )
                    }
                }
            }

            runOnIdle { routeFocusRequester.requestFocus() }
            assertFalse(routeItemFocused)
            onRoot().performKeyInput {
                keyDown(Key.Enter)
                keyUp(Key.Enter)
            }
            assertEquals(0, routeItemActivations)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dismissedPickerCompletionCannotCloseReplacementOrCancelNewSelection() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val tracks =
                    listOf("one", "two").map { id ->
                        Track(
                            id,
                            "Picker $id",
                            "Artist",
                            "Album",
                            180,
                            TrackAccent(0xFF000000, 0xFFFFFFFF),
                            AudioSource.FilePath("/$id.mp3"))
                    }
                val snapshot =
                    PlaylistSnapshot(
                        playlists =
                            listOf(PlaylistSummary("saved", "Saved", 1, 1)))
                val state =
                    mutableStateOf(
                        PlaylistState(
                            confirmedSnapshot = snapshot,
                            hasConfirmedSnapshot = true))
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = { 390.dp },
                        source = source(),
                        scanSession =
                            ScanSession(
                                "stale-picker",
                                "source",
                                ScanStatus.Completed,
                                1),
                        tracks = tracks,
                        libraryTracks = tracks.map(::libraryTrack),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playlistStateProvider = { state.value },
                        onPlaylistStateAction = {
                            state.value = reducePlaylistState(state.value, it)
                        },
                        onPlaylistMutation = { _, done -> completions += done })
                    onNodeWithTag("AppNavigationSearch").performClick()
                    onNode(hasSetTextAction()).performTextInput("Picker")
                    onNode(
                            hasText("Picker one") and
                                SemanticsMatcher.keyIsDefined(
                                    SemanticsActions.OnLongClick))
                        .performSemanticsAction(SemanticsActions.OnLongClick)
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"))
                        .performClick()
                    onNodeWithText("Saved").performClick()
                    onNodeWithTag("playlist-picker-append").performClick()
                    assertEquals(1, completions.size)
                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    assertEquals(null, state.value.picker)
                    onNode(
                            hasText("Picker two") and
                                SemanticsMatcher.keyIsDefined(
                                    SemanticsActions.OnLongClick))
                        .performSemanticsAction(SemanticsActions.OnLongClick)
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"))
                        .performClick()
                    onNodeWithText("Saved").performClick()
                    onNodeWithTag("playlist-picker-append").performClick()
                    assertEquals(2, completions.size)
                    runOnIdle {
                        completions[0](
                            PlaylistStateAction.SnapshotConfirmed(snapshot, 1))
                    }
                    onNodeWithTag("playlist-picker-append").assertIsDisplayed()
                    assertEquals(listOf("two"), state.value.picker?.trackIds)
                    onNodeWithTag(
                            SelectionShellPlacementTestTag,
                            useUnmergedTree = true,
                        )
                        .assertExists()
                    onNodeWithTag(SelectionShellPlacementTestTag)
                        .assertDoesNotExist()
                    runOnIdle {
                        completions[1](
                            PlaylistStateAction.SnapshotConfirmed(snapshot, 2))
                    }
                    onNodeWithTag("playlist-picker-append").assertDoesNotExist()
                    onNodeWithTag(SelectionShellPlacementTestTag)
                        .assertDoesNotExist()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dismissedBrowserCompletionCannotCloseOrClearReplacementDraft() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val tracks =
                    listOf("one", "two").map { id ->
                        Track(
                            id,
                            "Browser $id",
                            "Artist",
                            "Album",
                            180,
                            TrackAccent(0xFF000000, 0xFFFFFFFF),
                            AudioSource.FilePath("/$id.mp3"))
                    }
                val snapshot =
                    PlaylistSnapshot(
                        playlists =
                            listOf(PlaylistSummary("saved", "Saved", 1, 1)))
                val state =
                    mutableStateOf(
                        PlaylistState(
                            confirmedSnapshot = snapshot,
                            hasConfirmedSnapshot = true,
                            browser = PlaylistBrowserState("saved")))
                val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = { 390.dp },
                        source = source(),
                        scanSession =
                            ScanSession(
                                "stale-browser",
                                "source",
                                ScanStatus.Completed,
                                1),
                        tracks = tracks,
                        libraryTracks = tracks.map(::libraryTrack),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playlistStateProvider = { state.value },
                        onPlaylistStateAction = {
                            state.value = reducePlaylistState(state.value, it)
                        },
                        onPlaylistMutation = { _, done -> completions += done })
                    onNodeWithText("Browser one").performClick()
                    waitForIdle()
                    onNodeWithContentDescription(
                            "debug-browser-selection-count-1")
                        .assertIsDisplayed()
                    assertEquals(
                        setOf("one"), state.value.browser?.selectedTrackIds)
                    assertEquals(
                        listOf("one", "two"),
                        state.value.browser?.visibleTrackIds)
                    assertEquals(
                        androidx.compose.ui.state.ToggleableState.On,
                        onNodeWithText("Browser one")
                            .fetchSemanticsNode()
                            .config
                            .getOrNull(SemanticsProperties.ToggleableState),
                    )
                    onNodeWithTag("playlist-browser-confirm").assertIsEnabled()
                    onNodeWithTag("playlist-browser-confirm").performClick()
                    assertEquals(1, completions.size)
                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    assertEquals(null, state.value.browser)
                    runOnIdle {
                        state.value =
                            reducePlaylistState(
                                state.value,
                                PlaylistStateAction.OpenBrowser(
                                    PlaylistBrowserState("saved")))
                    }
                    onNodeWithText("Browser two").performClick()
                    onNodeWithTag("playlist-browser-confirm").performClick()
                    assertEquals(2, completions.size)
                    runOnIdle {
                        completions[0](
                            PlaylistStateAction.SnapshotConfirmed(snapshot, 1))
                    }
                    onNodeWithTag("playlist-browser-confirm")
                        .assertIsDisplayed()
                    assertEquals(
                        setOf("two"), state.value.browser?.selectedTrackIds)
                    runOnIdle {
                        completions[1](
                            PlaylistStateAction.SnapshotConfirmed(snapshot, 2))
                    }
                    onNodeWithTag("playlist-browser-confirm")
                        .assertDoesNotExist()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun libraryWithoutASelectedTrackDoesNotComposeThePlaybackDock() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 390.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "no-current-track",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onAllNodesWithTag("NowPlayingShellPlacement")
                    .assertCountEquals(0)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactNavigationAndSettingsRemainAvailableWithoutPlayback() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 390.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "navigation-without-playback",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                onNodeWithTag("AppNavigationLibrary").assertIsSelected()
                onNodeWithTag("AppNavigationSearch").assertIsDisplayed()
                onNodeWithTag("AppNavigationPlaylists").assertIsDisplayed()
                onAllNodesWithTag("AppNavigationSettings").assertCountEquals(0)
                val settingsTop =
                    onNodeWithTag("AppSettingsTopBar")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top
                val navigationTop =
                    onNodeWithTag("AppNavigationBottom")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top
                assertTrue(settingsTop < navigationTop)
                val contentBottom =
                    onNodeWithTag(AppShellContentRegionTestTag)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .bottom
                assertTrue(
                    contentBottom <= navigationTop + 1f,
                    "route viewport must end before bottom navigation",
                )
                onNodeWithTag("AppNavigationSearch").performClick()
                waitForIdle()
                assertEquals(
                    settingsTop,
                    onNodeWithTag("AppSettingsTopBar")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top,
                )
                onNodeWithTag("AppNavigationSearch").assertIsSelected()
                onNodeWithTag("AppNavigationPlaylists").performClick()
                waitForIdle()
                val settingsAfterLegacyRoute =
                    onNodeWithTag("AppSettingsTopBar")
                        .fetchSemanticsNode()
                        .boundsInRoot
                assertTrue(settingsAfterLegacyRoute.top >= 0f)
                assertTrue(settingsAfterLegacyRoute.bottom < navigationTop)
                onNodeWithTag("AppNavigationPlaylists").assertIsSelected()
                onNodeWithTag("AppSettingsTopBar").performClick()
                onNodeWithTag("settings-list").assertIsDisplayed()
                onAllNodesWithTag("AppSettingsTopBar").assertCountEquals(0)
                onAllNodesWithTag("AppNavigationSettings").assertCountEquals(0)
                onNodeWithTag("AppNavigationBottom").assertDoesNotExist()
                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationPlaylists").assertIsSelected()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun toolRouteKeepsCorrectNavigationAndHeaderAcrossWidths() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(390.dp)
                mount(
                    width = { width },
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "tool-route-header",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppSettingsTopBar").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle")
                    .assertTextContains("Settings")
                onNodeWithTag("AppShellHeaderBack").assertIsDisplayed()
                onAllNodesWithTag("AppNavigationBottom").assertCountEquals(0)
                onNodeWithTag("settings-list")
                    .performScrollToNode(hasTestTag("settings-about"))
                waitForIdle()
                onNodeWithTag("settings-about").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle").assertTextContains("About")
                onNodeWithTag("AppShellHeaderBack").assertIsDisplayed()
                onNodeWithTag("about-libraries", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle")
                    .assertTextContains("Open Source Libraries")
                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle").assertTextContains("About")
                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle")
                    .assertTextContains("Settings")

                onNodeWithTag("settings-list")
                    .performScrollToNode(hasTestTag("settings-about"))
                onNodeWithTag("settings-about").performClick()
                waitForIdle()
                width = 1200.dp
                waitForIdle()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()
                onNodeWithTag("AppNavigationLibrary").assertIsNotSelected()
                onNodeWithTag("AppNavigationSearch").assertIsNotSelected()
                onNodeWithTag("AppNavigationPlaylists").assertIsNotSelected()
                onNodeWithTag("AppShellHeaderTitle").assertTextContains("About")
                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle")
                    .assertTextContains("Settings")
                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationLibrary").assertIsSelected()
                onNodeWithTag("AppNavigationSettings").performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderTitle")
                    .assertTextContains("Settings")
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onboardingUsesFullSafeContentAndExposesOnlyExistingActions() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = { 390.dp },
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "onboarding-safe-content",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )
                onNodeWithTag("AppSettingsTopBar").performClick()
                onNodeWithTag("settings-list")
                    .performScrollToNode(
                        hasTestTag("settings-review-onboarding"),
                    )
                waitForIdle()
                onNodeWithTag("settings-review-onboarding").performClick()
                waitForIdle()
                onNodeWithTag(OnboardingRootTestTag).assertIsDisplayed()
                onNodeWithTag(OnboardingCloseTestTag)
                    .performScrollTo()
                    .assertIsDisplayed()
                onNodeWithTag(OnboardingNextTestTag)
                    .performScrollTo()
                    .assertIsDisplayed()
                onNodeWithTag(OnboardingBackTestTag).assertDoesNotExist()
                onNodeWithTag(OnboardingFinishTestTag).assertDoesNotExist()
                val root =
                    onNodeWithTag(OnboardingRootTestTag)
                        .fetchSemanticsNode()
                        .boundsInRoot
                assertTrue(root.left >= 0f && root.top >= 0f)
                assertTrue(root.right <= 390f && root.bottom <= 900f)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopDropFeedbackDoesNotOccludeChrome() =
        withDefaultLocale(Locale.ENGLISH) {
            androidx.compose.ui.test.v2.runSkikoComposeUiTest(
                size = androidx.compose.ui.geometry.Size(1280f, 800f),
                density = androidx.compose.ui.unit.Density(1f, 2f),
            ) {
                var active by mutableStateOf(false)
                var feedback by mutableStateOf<String?>(null)
                mount(
                    width = 1200.dp,
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "drop-feedback-layout",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    desktopDropTarget = {
                        DesktopDropTargetPresentation(
                            isAvailable = true,
                            isActive = active,
                            feedback = feedback,
                        )
                    },
                )

                onNodeWithText("Drop music files here.", useUnmergedTree = true)
                    .assertIsDisplayed()
                onNodeWithContentDescription("Choose music folder")
                    .assertIsDisplayed()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()

                runOnIdle { active = true }
                waitForIdle()
                onNodeWithText(
                        "Release to add folders or audio files.",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                onNodeWithContentDescription("Choose music folder")
                    .assertIsDisplayed()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()

                runOnIdle {
                    active = false
                    feedback = "Added 1 folder"
                }
                waitForIdle()
                onNodeWithText("Added 1 folder", useUnmergedTree = true)
                    .assertIsDisplayed()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()

                runOnIdle { feedback = "Unsupported item" }
                waitForIdle()
                onNodeWithText("Unsupported item", useUnmergedTree = true)
                    .assertIsDisplayed()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exitingLibraryRouteStopsExposingAndDispatchingTrackActions() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        "transition-track",
                        "Transition Track",
                        "Artist",
                        "Album",
                        180,
                        TrackAccent(0xFF000000, 0xFFFFFFFF),
                        AudioSource.FilePath("/transition.mp3"),
                    )
                val playbackController =
                    PlaybackController(FakePlaybackEngine())
                mount(
                    width = 390.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            "transition-scan",
                            "source",
                            ScanStatus.Completed,
                            1,
                        ),
                    tracks = listOf(track),
                    libraryTracks = listOf(libraryTrack(track)),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = playbackController,
                )
                onNode(hasContentDescription("Album Album")).performClick()
                waitForIdle()
                val trackTitle = onNodeWithText(track.title)
                trackTitle.assertIsDisplayed()
                trackTitle
                    .performSemanticsAction(SemanticsActions.RequestFocus)
                    .assertIsFocused()
                val contentRegion = onNodeWithTag(AppShellContentRegionTestTag)
                val touchPoint =
                    trackTitle.fetchSemanticsNode().boundsInRoot.center -
                        contentRegion.fetchSemanticsNode().boundsInRoot.topLeft
                mainClock.autoAdvance = false
                onNodeWithTag(AppNavigationSearchTestTag).performClick()
                mainClock.advanceTimeBy(1)
                waitForIdle()
                onNodeWithTag(AppNavigationSearchTestTag).assertIsSelected()
                onNode(
                        SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
                            .and(hasText(track.title, substring = false)))
                    .assertDoesNotExist()
                onNode(
                        SemanticsMatcher.keyIsDefined(
                                SemanticsActions.OnLongClick)
                            .and(hasText(track.title, substring = false)))
                    .assertDoesNotExist()
                onRoot().performKeyInput {
                    keyDown(Key.Tab)
                    keyUp(Key.Tab)
                    keyDown(Key.Enter)
                    keyUp(Key.Enter)
                    keyDown(Key.Spacebar)
                    keyUp(Key.Spacebar)
                }
                waitForIdle()
                assertEquals(null, playbackController.state.value.currentTrack)
                contentRegion.performTouchInput { click(touchPoint) }
                waitForIdle()
                mainClock.advanceTimeBy(240)
                waitForIdle()
                assertEquals(null, playbackController.state.value.currentTrack)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun enteringSearchRequestsFocusOnItsQueryField() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 390.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "search-entry-focus",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag(AppNavigationSearchTestTag).performClick()
                waitForIdle()
                onNodeWithTag("AppShellHeaderRoot").assertIsDisplayed()
                onNodeWithTag("AppShellHeaderTitle").assertIsDisplayed()
                onAllNodes(hasContentDescription("Back")).assertCountEquals(1)
                onNode(hasSetTextAction()).assertIsFocused()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zeroMotionBackCompletionAndCancellationSettleTheCurrentRouteOnce() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest(
                effectContext = TestMotionDurationScale(scaleFactor = 0f),
            ) {
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 390.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "zero-motion-back",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                    )

                    onNodeWithTag(AppNavigationSearchTestTag).performClick()
                    onNodeWithTag(AppNavigationPlaylistsTestTag).performClick()
                    onNodeWithTag(AppNavigationPlaylistsTestTag)
                        .assertIsSelected()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNodeWithTag(AppNavigationSearchTestTag).assertIsSelected()

                    input.backStarted(NavigationEvent())
                    input.backCancelled()
                    waitForIdle()
                    onNodeWithTag(AppNavigationSearchTestTag).assertIsSelected()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNodeWithTag(AppNavigationLibraryTestTag)
                        .assertIsSelected()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun bottomBarEntranceSettlesWithinStandardMotionBudget() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "animated-bottom-bar",
                        title = "Animated Bottom Bar",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source =
                            AudioSource.FilePath("/animated-bottom-bar.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                try {
                    mount(
                        width = 390.dp,
                        height = 600.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "animated-bottom-bar",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(track),
                        libraryTracks = listOf(libraryTrack(track)),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )

                    mainClock.autoAdvance = false
                    controller.setQueue(
                        listOf(track.toPlayableTrack()),
                        selectedTrackId = track.id,
                    )
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.currentTrack?.id == track.id
                    }
                    mainClock.advanceTimeBy(220, ignoreFrameDuration = true)
                    waitForIdle()

                    val bottomBarBottom =
                        onNodeWithTag(
                                NowPlayingShellPlacementTestTag,
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .bottom
                    val navigationTop =
                        onNodeWithTag(
                                AppNavigationBottomTestTag,
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .top
                    assertTrue(
                        bottomBarBottom <= navigationTop + 1f,
                        "bottom bar ends at $bottomBarBottom; navigation " +
                            "starts at $navigationTop after 220ms",
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zeroMotionPlayerExpansionSnapsBeforeNormalDurationElapsed() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest(
                effectContext = TestMotionDurationScale(scaleFactor = 0f),
            ) {
                val track =
                    Track(
                        id = "zero-motion-player",
                        title = "Zero Motion Player",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source =
                            AudioSource.FilePath("/zero-motion-player.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                try {
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    mount(
                        width = 390.dp,
                        height = 600.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "zero-motion-player",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )

                    mainClock.autoAdvance = false
                    onNodeWithTag(
                            "NowPlayingBarRoot",
                            useUnmergedTree = true,
                        )
                        .performClick()
                    mainClock.advanceTimeBy(32)
                    waitForIdle()

                    val viewportHeight =
                        onRoot().fetchSemanticsNode().boundsInRoot.height
                    val playerHeight =
                        onNodeWithTag("NowPlayingContentRoot")
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .height
                    assertTrue(
                        playerHeight >= viewportHeight * 0.95f,
                        "zero-motion player height $playerHeight must fill " +
                            "viewport $viewportHeight before the 200ms " +
                            "normal transition completes",
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun documentCommandsCannotExportAnUnconfirmedDefaultSnapshot() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 390.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            "unconfirmed-documents",
                            "source",
                            ScanStatus.Completed,
                            1L),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playlistState = PlaylistState(readErrorMessage = "Failed"),
                    backupDocumentAvailable = true,
                )
                onNodeWithTag("AppNavigationPlaylists").performClick()
                onNodeWithTag("playlist-backup-json-export")
                    .assertDoesNotExist()
                onNodeWithTag("playlist-backup-json-import")
                    .assertDoesNotExist()
                onNodeWithTag("AppSettingsTopBar").performClick()
                assertFailsWith<AssertionError> {
                    onNodeWithTag("settings-list")
                        .performScrollToNode(
                            hasTestTag("playlist-backup-json-export"))
                }
                onNodeWithTag("settings-list")
                    .performScrollToNode(hasTestTag("settings-about"))
                onNodeWithTag("playlist-backup-json-export")
                    .assertDoesNotExist()
                onNodeWithTag("playlist-backup-json-import")
                    .assertDoesNotExist()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun previewCancelStaleConfirmationAndReadFailurePreserveLibrarySnapshot() =
        withDefaultLocale(Locale.ENGLISH) {
            androidx.compose.ui.test.v2.runSkikoComposeUiTest(
                size = androidx.compose.ui.geometry.Size(1280f, 800f),
                density = androidx.compose.ui.unit.Density(1f, 2f),
            ) {
                val backupState = mutableStateOf(PlaylistBackupUiState())
                var openRequests = 0
                var width by mutableStateOf(390.dp)
                val backupTrack =
                    LibraryTrack(
                        id = "backup-track",
                        sourceId = "source",
                        sourceLocalKey = "backup-track.mp3",
                        audioSource = AudioSource.FilePath("backup-track.mp3"),
                        displayName = "backup-track.mp3",
                        title = "Backup song",
                        artist = "Backup artist",
                        album = "Backup album",
                        durationMillis = 60_000,
                        sizeBytes = 1,
                        modifiedAtEpochMillis = 1,
                        lastSeenScanId = "scan",
                        createdAtEpochMillis = 1,
                        updatedAtEpochMillis = 1,
                    )
                val librarySnapshot =
                    PlaylistSnapshot(
                        playlists =
                            listOf(
                                PlaylistSummary("existing", "Existing", 1, 1)),
                        entriesByPlaylistId =
                            mapOf(
                                "existing" to
                                    listOf(
                                        PlaylistEntry(
                                            "backup-entry",
                                            "existing",
                                            backupTrack.id,
                                            0,
                                            60_000,
                                        ),
                                    ),
                            ),
                    )
                var document = byteArrayOf()
                val launcher =
                    object : PlaylistBackupDocumentLauncher {
                        override val isAvailable = true

                        override fun open() {
                            openRequests++
                        }

                        override fun save(
                            suggestedFileName: String,
                            bytes: ByteArray
                        ) {
                            document = bytes.copyOf()
                        }
                    }
                var guardedRevisionChecks = 0
                val liveState =
                    PlaylistState(
                        confirmedSnapshot = librarySnapshot,
                        hasConfirmedSnapshot = true,
                    )
                var confirmation: PlaylistBackupImportConfirmation? = null
                val controller =
                    createPlaylistBackupController(
                        PlaylistStateOwner(
                            EmptyPlaylistRepository, Dispatchers.Unconfined),
                        Dispatchers.Unconfined,
                        launcher,
                        object : PlaylistBackupRevisionGuard {
                            override suspend fun <T> withCurrentRevision(
                                expectedRevision: Long,
                                block: suspend () -> T,
                            ): PlaylistBackupRevisionGuardResult<T> {
                                guardedRevisionChecks++
                                return PlaylistBackupRevisionGuardResult.Stale
                            }
                        },
                    )
                val saving = runBlocking {
                    controller.beginExport(
                        backupState.value,
                        librarySnapshot,
                        listOf(backupTrack),
                        1,
                    )
                }
                backupState.value =
                    controller.receiveSave(
                        saving, PlaylistBackupDocumentSaveResult.Success)
                fun receive(result: PlaylistBackupDocumentOpenResult) {
                    backupState.value = runBlocking {
                        controller.receiveOpen(
                            backupState.value,
                            result,
                            listOf(backupTrack),
                            listOf("Existing"),
                            "Imported",
                            1L)
                    }
                }
                mount(
                    width = { width },
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "playlist-backup-production-host",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    libraryTracks = listOf(backupTrack),
                    playlistStateProvider = { liveState },
                    playlistBackupState = { backupState.value },
                    backupDocumentAvailable = true,
                    onOpenPlaylistBackup = {
                        backupState.value =
                            controller.beginOpen(backupState.value)
                    },
                    onPlaylistBackupAction = { action ->
                        backupState.value =
                            controller.reduce(backupState.value, action)
                    },
                    onConfirmPlaylistBackup = {
                        confirmation = runBlocking {
                            controller.confirm(
                                backupState.value,
                                librarySnapshot,
                            )
                        }
                        backupState.value = requireNotNull(confirmation).state
                    },
                )

                onNodeWithTag("AppNavigationPlaylists").performClick()
                onNodeWithTag("AppSettingsTopBar").assertIsDisplayed()
                onNodeWithTag("playlist-backup-json-import")
                    .performScrollTo()
                    .performClick()
                assertEquals(1, openRequests)
                runOnIdle {
                    receive(PlaylistBackupDocumentOpenResult.Cancelled)
                }
                assertEquals(
                    PlaylistBackupOperation.Idle, backupState.value.operation)
                assertEquals(librarySnapshot, liveState.confirmedSnapshot)
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(0)
                onNodeWithTag("playlist-backup-json-import").performClick()
                assertEquals(2, openRequests)
                runOnIdle {
                    receive(PlaylistBackupDocumentOpenResult.Success(document))
                }
                waitForIdle()
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(1)

                width = 1200.dp
                onNodeWithTag("playlist-backup-preview-confirm")
                    .assertIsDisplayed()
                onNodeWithTag("playlist-backup-preview-confirm").performClick()
                waitForIdle()
                assertEquals(1, guardedRevisionChecks)
                assertEquals(
                    liveState.confirmedSnapshot,
                    confirmation?.confirmedSnapshot,
                )
                assertEquals(librarySnapshot, liveState.confirmedSnapshot)
                assertEquals(
                    PlaylistBackupUiError.StalePreview,
                    backupState.value.error,
                )
                onNodeWithTag("playlist-backup-preview-confirm")
                    .assertIsNotEnabled()
                onNodeWithTag("playlist-backup-preview-error")
                    .assertIsDisplayed()
                onNodeWithTag("playlist-backup-preview-dismiss").performClick()
                waitForIdle()
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(0)

                onNodeWithTag("AppNavigationSettings").performClick()
                waitForIdle()
                onNodeWithTag("settings-list")
                    .performScrollToNode(
                        hasTestTag("playlist-backup-json-import"))
                onAllNodesWithTag("playlist-backup-json-import")
                    .assertCountEquals(1)
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(0)
                onNodeWithTag("playlist-backup-json-import").performClick()
                assertEquals(3, openRequests)
                runOnIdle {
                    receive(
                        PlaylistBackupDocumentOpenResult.Failure(
                            "read failure"))
                }
                assertEquals(
                    PlaylistBackupUiError.ReadFailed, backupState.value.error)
                assertEquals(librarySnapshot, liveState.confirmedSnapshot)
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(0)
                onNodeWithTag("playlist-backup-json-import").performClick()
                assertEquals(4, openRequests)
                runOnIdle {
                    receive(PlaylistBackupDocumentOpenResult.Success(document))
                }
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(1)
                onNodeWithTag("playlist-backup-preview-dismiss").performClick()
                waitForIdle()
                onAllNodesWithTag("playlist-backup-preview-confirm")
                    .assertCountEquals(0)
                assertEquals(null, backupState.value.preview)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactSettingsRemainsReachableFromAlbumDetail() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "compact-settings-track",
                        title = "Compact settings track",
                        artist = "Compact settings artist",
                        album = "Compact settings album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source =
                            AudioSource.FilePath("/compact-settings-track.mp3"),
                    )
                mount(
                    width = 600.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "compact-settings",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNode(hasContentDescription("Album Compact settings album"))
                    .performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                onNodeWithTag(AppSettingsTopBarTestTag).performClick()
                onNodeWithTag("settings-list").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sidebarCollectionDestinationsReuseHomeWithoutPushingRoutes() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val favorite =
                    Track(
                        id = "sidebar-favorite",
                        title = "Sidebar favorite",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/sidebar-favorite.mp3"),
                    )
                val recent =
                    Track(
                        id = "sidebar-recent",
                        title = "Sidebar recent",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/sidebar-recent.mp3"),
                    )
                val added =
                    Track(
                        id = "sidebar-added",
                        title = "Sidebar newest",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/sidebar-added.mp3"),
                    )
                mount(
                    width = 1000.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "sidebar-browse-shortcuts",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(favorite, recent, added),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    favoriteTrackIds = { setOf(favorite.id) },
                    playHistory =
                        mapOf(
                            recent.id to TrackPlayHistory(recent.id, 1L, 100L),
                        ),
                    createdAtByTrackId =
                        mapOf(
                            favorite.id to 1L,
                            recent.id to 2L,
                            added.id to 3L,
                        ),
                )

                onNodeWithTag("AppNavigationFavorites").performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationLibrary").assertIsSelected()
                onNodeWithText("Sidebar favorite").assertIsDisplayed()
                onAllNodes(hasText("Sidebar recent", substring = false))
                    .assertCountEquals(0)

                onNodeWithTag("AppNavigationRecentlyPlayed").performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationLibrary").assertIsSelected()
                onNodeWithText("Sidebar recent").assertIsDisplayed()
                onAllNodes(hasText("Sidebar favorite", substring = false))
                    .assertCountEquals(0)

                onNodeWithTag("AppNavigationRecentlyAdded").performClick()
                waitForIdle()
                onNodeWithTag("AppNavigationRecentlyAdded").assertIsSelected()
                onNodeWithText("Sidebar newest").assertIsDisplayed()

                onNodeWithContentDescription("Albums").performClick()
                waitForIdle()
                onNodeWithContentDescription("Albums").assertIsSelected()
                onNodeWithText("Album").assertIsDisplayed()

                onNodeWithContentDescription("Artists").performClick()
                waitForIdle()
                onNodeWithContentDescription("Artists").assertIsSelected()
                onNodeWithText("Artist").assertIsDisplayed()

                onNodeWithContentDescription("Songs").performClick()
                waitForIdle()
                onNodeWithContentDescription("Songs").assertIsSelected()
                onNodeWithText("Sidebar favorite").assertIsDisplayed()
                onNodeWithText("Sidebar recent").assertIsDisplayed()
                onNodeWithText("Sidebar newest").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun navigationReflowsAtBottomRailAndSidebarBoundaries() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(719.dp)
                mount(
                    width = { width },
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "navigation-breakpoints",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                width = 720.dp
                waitForIdle()
                onNodeWithTag("AppNavigationRail").assertIsDisplayed()
                width = 999.dp
                waitForIdle()
                onNodeWithTag("AppNavigationRail").assertIsDisplayed()
                width = 1000.dp
                waitForIdle()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun nowPlayingDockRespectsCompactAndDesktopMinimumHeight() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "adaptive-dock-track",
                        title = "Adaptive dock track",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/adaptive-dock.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                var width by mutableStateOf(390.dp)
                mount(
                    width = { width },
                    height = 600.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "adaptive-dock-height",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )

                fun assertDockMinimumHeight(minimumHeightDp: Dp) {
                    val dock =
                        onNodeWithTag(
                                "NowPlayingShellPlacement",
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                    val minimumHeightPx =
                        minimumHeightDp.value * dock.layoutInfo.density.density
                    assertTrue(
                        dock.boundsInRoot.height >= minimumHeightPx,
                        "playback dock measured ${dock.boundsInRoot.height}px; " +
                            "minimum is $minimumHeightPx px",
                    )
                }

                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                assertDockMinimumHeight(64.dp)
                width = 900.dp
                waitForIdle()
                onNodeWithTag("AppNavigationRail").assertIsDisplayed()
                assertDockMinimumHeight(80.dp)
                width = 1_000.dp
                waitForIdle()
                onNodeWithTag("AppNavigationSidebar").assertIsDisplayed()
                assertDockMinimumHeight(80.dp)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopHomeDoesNotReserveAnEmptyDetailPane() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 800.dp,
                    height = 600.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "wide-home-without-detail",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onAllNodes(
                        hasText(
                            "Select an album or artist to show details here.",
                            substring = false,
                        ),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(0)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sidebarNavigationTargetsMeetThe48DpMinimum() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 1200.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "sidebar-touch-targets",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                val navigationTags =
                    listOf(
                        AppNavigationLibraryTestTag,
                        AppNavigationSearchTestTag,
                        AppNavigationPlaylistsTestTag,
                        AppNavigationFavoritesTestTag,
                        AppNavigationRecentlyPlayedTestTag,
                        AppNavigationRecentlyAddedTestTag,
                        AppNavigationSettingsTestTag,
                    )
                navigationTags.forEach { tag ->
                    val node =
                        onNodeWithTag(tag, useUnmergedTree = true)
                            .fetchSemanticsNode()
                    val minimumHeightPx = 48f * node.layoutInfo.density.density
                    assertTrue(
                        node.boundsInRoot.height >= minimumHeightPx,
                        "$tag target height is ${node.boundsInRoot.height}px; " +
                            "minimum is $minimumHeightPx px",
                    )
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun rowDensityUsesAppWindowWidthWhenRailReducesContentBelowBreakpoint() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(800.dp)
                val track =
                    Track(
                        id = "density-window-track",
                        title = "Density window track",
                        artist = "Density artist",
                        album = "Density album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source =
                            AudioSource.FilePath("/density-window-track.mp3"),
                    )
                mount(
                    width = { width },
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "density-window",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppNavigationRail").assertIsDisplayed()
                val contentRegion =
                    onNodeWithTag(AppShellContentRegionTestTag)
                        .fetchSemanticsNode()
                val density = contentRegion.layoutInfo.density.density
                assertTrue(
                    contentRegion.boundsInRoot.width < 720f * density,
                    "800dp app window should leave less than 720dp content " +
                        "after rail and margins",
                )

                onNodeWithContentDescription("Songs").performClick()
                waitForIdle()
                val desktopHomeRowHeight =
                    onNodeWithContentDescription(track.title)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .height
                width = 600.dp
                waitForIdle()
                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                val compactHomeRowHeight =
                    onNodeWithContentDescription(track.title)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .height
                assertTrue(
                    desktopHomeRowHeight < compactHomeRowHeight,
                    "800dp desktop Home row must be denser than the 600dp " +
                        "compact Home row; heights were $desktopHomeRowHeight " +
                        "and $compactHomeRowHeight px",
                )

                onNodeWithContentDescription("Albums").performClick()
                waitForIdle()
                onNode(hasContentDescription("Album Density album"))
                    .performClick()
                waitForIdle()
                width = 800.dp
                waitForIdle()
                val desktopDetailRowHeight =
                    onNodeWithContentDescription(track.title)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .height
                width = 600.dp
                waitForIdle()
                val compactDetailRowHeight =
                    onNodeWithContentDescription(track.title)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .height
                assertTrue(
                    desktopDetailRowHeight < compactDetailRowHeight,
                    "800dp desktop detail row must be denser than the 600dp " +
                        "compact detail row; heights were " +
                        "$desktopDetailRowHeight and $compactDetailRowHeight px",
                )
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopDetailKeepsHomeBesideItAtMinimumNetWidthAndReturnsToHome() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "split-track",
                        title = "Split boundary track",
                        artist = "Split artist",
                        album = "Split album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/split-track.mp3"),
                    )
                mount(
                    width = 1016.dp,
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "split-boundary",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppNavigationSettings").assertIsDisplayed()
                onAllNodesWithTag(AppSettingsTopBarTestTag).assertCountEquals(0)

                onNode(hasContentDescription("Album Split album"))
                    .performClick()
                waitForIdle()
                onNodeWithText("Albums").assertIsDisplayed()
                onNodeWithText("Split boundary track").assertIsDisplayed()
                val browsePane =
                    onNodeWithTag("AppShellHomePane").fetchSemanticsNode()
                val detailPane =
                    onNodeWithTag("AppShellDetailPane").fetchSemanticsNode()
                val density = browsePane.layoutInfo.density.density
                assertTrue(
                    browsePane.boundsInRoot.width >= 320f * density,
                    "desktop browsing pane must retain its 320dp minimum",
                )
                assertTrue(
                    detailPane.boundsInRoot.left -
                        browsePane.boundsInRoot.right >= 24f * density,
                    "desktop detail panes must retain a 24dp gap",
                )
                assertTrue(
                    detailPane.boundsInRoot.width >= 400f * density,
                    "desktop detail pane must retain its 400dp minimum",
                )

                onNode(hasContentDescription("Back")).performClick()
                waitForIdle()
                onNodeWithText("Albums").assertIsDisplayed()

                onNodeWithText("Artists").performClick()
                onNode(hasContentDescription("Artist Split artist"))
                    .performClick()
                waitForIdle()
                onNodeWithText("Split boundary track").assertIsDisplayed()
                onNodeWithTag("AppShellHomePane").assertIsDisplayed()
                onNodeWithTag("AppShellDetailPane").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistRoutesUseSharedHeaderWithoutDuplicateFeatureBack() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(600.dp)
                val playlist = PlaylistSummary("static-route", "Road Mix", 1, 1)
                val smart =
                    SmartPlaylistSummary(
                        "smart-route",
                        "Favorite Songs",
                        com.eterocell.rhythhaus.library.SmartPlaylistRule
                            .Favorites,
                        1,
                        1,
                    )
                val state =
                    PlaylistState(
                        confirmedSnapshot =
                            PlaylistSnapshot(
                                playlists = listOf(playlist),
                                entriesByPlaylistId =
                                    mapOf(
                                        playlist.id to
                                            listOf(
                                                PlaylistEntry(
                                                    "route-entry",
                                                    playlist.id,
                                                    "route-track",
                                                    0,
                                                    1,
                                                ),
                                            ),
                                    ),
                                smartPlaylists = listOf(smart),
                            ),
                        hasConfirmedSnapshot = true,
                    )
                mount(
                    width = { width },
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "playlist-header-routes",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = emptyList(),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playlistState = state,
                )

                onNodeWithTag("AppNavigationPlaylists").performClick()
                waitForIdle()
                assertSharedPlaylistHeader("Playlists")

                onNode(hasContentDescription("Playlist Road Mix, 1 tracks"))
                    .performClick()
                waitForIdle()
                assertSharedPlaylistHeader("Road Mix")

                onNodeWithTag("AppShellHeaderBack").performClick()
                waitForIdle()
                assertSharedPlaylistHeader("Playlists")

                onNode(hasContentDescription("Favorite Songs")).performClick()
                waitForIdle()
                assertSharedPlaylistHeader("Favorite Songs")

                width = 1200.dp
                waitForIdle()
                onNodeWithTag("AppShellHeaderRoot").assertIsDisplayed()
                onNodeWithText("Favorite Songs").assertIsDisplayed()
                onNodeWithTag("AppNavigationSettings").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest
        .assertSharedPlaylistHeader(
        title: String,
    ) {
        onNodeWithTag("AppShellHeaderRoot").assertIsDisplayed()
        onNodeWithTag("AppShellHeaderTitle").assertTextContains(title)
        onNodeWithTag("playlist-back").assertDoesNotExist()
        onNodeWithTag("playlist-toolbar-title").assertDoesNotExist()
        if (title == "Playlists") {
            onNodeWithTag("AppShellHeaderBack").assertDoesNotExist()
            onNodeWithTag("AppSettingsTopBar").assertIsDisplayed()
        } else {
            onNodeWithTag("AppShellHeaderBack").assertIsDisplayed()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun albumDestinationAndSelectionSurviveDesktopToRailReflow() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(1200.dp)
                val track =
                    Track(
                        id = "detail-reflow-track",
                        title = "Detail reflow track",
                        artist = "Detail reflow artist",
                        album = "Detail reflow album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source =
                            AudioSource.FilePath("/detail-reflow-track.mp3"),
                    )
                mount(
                    width = { width },
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "detail-reflow",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNode(hasContentDescription("Album Detail reflow album"))
                    .performClick()
                waitForIdle()
                onNodeWithTag(AppShellHomePaneTestTag).assertIsDisplayed()
                onNodeWithTag(AppShellDetailPaneTestTag).assertIsDisplayed()
                onNode(
                        SemanticsMatcher.keyIsDefined(
                                SemanticsActions.OnLongClick)
                            .and(
                                hasText(
                                    "Detail reflow track", substring = false)))
                    .performSemanticsAction(SemanticsActions.OnLongClick)
                onNodeWithTag(SelectionShellPlacementTestTag)
                    .assertIsDisplayed()

                width = 800.dp
                waitForIdle()

                onNodeWithText("Detail reflow track").assertIsDisplayed()
                onNodeWithTag(SelectionShellPlacementTestTag)
                    .assertIsDisplayed()
                onNodeWithTag(AppNavigationLibraryTestTag).assertIsSelected()
                onNode(hasContentDescription("Back")).assertIsDisplayed()
                onAllNodesWithTag(AppShellDetailPaneTestTag)
                    .assertCountEquals(0)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun zeroHeightNowPlayingDockExposesNoActions() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "zero-height-dock-track",
                        title = "Zero height dock track",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/zero-height-dock.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                mount(
                    width = 390.dp,
                    height = 0.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "zero-height-dock",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )

                val dock =
                    onNodeWithTag(
                            "NowPlayingShellPlacement",
                            useUnmergedTree = true,
                        )
                        .fetchSemanticsNode()
                assertTrue(
                    dock.boundsInRoot.height <= 1f,
                    "zero-height viewport measured dock at " +
                        "${dock.boundsInRoot.height}px",
                )
                onAllNodes(
                        SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
                            .and(
                                hasAnyAncestor(
                                    hasTestTag("NowPlayingShellPlacement"),
                                ),
                            ),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(0)
                onAllNodes(
                        SemanticsMatcher.keyIsDefined(
                                SemanticsActions.OnLongClick,
                            )
                            .and(
                                hasAnyAncestor(
                                    hasTestTag("NowPlayingShellPlacement"),
                                ),
                            ),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(0)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchQueryScrollAndSelectionSurviveReflowAndRealNavigationClearsSelection() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(390.dp)
                val tracks =
                    List(60) { index ->
                        Track(
                            id = "search-reflow-$index",
                            title = "Reflow query $index",
                            artist = "Artist",
                            album = "Album",
                            durationSeconds = 180,
                            accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                            source =
                                AudioSource.FilePath(
                                    "/search-reflow-$index.mp3"),
                        )
                    }
                mount(
                    width = { width },
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "search-reflow",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = tracks,
                    libraryTracks = tracks.map(::libraryTrack),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onNodeWithTag("AppNavigationSearch").performClick()
                onNode(hasSetTextAction()).performTextInput("reflow query")
                waitForIdle()
                onNode(hasSetTextAction()).assertTextContains("reflow query")
                onNodeWithText("60 results").assertIsDisplayed()
                onNode(hasScrollToIndexAction()).performScrollToIndex(45)
                onNodeWithText("Reflow query 45").assertIsDisplayed()
                onNode(
                        SemanticsMatcher.keyIsDefined(
                                SemanticsActions.OnLongClick)
                            .and(hasText("Reflow query 45", substring = false)))
                    .performSemanticsAction(SemanticsActions.OnLongClick)
                onNodeWithTag(SelectionShellPlacementTestTag)
                    .assertIsDisplayed()
                onNodeWithTag(AppNavigationSearchTestTag).performClick()
                waitForIdle()
                onNodeWithTag(SelectionShellPlacementTestTag)
                    .assertIsDisplayed()
                onNode(hasSetTextAction()).assertTextContains("reflow query")
                onNode(hasScrollToIndexAction()).performScrollToIndex(59)
                waitForIdle()
                onNodeWithText("Reflow query 59").assertIsDisplayed()
                val compactSearchRow =
                    onNode(
                            SemanticsMatcher.keyIsDefined(
                                    SemanticsActions.OnLongClick)
                                .and(
                                    hasText(
                                        "Reflow query 59", substring = false)))
                        .fetchSemanticsNode()
                val compactSearchRowHeightDp =
                    compactSearchRow.boundsInRoot.height /
                        compactSearchRow.layoutInfo.density.density
                val compactLastRowBottom =
                    onNodeWithText("Reflow query 59")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .bottom
                val compactDockTop =
                    onNodeWithTag(SelectionShellPlacementTestTag)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top
                assertTrue(
                    compactLastRowBottom <= compactDockTop + 1f,
                    "compact final result row must end above the selection dock",
                )
                val compactVisibleSearchResults =
                    onAllNodes(hasText("Reflow query", substring = true))
                        .fetchSemanticsNodes()
                        .sortedBy { it.boundsInRoot.top }
                        .map { node ->
                            node.config[SemanticsProperties.Text].joinToString(
                                separator = "") {
                                    it.text
                                }
                        }
                val compactFirstVisibleSearchResult =
                    compactVisibleSearchResults.first()
                width = 800.dp
                waitForIdle()
                onNode(hasScrollToIndexAction()).performScrollToIndex(59)
                waitForIdle()
                val railSearchRow =
                    onNode(
                            SemanticsMatcher.keyIsDefined(
                                    SemanticsActions.OnLongClick)
                                .and(
                                    hasText(
                                        "Reflow query 59", substring = false)))
                        .fetchSemanticsNode()
                val railSearchRowHeightDp =
                    railSearchRow.boundsInRoot.height /
                        railSearchRow.layoutInfo.density.density
                assertEquals(56f, railSearchRowHeightDp, 0.5f)
                width = 1200.dp
                waitForIdle()

                onNode(hasSetTextAction()).assertTextContains("reflow query")
                onNodeWithText("60 results").assertIsDisplayed()
                onNodeWithTag("AppNavigationSearch").assertIsSelected()
                val desktopVisibleSearchResults =
                    onAllNodes(hasText("Reflow query", substring = true))
                        .fetchSemanticsNodes()
                        .sortedBy { it.boundsInRoot.top }
                        .map { node ->
                            node.config[SemanticsProperties.Text].joinToString(
                                separator = "") {
                                    it.text
                                }
                        }
                assertTrue(
                    compactFirstVisibleSearchResult in
                        desktopVisibleSearchResults,
                    "search scroll anchor should survive reflow; " +
                        "before=$compactVisibleSearchResults, " +
                        "after=$desktopVisibleSearchResults",
                )
                onNode(hasScrollToIndexAction()).performScrollToIndex(59)
                waitForIdle()
                onNodeWithText("Reflow query 59").assertIsDisplayed()
                val desktopSearchRow =
                    onNode(
                            SemanticsMatcher.keyIsDefined(
                                    SemanticsActions.OnLongClick)
                                .and(
                                    hasText(
                                        "Reflow query 59", substring = false)))
                        .fetchSemanticsNode()
                val desktopSearchRowHeightDp =
                    desktopSearchRow.boundsInRoot.height /
                        desktopSearchRow.layoutInfo.density.density
                assertEquals(56f, desktopSearchRowHeightDp, 0.5f)
                assertTrue(
                    desktopSearchRowHeightDp < compactSearchRowHeightDp,
                    "wide Search must use desktop row density",
                )
                onNodeWithTag(SelectionShellPlacementTestTag)
                    .assertIsDisplayed()
                val desktopLastRowBottom =
                    onNodeWithText("Reflow query 59")
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .bottom
                val desktopDockTop =
                    onNodeWithTag(SelectionShellPlacementTestTag)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .top
                assertTrue(
                    desktopLastRowBottom <= desktopDockTop + 1f,
                    "desktop final result row must end above the selection dock",
                )

                onNodeWithTag(AppNavigationLibraryTestTag).performClick()
                waitForIdle()
                onNodeWithTag(AppNavigationLibraryTestTag).assertIsSelected()
                onNodeWithTag(AppNavigationSearchTestTag).assertIsNotSelected()
                onAllNodesWithTag(SelectionShellPlacementTestTag)
                    .assertCountEquals(0)
                onNodeWithTag(AppNavigationSearchTestTag).performClick()
                waitForIdle()
                assertTrue(
                    onNode(hasSetTextAction())
                        .fetchSemanticsNode()
                        .config[SemanticsProperties.EditableText]
                        .text
                        .isEmpty(),
                    "a new Search destination should start with a blank query",
                )
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onboardingReviewPreservesPlayingStateAndQueue() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        "playing-track",
                        "Playing track",
                        "Artist",
                        "Album",
                        1_000,
                        TrackAccent(0, 0),
                        AudioSource.FilePath("/playing.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                controller.play()
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Playing
                }
                val beforeReview = controller.state.value

                mount(
                    width = 420.dp,
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "playing-review",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )
                onNodeWithTag("AppSettingsTopBar", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                onNodeWithTag("settings-review-onboarding").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    onAllNodesWithTag(OnboardingRootTestTag)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
                onNodeWithTag(OnboardingRootTestTag).assertIsDisplayed()
                onAllNodesWithTag(AppSettingsTopBarTestTag).assertCountEquals(0)

                assertEquals(beforeReview, controller.state.value)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onboardingReviewExclusivelyComposesOverSettingsAtAllWidths() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                listOf(420.dp, 1200.dp).forEach { width ->
                    mount(
                        width = width,
                        height = 400.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "review-isolation-$width",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                    )
                    when {
                        width < 720.dp -> onNodeWithTag("AppNavigationBottom")
                        width < 1_000.dp -> onNodeWithTag("AppNavigationRail")
                        else -> onNodeWithTag("AppNavigationSidebar")
                    }.assertIsDisplayed()
                    val settingsActionTag =
                        if (width < 720.dp) AppSettingsTopBarTestTag
                        else "AppNavigationSettings"
                    onNodeWithTag(settingsActionTag, useUnmergedTree = true)
                        .performClick()
                    waitForIdle()
                    onNodeWithTag("settings-list", useUnmergedTree = true)
                        .performScrollToNode(hasText("Review onboarding"))
                    onNodeWithTag("settings-review-onboarding").performClick()

                    onNodeWithTag(OnboardingRootTestTag).assertIsDisplayed()
                    onAllNodesWithTag(
                            "settings-root",
                            useUnmergedTree = true,
                        )
                        .assertCountEquals(0)
                    onAllNodesWithTag(
                            "settings-list",
                            useUnmergedTree = true,
                        )
                        .assertCountEquals(0)
                    onAllNodesWithTag("AppNavigationBottom")
                        .assertCountEquals(0)
                    onAllNodesWithTag("AppNavigationRail").assertCountEquals(0)
                    onAllNodesWithTag("AppNavigationSidebar")
                        .assertCountEquals(0)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsReviewRestoresDestinationScrollState() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val appearance = mutableStateOf(defaultSettingsAppearance())
                mount(
                    width = { 420.dp },
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "settings-review",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    settingsAppearanceProvider = { appearance.value },
                )
                onNodeWithTag("AppSettingsTopBar", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("View scan report"))
                val settingsBottom =
                    onNodeWithTag("settings-list", useUnmergedTree = true)
                        .fetchSemanticsNode()
                        .boundsInRoot
                        .bottom
                onNodeWithTag("AppNavigationBottom").assertDoesNotExist()
                appearance.value =
                    appearance.value.copy(
                        paletteId =
                            com.eterocell.rhythhaus.theme.HausPaletteId.Iris,
                    )
                waitForIdle()
                onNode(hasText("View scan report")).assertIsDisplayed()
                assertTrue(
                    settingsBottom <= 400f,
                    "Settings viewport must stay within the full tool viewport",
                )
                onNode(hasText("View scan report")).performClick()
                onNodeWithTag("settings-root", useUnmergedTree = true)
                    .assertIsDisplayed()
                onNodeWithTag("AppNavigationLibrary").assertDoesNotExist()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                val reviewAction = onNodeWithTag("settings-review-onboarding")
                reviewAction.assertIsDisplayed().performClick()
                onNodeWithTag(OnboardingCloseTestTag).performClick()
                reviewAction.assertIsDisplayed()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Hide scan report"))
                onNode(hasText("Hide scan report"), useUnmergedTree = true)
                    .assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsReviewRestoresStateAcrossAdaptiveWidthChange() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                var width by mutableStateOf(420.dp)
                mount(
                    width = { width },
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "adaptive-review",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )
                onNodeWithTag("AppSettingsTopBar", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("View scan report"))
                onNode(hasText("View scan report")).performClick()
                onNodeWithTag("settings-root", useUnmergedTree = true)
                    .assertIsDisplayed()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                onNodeWithTag("settings-review-onboarding").performClick()

                width = 1200.dp
                waitForIdle()
                onNodeWithTag(OnboardingCloseTestTag).performClick()

                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Hide scan report"))
                onNode(hasText("Hide scan report"), useUnmergedTree = true)
                    .assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun lyricsAndTimerScrollDoNotDismissPlayer() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "sleep-timer-track",
                        title = "Sleep timer track",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/sleep-timer.mp3"),
                        lyrics =
                            List(40) { index ->
                                    "Sleep timer lyric ${index + 1}"
                                }
                                .joinToString("\n"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                mount(
                    width = 600.dp,
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "sleep-timer-route",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )

                onNodeWithTag("AppNavigationBottom").assertIsDisplayed()
                onNodeWithTag(
                        "NowPlayingShellPlacement", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onAllNodesWithTag("AppNavigationBottom").assertCountEquals(0)
                onAllNodesWithTag("AppNavigationRail").assertCountEquals(0)
                onAllNodesWithTag("AppNavigationSidebar").assertCountEquals(0)
                onAllNodes(
                        SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick)
                            .and(
                                hasAnyAncestor(
                                    hasTestTag("NowPlayingShellPlacement"))),
                        useUnmergedTree = true,
                    )
                    .assertCountEquals(0)
                onNode(hasText("Lyrics") and hasClickAction())
                    .performScrollTo()
                    .performClick()
                repeat(8) {
                    onNodeWithTag("NowPlayingLyricsList").performTouchInput {
                        swipe(
                            Offset(center.x, center.y + 120f),
                            Offset(center.x, center.y - 120f),
                            durationMillis = 500,
                        )
                    }
                }
                onNode(hasText("Sleep timer lyric 40")).assertIsDisplayed()
                onNodeWithTag("NowPlayingContentRoot").performTouchInput {
                    swipe(
                        Offset(center.x, 40f),
                        Offset(center.x, center.y / 2f),
                        durationMillis = 600,
                    )
                }
                waitForIdle()
                onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                onNodeWithTag("NowPlayingLyricsList").assertIsDisplayed()
                onNode(hasContentDescription("Close lyrics")).performClick()
                onNode(hasText("Timer")).performScrollTo().performClick()
                onNodeWithTag("NowPlayingSleepTimerPanel").performTouchInput {
                    swipe(
                        Offset(center.x, center.y + 120f),
                        Offset(center.x, center.y - 120f),
                        durationMillis = 600,
                    )
                }
                waitForIdle()
                onNode(
                        hasContentDescription("3 tracks"),
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                onNodeWithTag("NowPlayingTimerPanel").performTouchInput {
                    swipe(
                        Offset(center.x, 24f),
                        Offset(center.x, center.y),
                        durationMillis = 600,
                    )
                }
                waitForIdle()
                onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                onNodeWithTag("NowPlayingTimerPanel").assertIsDisplayed()
                onNode(
                        hasContentDescription("15 minutes"),
                        useUnmergedTree = true,
                    )
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value.mode ==
                        SleepTimerMode.Timed
                }
                val timedState = controller.sleepTimerState.value
                assertEquals(SleepTimerMode.Timed, timedState.mode)
                assertEquals(false, timedState.fadeEnabled)
                assertEquals(
                    true,
                    timedState.remainingMillis?.let { it in 1L..900_000L } ==
                        true,
                )

                onNode(
                        hasContentDescription("30 minutes"),
                        useUnmergedTree = true,
                    )
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value.remainingMillis?.let {
                        it in 900_001L..1_800_000L
                    } == true
                }

                onNode(
                        hasContentDescription("Cancel timer"),
                        useUnmergedTree = true,
                    )
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value == SleepTimerState()
                }
                onNode(
                        hasContentDescription("10-second fade"),
                        useUnmergedTree = true,
                    )
                    .performClick()
                onNode(
                        hasContentDescription("3 tracks"),
                        useUnmergedTree = true,
                    )
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value.mode ==
                        SleepTimerMode.TrackCount
                }
                assertEquals(
                    SleepTimerState(
                        mode = SleepTimerMode.TrackCount,
                        remainingTracks = 3,
                        fadeEnabled = true,
                    ),
                    controller.sleepTimerState.value,
                )

                onNode(
                        hasContentDescription("Cancel timer"),
                        useUnmergedTree = true,
                    )
                    .performScrollTo()
                    .performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.sleepTimerState.value == SleepTimerState()
                }
                controller.release()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queueBodyScrollAndReorderDoNotDismissPlayer() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val tracks =
                    (0 until 20).map { index ->
                        Track(
                            id = "drag-track-$index",
                            title = "Drag Track $index",
                            artist = "Artist",
                            album = "Album",
                            durationSeconds = 180,
                            accent = TrackAccent(0xFF123456, 0xFF654321),
                            source =
                                AudioSource.FilePath("/drag-track-$index.mp3"),
                        )
                    }
                val occurrences = tracks.mapIndexed { index, track ->
                    QueueOccurrence(
                        "drag-occurrence-$index",
                        track.toPlayableTrack(),
                    )
                }
                val controller = PlaybackController(FakePlaybackEngine())
                runBlocking {
                    controller.setOccurrenceQueue(
                        occurrences,
                        selectedOccurrenceId = "drag-occurrence-0",
                    )
                }
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                val playbackBefore = controller.state.value
                val queueBefore = occurrences.map { it.id }
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 1280.dp,
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "downward-player-swipe",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = tracks,
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                        desktopDropTarget = {
                            DesktopDropTargetPresentation(
                                isAvailable = true,
                                isActive = false,
                                feedback = "Player is closed",
                            )
                        },
                    )

                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()
                    onNodeWithTag("NowPlayingQueueButton").performClick()
                    waitForIdle()
                    onNodeWithContentDescription("Back to player")
                        .assertIsDisplayed()

                    val queuePanel = onNodeWithTag("NowPlayingQueuePanel")
                    queuePanel.performTouchInput {
                        swipe(
                            Offset(center.x, 36f),
                            Offset(center.x, center.y),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()

                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    queuePanel.assertDoesNotExist()
                    onNodeWithContentDescription("Back to player")
                        .assertDoesNotExist()
                    onNodeWithText("Player is closed").assertDoesNotExist()
                    assertEquals(playbackBefore, controller.state.value)
                    assertEquals(
                        queueBefore,
                        controller.state.value.queue.map { it.id },
                    )

                    onNodeWithTag("NowPlayingQueueButton").performClick()
                    waitForIdle()
                    queuePanel.assertIsDisplayed()
                    onNodeWithContentDescription("Back to player")
                        .assertIsDisplayed()

                    queuePanel.performTouchInput {
                        swipe(
                            Offset(center.x, height - 24f),
                            Offset(center.x, 24f),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()
                    onNodeWithTag("NowPlayingQueueList").performTouchInput {
                        swipe(
                            Offset(center.x, height - 24f),
                            Offset(center.x, 24f),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()
                    onNodeWithText("Drag Track 19").assertIsDisplayed()

                    onNodeWithTag("NowPlayingQueueList")
                        .performScrollToNode(
                            hasContentDescription(
                                "Reorder Drag Track 2 in queue",
                            ),
                        )
                    val secondHandle =
                        onNodeWithContentDescription(
                            "Reorder Drag Track 2 in queue",
                            useUnmergedTree = true,
                        )
                    onNodeWithTag("NowPlayingQueueList")
                        .performScrollToNode(
                            hasContentDescription(
                                "Reorder Drag Track 1 in queue",
                            ),
                        )
                    val firstHandle =
                        onNodeWithContentDescription(
                            "Reorder Drag Track 1 in queue",
                            useUnmergedTree = true,
                        )
                    val reorderDistance =
                        firstHandle.fetchSemanticsNode().boundsInRoot.center.y -
                            secondHandle
                                .fetchSemanticsNode()
                                .boundsInRoot
                                .center
                                .y
                    secondHandle.performTouchInput {
                        down(center)
                        advanceEventTime(600)
                        moveBy(Offset(0f, reorderDistance))
                        up()
                    }
                    val queueAfterReorder =
                        listOf(
                            "drag-occurrence-0",
                            "drag-occurrence-2",
                            "drag-occurrence-1",
                        ) + queueBefore.drop(3)
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.queue.map { it.id } ==
                            queueAfterReorder
                    }
                    assertEquals(
                        "drag-occurrence-0",
                        controller.state.value.currentOccurrenceId,
                    )
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    queuePanel.assertIsDisplayed()

                    onNodeWithContentDescription("Back to player")
                        .performTouchInput {
                            swipe(
                                center,
                                center + Offset(0f, 120f),
                                durationMillis = 600,
                            )
                        }
                    waitForIdle()
                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    onNodeWithTag("NowPlayingQueuePanel").assertDoesNotExist()
                    onNodeWithContentDescription("Back to player")
                        .assertDoesNotExist()
                    assertEquals(
                        queueAfterReorder,
                        controller.state.value.queue.map { it.id },
                    )

                    onNodeWithTag("NowPlayingQueueButton").performClick()
                    onNodeWithContentDescription("Back to player")
                        .performClick()
                    waitForIdle()
                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    onNodeWithTag("NowPlayingQueuePanel").assertDoesNotExist()
                    assertEquals(
                        queueAfterReorder,
                        controller.state.value.queue.map { it.id },
                    )
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dismissedPlayerSurfaceCannotReceiveInputDuringExitAnimation() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest(
                effectContext = TestMotionDurationScale(scaleFactor = 1f),
            ) {
                val track =
                    Track(
                        id = "player-exit",
                        title = "Player Exit",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/player-exit.mp3"),
                    )
                val backgroundTrack =
                    Track(
                        id = "background-only",
                        title = "Background Only",
                        artist = "Artist",
                        album = "Background Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/background-only.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                val queueBefore = controller.state.value.queue.map { it.id }
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 1280.dp,
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "player-exit",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(track, backgroundTrack),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )

                    onNodeWithContentDescription("Album Background Album")
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()
                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    waitForIdle()
                    onNodeWithContentDescription("Album Background Album")
                        .assertDoesNotExist()
                    onNodeWithTag("NowPlayingPlayPause")
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()

                    mainClock.autoAdvance = false
                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    mainClock.advanceTimeBy(32)
                    waitForIdle()

                    onNodeWithContentDescription("Album Background Album")
                        .assertDoesNotExist()
                    onNodeWithTag(
                            "NowPlayingFocusTarget",
                            useUnmergedTree = true,
                        )
                        .assertExists()
                    onNodeWithTag(
                            "NowPlayingContentRoot",
                            useUnmergedTree = true,
                        )
                        .assertIsDisplayed()
                    onNodeWithTag("NowPlayingPlayPause").assertDoesNotExist()
                    onRoot().performKeyInput {
                        keyDown(Key.Enter)
                        keyUp(Key.Enter)
                    }
                    onRoot().performKeyInput {
                        keyDown(Key.Spacebar)
                        keyUp(Key.Spacebar)
                    }
                    waitForIdle()
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    mainClock.advanceTimeBy(240)
                    mainClock.advanceTimeBy(16)
                    waitForIdle()
                    onNodeWithTag(
                            "NowPlayingContentRoot",
                            useUnmergedTree = true,
                        )
                        .assertDoesNotExist()
                    onNodeWithTag(
                            "NowPlayingBarRoot",
                            useUnmergedTree = true,
                        )
                        .assertIsFocused()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopQueueMutationActionsFollowShellDensity() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val currentTrack = track("current", "Current")
                val upcomingTrack = track("upcoming", "Upcoming")
                val controller = PlaybackController(FakePlaybackEngine())
                runBlocking {
                    controller.setOccurrenceQueue(
                        listOf(
                            QueueOccurrence(
                                "current",
                                currentTrack.toPlayableTrack(),
                            ),
                            QueueOccurrence(
                                "upcoming",
                                upcomingTrack.toPlayableTrack(),
                            ),
                        ),
                        "current",
                    )
                }

                mount(
                    width = 1_200.dp,
                    height = 800.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "desktop-queue-density",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(currentTrack, upcomingTrack),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )

                onNodeWithContentDescription("Queue").performClick()
                waitForIdle()
                onNodeWithContentDescription("Move Upcoming up in queue")
                    .assertHeightIsEqualTo(44.dp)
                val disabledMoveDown =
                    onNodeWithContentDescription("Move Upcoming down in queue")
                disabledMoveDown
                    .assert(
                        SemanticsMatcher.expectValue(
                            SemanticsProperties.Role,
                            Role.Button,
                        ),
                    )
                    .assertIsNotEnabled()
                    .assertHasNoClickAction()
                    .assert(
                        SemanticsMatcher(
                            "Disabled queue action is not focusable",
                        ) {
                            it.config.getOrNull(
                                SemanticsActions.RequestFocus,
                            ) == null
                        },
                    )
                val currentTrackId = controller.state.value.currentTrack?.id
                disabledMoveDown.performTouchInput { click(center) }
                waitForIdle()
                assertEquals(
                    currentTrackId,
                    controller.state.value.currentTrack?.id,
                    "Disabled queue action must not activate its parent row",
                )
                controller.release()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun sharedQueueMutationReordersAuthoritativeOccurrenceIds() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val currentTrack = track("current", "Current")
                val firstTrack = track("first", "First")
                val secondTrack = track("second", "Second")
                val controller = PlaybackController(FakePlaybackEngine())
                runBlocking {
                    controller.setOccurrenceQueue(
                        listOf(
                            QueueOccurrence(
                                "current",
                                currentTrack.toPlayableTrack(),
                            ),
                            QueueOccurrence(
                                "first", firstTrack.toPlayableTrack()),
                            QueueOccurrence(
                                "second", secondTrack.toPlayableTrack()),
                        ),
                        "current",
                    )
                }

                try {
                    mount(
                        width = 1280.dp,
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "shared-queue-mutation",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(currentTrack, firstTrack, secondTrack),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )

                    onNodeWithContentDescription("Queue").performClick()
                    onNodeWithContentDescription("Move Second up in queue")
                        .performClick()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.queue.map { it.id } ==
                            listOf("current", "second", "first")
                    }
                    assertEquals(
                        listOf("current", "second", "first"),
                        controller.state.value.queue.map { it.id },
                    )
                } finally {
                    controller.release()
                }
            }
        }

    @Test
    fun sharedQueueMutationRejectionRefreshesFromAuthoritativeState() =
        runBlocking {
            fun track(id: String, title: String) =
                Track(
                    id = id,
                    title = title,
                    artist = "Artist",
                    album = "Album",
                    durationSeconds = 180,
                    accent = TrackAccent(0xFF123456, 0xFF654321),
                    source = AudioSource.FilePath("/$id.mp3"),
                )

            val currentTrack = track("stale-current", "Current")
            val staleTrack = track("stale-removed", "Removed")
            val remainingTrack = track("stale-remaining", "Remaining")
            val controller = PlaybackController(FakePlaybackEngine())
            try {
                controller.setOccurrenceQueue(
                    listOf(
                        QueueOccurrence(
                            "stale-current",
                            currentTrack.toPlayableTrack(),
                        ),
                        QueueOccurrence(
                            "stale-removed",
                            staleTrack.toPlayableTrack(),
                        ),
                        QueueOccurrence(
                            "stale-remaining",
                            remainingTrack.toPlayableTrack(),
                        ),
                    ),
                    "stale-current",
                )
                val dispatcher =
                    QueueMutationDispatcher(
                        state = controller.state,
                        reorderCommand = controller::reorderUpcoming,
                        removeCommand = controller::removeUpcoming,
                        clearCommand = controller::clearUpcoming,
                    )
                assertEquals(
                    listOf("stale-current", "stale-removed", "stale-remaining"),
                    controller.state.value.queue.map { it.id },
                )

                // The authoritative queue drops the upcoming occurrence the
                // dispatcher is about to address.
                controller.setOccurrenceQueue(
                    listOf(
                        QueueOccurrence(
                            "stale-current",
                            currentTrack.toPlayableTrack(),
                        ),
                        QueueOccurrence(
                            "stale-remaining",
                            remainingTrack.toPlayableTrack(),
                        ),
                    ),
                    "stale-current",
                )

                val feedback = dispatcher.remove("stale-removed")

                assertTrue(
                    feedback.showQueueChanged,
                    "A rejected queue mutation must report that the queue changed",
                )
                assertEquals(
                    listOf("stale-current", "stale-remaining"),
                    feedback.refreshedState.queue.map { it.id },
                    "Rejected mutation must refresh from the authoritative queue",
                )
                assertEquals(
                    controller.state.value.queue.map { it.id },
                    feedback.refreshedState.queue.map { it.id },
                )
                assertEquals(
                    controller.state.value.currentOccurrenceId,
                    feedback.refreshedState.currentOccurrenceId,
                )
            } finally {
                controller.release()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playerDialogAndQueueDraftSurviveAppearanceChangeAndTrackReplacement() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val first = track("dialog-focus-first", "First")
                val second = track("dialog-focus-second", "Second")
                val third = track("dialog-focus-third", "Third")
                val controller = PlaybackController(FakePlaybackEngine())
                val appearance = mutableStateOf(defaultSettingsAppearance())
                controller.setQueue(
                    listOf(
                        first.toPlayableTrack(),
                        second.toPlayableTrack(),
                        third.toPlayableTrack(),
                    ),
                    selectedTrackId = first.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                try {
                    mount(
                        width = { 1280.dp },
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "player-dialog-focus",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(first, second, third),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                        playlistState =
                            PlaylistState(
                                confirmedSnapshot = PlaylistSnapshot(),
                                hasConfirmedSnapshot = true,
                            ),
                        settingsAppearanceProvider = { appearance.value },
                    )

                    onNodeWithTag("NowPlayingShellPlacement").performClick()
                    onNodeWithTag("NowPlayingPlayPause")
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()
                    onNodeWithTag("NowPlayingPlayPause").assertIsFocused()
                    onNodeWithTag("NowPlayingQueueButton").performClick()
                    val saveQueueButton =
                        onNode(hasText("Save queue as playlist"))
                    saveQueueButton
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()
                    saveQueueButton.performClick()
                    val dialog =
                        onNode(
                            SemanticsMatcher.expectValue(
                                SemanticsProperties.PaneTitle,
                                "Save queue as playlist",
                            ),
                            useUnmergedTree = true,
                        )
                    dialog.assertIsFocused()

                    onNodeWithTag("NowPlayingFocusTarget").assertDoesNotExist()
                    val dialogConfirm = onNodeWithTag("playlist-name-confirm")
                    dialogConfirm
                        .performSemanticsAction(SemanticsActions.RequestFocus)
                        .assertIsFocused()
                    onRoot().performKeyInput {
                        keyDown(Key.Tab)
                        keyUp(Key.Tab)
                    }
                    val playerRootFocused =
                        onNodeWithTag(
                                "NowPlayingFocusTarget",
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                            .config
                            .getOrNull(SemanticsProperties.Focused) == true
                    assertFalse(
                        playerRootFocused,
                        "Tab from the dialog must not focus the player root",
                    )
                    val playerFocusAfterDialogConfirm =
                        onAllNodes(
                                SemanticsMatcher.expectValue(
                                        SemanticsProperties.Focused,
                                        true,
                                    )
                                    .and(
                                        hasAnyAncestor(
                                            hasTestTag("NowPlayingFocusTarget"),
                                        ),
                                    ),
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNodes()
                    assertTrue(
                        playerFocusAfterDialogConfirm.isEmpty(),
                        "Tab from the dialog's final action must not focus the player",
                    )
                    repeat(8) { traversal ->
                        onRoot().performKeyInput {
                            keyDown(Key.Tab)
                            keyUp(Key.Tab)
                        }
                        val focusedPlayerTargets =
                            onAllNodes(
                                    SemanticsMatcher.expectValue(
                                            SemanticsProperties.Focused,
                                            true,
                                        )
                                        .and(
                                            hasAnyAncestor(
                                                hasTestTag(
                                                    "NowPlayingFocusTarget",
                                                ),
                                            ),
                                        ),
                                    useUnmergedTree = true,
                                )
                                .fetchSemanticsNodes()
                        assertTrue(
                            focusedPlayerTargets.isEmpty(),
                            "Tab traversal $traversal must stay above the dialog",
                        )
                    }
                    onRoot().performKeyInput {
                        keyDown(Key.ShiftLeft)
                        keyDown(Key.Tab)
                        keyUp(Key.Tab)
                        keyUp(Key.ShiftLeft)
                    }
                    dialog.assertIsDisplayed()
                    val playerFocusAfterReverseTraversal =
                        onNodeWithTag(
                                "NowPlayingFocusTarget",
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                            .config
                            .getOrNull(SemanticsProperties.Focused) == true
                    assertFalse(
                        playerFocusAfterReverseTraversal,
                        "Reverse traversal must not move focus under the dialog",
                    )
                    val focusedPlayerTargets =
                        onAllNodes(
                                SemanticsMatcher.expectValue(
                                        SemanticsProperties.Focused,
                                        true,
                                    )
                                    .and(
                                        hasAnyAncestor(
                                            hasTestTag("NowPlayingFocusTarget"),
                                        ),
                                    ),
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNodes()
                    assertTrue(
                        focusedPlayerTargets.isEmpty(),
                        "No player descendant can receive focus beneath a dialog",
                    )

                    dialog.performSemanticsAction(SemanticsActions.Dismiss)
                    waitForIdle()
                    val focusedAfterDialogDismissal =
                        onAllNodes(
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.Focused,
                                    true,
                                ),
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNodes()
                    assertTrue(
                        saveQueueButton
                            .fetchSemanticsNode()
                            .config
                            .getOrNull(SemanticsProperties.Focused) == true,
                        "Focus after dialog dismissal: " +
                            focusedAfterDialogDismissal.joinToString {
                                it.config.toString()
                            },
                    )

                    saveQueueButton.performClick()
                    dialog.assertIsFocused()
                    onNode(hasSetTextAction())
                        .performTextInput(
                            "Keep this draft",
                        )
                    appearance.value =
                        appearance.value.copy(
                            paletteId =
                                com.eterocell.rhythhaus.theme.HausPaletteId
                                    .Iris,
                        )
                    waitForIdle()
                    dialog.assertIsDisplayed()
                    onNode(hasText("Keep this draft")).assertIsDisplayed()
                    assertEquals(
                        first.id,
                        controller.state.value.currentTrack?.id,
                    )
                    controller.skipToNext()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.currentTrack?.id == second.id
                    }
                    dialog.assertIsDisplayed()
                    onNode(hasText("Keep this draft")).assertIsDisplayed()
                } finally {
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun protectedOverlayBlocksUnderlyingPlayerDrag() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val firstTrack = track("a", "First")
                val secondTrack = track("b", "Second")
                val first = firstTrack.toPlayableTrack()
                val second = secondTrack.toPlayableTrack()
                val controller = PlaybackController(FakePlaybackEngine())
                runBlocking {
                    controller.setOccurrenceQueue(
                        listOf(
                            QueueOccurrence("played", second),
                            QueueOccurrence("current", first),
                            QueueOccurrence("next", second),
                            QueueOccurrence("duplicate", first),
                        ),
                        "current",
                    )
                }
                val before = controller.state.value
                val savedEntries = mutableListOf<PlaylistEntry>()
                var mutationAttempts = 0
                val repository =
                    object : PlaylistRepository by EmptyPlaylistRepository {
                        private var savedPlaylist: PlaylistSummary? = null

                        override fun createWithEntries(
                            name: String,
                            trackIds: List<String>,
                        ): PlaylistSummary {
                            val playlist = PlaylistSummary("saved", name, 1, 1)
                            savedPlaylist = playlist
                            savedEntries.clear()
                            savedEntries +=
                                trackIds.mapIndexed { index, trackId ->
                                    PlaylistEntry(
                                        "entry-$index",
                                        playlist.id,
                                        trackId,
                                        index,
                                        1,
                                    )
                                }
                            return playlist
                        }

                        override fun playlists(): List<PlaylistSummary> =
                            listOfNotNull(savedPlaylist)

                        override fun playlist(id: String): PlaylistSummary? =
                            savedPlaylist?.takeIf {
                                it.id == id
                            }

                        override fun entries(playlistId: String) =
                            if (savedPlaylist?.id == playlistId) {
                                savedEntries.toList()
                            } else {
                                emptyList()
                            }
                    }
                val owner = PlaylistStateOwner(repository, Dispatchers.Default)
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 1280.dp,
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "queue-player",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(firstTrack, secondTrack),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                        playlistRepository = repository,
                        playlistState =
                            PlaylistState(
                                confirmedSnapshot = PlaylistSnapshot(),
                                hasConfirmedSnapshot = true,
                            ),
                        onPlaylistMutation = { mutation, onOutcome ->
                            mutationAttempts++
                            onOutcome(
                                if (mutationAttempts == 1) {
                                    PlaylistStateAction.MutationFailed(
                                        "temporary failure",
                                    )
                                } else {
                                    runBlocking {
                                        owner.mutate(mutation = mutation)
                                    }
                                },
                            )
                        },
                    )

                    onNode(hasContentDescription("Queue")).performClick()
                    onNode(hasContentDescription("Back to player"))
                        .assertIsDisplayed()
                    val playerWidth =
                        onNodeWithTag("NowPlayingContentRoot")
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .width
                    onNode(hasText("Save queue as playlist")).performClick()
                    val dialogHostWidth =
                        onNode(
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.PaneTitle,
                                    "Save queue as playlist",
                                ),
                            )
                            .fetchSemanticsNode()
                            .boundsInRoot
                            .width
                    assertTrue(
                        dialogHostWidth >= playerWidth * 0.75f,
                        "queue dialog host width $dialogHostWidth must span " +
                            "the expanded player width $playerWidth",
                    )
                    val dialogPanel =
                        onNodeWithTag(
                                "HausDialogPanel",
                                useUnmergedTree = true,
                            )
                            .fetchSemanticsNode()
                    val dialogPanelWidthDp =
                        dialogPanel.boundsInRoot.width /
                            dialogPanel.layoutInfo.density.density
                    assertTrue(
                        dialogPanelWidthDp > 280f,
                        "queue dialog panel width $dialogPanelWidthDp dp must " +
                            "exceed the 280dp queue rail",
                    )
                    onNode(hasSetTextAction()).performTextInput("Trip")

                    onNodeWithTag(
                            "HausDialogPanel",
                            useUnmergedTree = true,
                        )
                        .performTouchInput {
                            swipe(
                                Offset(center.x, center.y - 80f),
                                Offset(center.x, center.y + 80f),
                                durationMillis = 600,
                            )
                        }
                    waitForIdle()
                    onNode(hasText("Save queue as playlist"))
                        .assertIsDisplayed()
                    onNode(hasText("Create playlist")).assertIsDisplayed()
                    onNode(hasText("Trip")).assertIsDisplayed()
                    assertEquals(before.queue, controller.state.value.queue)
                    assertEquals(
                        "current",
                        controller.state.value.currentOccurrenceId,
                    )

                    input.backStarted(NavigationEvent())
                    input.backCancelled()
                    waitForIdle()
                    onNode(hasText("Create playlist")).assertIsDisplayed()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNode(hasText("Save queue as playlist"))
                        .assertIsDisplayed()
                    onNode(hasContentDescription("Back to player"))
                        .assertIsDisplayed()
                    assertEquals(0, mutationAttempts)
                    assertTrue(savedEntries.isEmpty())

                    onNode(hasText("Save queue as playlist")).performClick()
                    onNode(hasSetTextAction()).performTextInput("Cancel this")
                    onNode(hasText("Cancel")).performClick()
                    waitForIdle()
                    assertEquals(0, mutationAttempts)
                    assertTrue(savedEntries.isEmpty())

                    onNode(hasText("Save queue as playlist")).performClick()
                    onNode(hasSetTextAction()).performTextInput("Trip")
                    controller.skipToNext()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.currentOccurrenceId == "next"
                    }
                    onNode(hasText("Save queue as playlist"))
                        .assertIsDisplayed()
                    onNode(hasText("Trip")).assertIsDisplayed()
                    onNode(
                            hasContentDescription("Back to player"),
                            useUnmergedTree = true,
                        )
                        .assertExists()

                    onNode(hasText("Create playlist")).performClick()
                    waitForIdle()
                    assertEquals(1, mutationAttempts)
                    onNode(hasText("Could not save playlist changes"))
                        .assertIsDisplayed()
                    onNode(hasText("Trip")).assertIsDisplayed()
                    assertTrue(savedEntries.isEmpty())

                    onNode(hasText("Create playlist")).performClick()
                    waitForIdle()
                    assertEquals(2, mutationAttempts)
                    assertEquals(
                        listOf("a", "b", "a"),
                        savedEntries.map { it.trackId },
                    )
                    assertEquals(
                        listOf(0, 1, 2), savedEntries.map { it.position })
                    assertEquals(before.queue, controller.state.value.queue)
                    assertEquals(
                        "next",
                        controller.state.value.currentOccurrenceId,
                    )
                    onNode(hasContentDescription("Back to player"))
                        .assertIsDisplayed()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onAllNodes(hasContentDescription("Back to player"))
                        .assertCountEquals(0)
                    onNode(hasText("Timer"))
                        .performScrollTo()
                        .assertIsDisplayed()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNodeWithTag(
                            "NowPlayingShellPlacement",
                            useUnmergedTree = true,
                        )
                        .assertIsDisplayed()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun platformBackKeepsExistingLayerPrecedence() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "back-precedence-track",
                        title = "Back precedence track",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source =
                            AudioSource.FilePath("/back-precedence-track.mp3"),
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(track.toPlayableTrack()),
                    selectedTrackId = track.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                val queueBefore = controller.state.value.queue.map { it.id }
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 1280.dp,
                        height = 800.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                "back-precedence-route",
                                "source",
                                ScanStatus.Completed,
                                1,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )
                    onNodeWithTag(
                            "NowPlayingShellPlacement",
                            useUnmergedTree = true,
                        )
                        .performClick()
                    onNode(hasContentDescription("Queue")).performClick()
                    onNodeWithTag("NowPlayingQueuePanel").assertIsDisplayed()
                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()

                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    onNodeWithTag("NowPlayingQueuePanel").assertDoesNotExist()
                    onNode(hasContentDescription("Back to player"))
                        .assertDoesNotExist()
                    assertEquals(
                        track.id,
                        controller.state.value.currentTrack?.id,
                    )
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    assertEquals(
                        queueBefore, controller.state.value.queue.map { it.id })

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNodeWithTag("NowPlayingContentRoot").assertDoesNotExist()
                    onNodeWithTag("NowPlayingShellPlacement")
                        .assertIsDisplayed()
                    assertEquals(
                        track.id,
                        controller.state.value.currentTrack?.id,
                    )
                    assertEquals(
                        PlaybackStatus.Paused,
                        controller.state.value.status,
                    )
                    assertEquals(
                        queueBefore, controller.state.value.queue.map { it.id })
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun replacingLyricsTrackClosesReaderBeforePlayerBack() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val first =
                    Track(
                        id = "lyrics-a",
                        title = "First with lyrics",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/lyrics-a.mp3"),
                        lyrics =
                            List(40) { index ->
                                    "First local lyric ${index + 1}"
                                }
                                .joinToString("\n"),
                    )
                val second =
                    first.copy(
                        id = "lyrics-b",
                        title = "Second without lyrics",
                        source = AudioSource.FilePath("/lyrics-b.mp3"),
                        lyrics = null,
                    )
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(first.toPlayableTrack(), second.toPlayableTrack()),
                    selectedTrackId = first.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                val input = DirectNavigationEventInput()
                TestNavigationOwner.navigationEventDispatcher.addInput(input)
                try {
                    mount(
                        width = 600.dp,
                        height = 400.dp,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "lyrics-player",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(first, second),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                    )

                    onNode(hasContentDescription("Queue")).performClick()
                    mainClock.advanceTimeBy(300)
                    waitForIdle()
                    onNode(hasContentDescription("Back to player"))
                        .assertIsDisplayed()
                    onNode(hasContentDescription("Back to player"))
                        .performClick()
                    mainClock.advanceTimeBy(300)
                    waitForIdle()
                    onNodeWithTag("NowPlayingTitle")
                        .performScrollTo()
                        .assertTextContains(first.title)
                        .assertIsDisplayed()
                    onNode(hasText("Lyrics") and hasClickAction())
                        .performScrollTo()
                        .performClick()
                    waitForIdle()
                    onNode(hasContentDescription("Close lyrics"))
                        .assertIsDisplayed()
                    repeat(8) {
                        onNodeWithTag("NowPlayingLyricsList")
                            .performTouchInput {
                                swipe(
                                    Offset(center.x, center.y + 120f),
                                    Offset(center.x, center.y - 120f),
                                    durationMillis = 500,
                                )
                            }
                    }
                    onNode(hasText("First local lyric 40")).assertIsDisplayed()
                    onNodeWithTag("NowPlayingContentRoot").performTouchInput {
                        swipe(
                            Offset(center.x, 40f),
                            Offset(center.x, center.y / 2f),
                            durationMillis = 600,
                        )
                    }
                    waitForIdle()
                    onNodeWithTag("NowPlayingContentRoot").assertIsDisplayed()
                    onNodeWithTag("NowPlayingLyricsList").assertIsDisplayed()
                    onNode(hasText("First local lyric 40")).assertIsDisplayed()

                    controller.skipToNext()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.currentTrack?.id == second.id
                    }
                    waitForIdle()

                    onNodeWithTag("NowPlayingTitle")
                        .performScrollTo()
                        .assertTextContains(second.title)
                        .assertIsDisplayed()
                    onAllNodes(hasText("First local lyric"))
                        .assertCountEquals(0)
                    onAllNodes(hasText("Lyrics")).assertCountEquals(0)
                    onNode(hasText("Timer"))
                        .performScrollTo()
                        .assertIsDisplayed()

                    input.backStarted(NavigationEvent())
                    input.backCompleted()
                    waitForIdle()
                    onNodeWithTag("NowPlayingShellPlacement")
                        .assertIsDisplayed()
                } finally {
                    TestNavigationOwner.navigationEventDispatcher.removeInput(
                        input,
                    )
                    controller.release()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun miniPlayerNextUsesPlaybackControllerSkipToNext() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val first =
                    Track(
                        id = "next-a",
                        title = "First",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/first.mp3"),
                    )
                val second = first.copy(id = "next-b", title = "Second")
                val controller = PlaybackController(FakePlaybackEngine())
                controller.setQueue(
                    listOf(first.toPlayableTrack(), second.toPlayableTrack()),
                    selectedTrackId = first.id,
                )
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.status == PlaybackStatus.Paused
                }
                mount(
                    width = 600.dp,
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "next-player",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(first, second),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    playbackController = controller,
                )

                onNode(hasContentDescription("Next track")).performClick()
                waitUntil(timeoutMillis = 5_000) {
                    controller.state.value.currentTrack?.id == second.id
                }
                assertEquals(second.id, controller.state.value.currentTrack?.id)
                controller.release()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopBannerLeavesCompactSearchFieldReachable() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val picker = CountingPicker()
                mount(
                    width = 600.dp,
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "drop-search",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = picker,
                    callbacks = CallbackRecorder(),
                    desktopDropTarget = {
                        DesktopDropTargetPresentation(
                            true, false, "Unsupported item")
                    },
                )
                onNodeWithTag("AppNavigationSearch", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                val field = onNode(hasSetTextAction())
                field.assertIsDisplayed()
                val bannerButton =
                    onNode(hasContentDescription("Choose music folder"))
                bannerButton.assertIsDisplayed()
                assertTrue(
                    bannerButton.fetchSemanticsNode().boundsInRoot.bottom <=
                        field.fetchSemanticsNode().boundsInRoot.top,
                    "Import action must not overlap the search field",
                )
                field.performClick().performTextInput("reachable")
                waitForIdle()
                assertEquals(0, picker.launchCalls)
                onNode(hasText("Unsupported item"), useUnmergedTree = true)
                    .assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun desktopBannerIsHiddenForNonemptyLibraryWhileDropRemainsAvailable() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "existing-track",
                        title = "Existing track",
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF123456, 0xFF654321),
                        source = AudioSource.FilePath("/existing-track.mp3"),
                    )
                var dropTargetAvailable by mutableStateOf(true)
                var dropTargetActive by mutableStateOf(false)
                var nativeDropAvailable = false
                mount(
                    width = 600.dp,
                    height = 400.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "populated-drop-search",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = listOf(track),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    desktopDropTarget = {
                        DesktopDropTargetPresentation(
                            dropTargetAvailable, dropTargetActive)
                    },
                    onDesktopDropAvailabilityChanged = {
                        nativeDropAvailable = it
                    },
                )

                onNodeWithTag("AppNavigationSearch", useUnmergedTree = true)
                    .performClick()
                val searchField = onNode(hasSetTextAction())
                searchField.assertIsDisplayed()
                onNodeWithText("Drop music files here.", useUnmergedTree = true)
                    .assertDoesNotExist()
                onNodeWithContentDescription("Choose music folder")
                    .assertDoesNotExist()
                assertTrue(nativeDropAvailable)

                val searchFieldTopWithoutBanner =
                    searchField.fetchSemanticsNode().boundsInRoot.top
                runOnIdle { dropTargetActive = true }
                onNodeWithText(
                        "Release to add folders or audio files.",
                        useUnmergedTree = true,
                    )
                    .assertIsDisplayed()
                onNodeWithText("Drop music files here.", useUnmergedTree = true)
                    .assertDoesNotExist()
                onNodeWithContentDescription("Choose music folder")
                    .assertDoesNotExist()
                runOnIdle {
                    dropTargetActive = false
                    dropTargetAvailable = false
                }
                waitForIdle()
                val searchFieldTopWithoutDropTarget =
                    searchField.fetchSemanticsNode().boundsInRoot.top
                assertEquals(
                    searchFieldTopWithoutBanner,
                    searchFieldTopWithoutDropTarget,
                    "A populated library must not reserve space for the import banner",
                )
            }
        }

    private inline fun <T> withDefaultLocale(
        locale: Locale,
        block: () -> T,
    ): T {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(locale)
        return try {
            block()
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun searchMetadataMenuOpensEditorAndCancelPreservesQuery() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val selectedTrack =
                    LibraryTrack(
                        id = "search-target",
                        sourceId = "source",
                        sourceLocalKey = "search-target.mp3",
                        audioSource =
                            AudioSource.FilePath("/music/search-target.mp3"),
                        displayName = "search-target.mp3",
                        title = "Search target",
                        artist = "Artist",
                        album = "Album",
                        durationMillis = 1,
                        sizeBytes = null,
                        modifiedAtEpochMillis = null,
                        lastSeenScanId = "scan",
                        createdAtEpochMillis = 1L,
                        updatedAtEpochMillis = 1L,
                    )
                val loadedTrackIds = mutableListOf<String>()
                val saveRequests =
                    mutableListOf<Pair<String, TrackMetadataOverride>>()

                mount(
                    width = { 390.dp },
                    source = source(),
                    scanSession =
                        ScanSession(
                            "search-metadata",
                            "source",
                            ScanStatus.Completed,
                            1L,
                        ),
                    libraryTracks = listOf(selectedTrack),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    onLoadTrackMetadata = { trackId ->
                        loadedTrackIds += trackId
                        TrackMetadataEditorData(
                            scannedTrack = selectedTrack,
                            overrides = TrackMetadataOverride(),
                        )
                    },
                    onSetTrackMetadataOverride = { trackId, override ->
                        saveRequests += trackId to override
                        true
                    },
                )
                onNodeWithTag("AppNavigationSearch").performClick()
                onNode(hasSetTextAction()).performTextInput("Search target")
                waitForIdle()

                onNodeWithTag("search-result-more-actions").performClick()
                onNodeWithTag("search-result-edit-metadata").performClick()
                waitUntil(timeoutMillis = 5_000) {
                    runCatching {
                        onNodeWithTag("metadata-title").assertIsDisplayed()
                    }
                        .isSuccess
                }
                onNodeWithTag("metadata-title").assertIsDisplayed()
                assertTrue(loadedTrackIds.isNotEmpty())
                assertTrue(loadedTrackIds.all { it == selectedTrack.id })

                onNodeWithText("Cancel").performClick()
                waitForIdle()

                assertTrue(saveRequests.isEmpty())
                onNode(hasSetTextAction()).assertTextContains("Search target")
                onNodeWithTag("search-result-more-actions").assertIsDisplayed()
            }
        }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.mount(
        width: Dp,
        height: Dp = 900.dp,
        source: LibrarySource,
        scanSession: ScanSession,
        scanErrors: List<ScanError> = emptyList(),
        tracks: List<Track> = emptyList(),
        libraryTracks: List<LibraryTrack> = emptyList(),
        picker: CountingPicker,
        callbacks: CallbackRecorder,
        playbackController: PlaybackController =
            PlaybackController(FakePlaybackEngine()),
        favoriteTrackIds: () -> Set<String> = { emptySet() },
        playHistory: Map<String, TrackPlayHistory> = emptyMap(),
        createdAtByTrackId: Map<String, Long> = emptyMap(),
        onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
        playlistRepository: PlaylistRepository = EmptyPlaylistRepository,
        playlistState: PlaylistState = PlaylistState(),
        onPlaylistMutation:
            (
                PlaylistRepository.() -> Unit,
                (PlaylistStateAction) -> Unit) -> Unit =
            { _, _ ->
            },
        onPlaylistStateAction: (PlaylistStateAction) -> Unit = {},
        playlistBackupState: () -> PlaylistBackupUiState = {
            PlaylistBackupUiState()
        },
        backupDocumentAvailable: Boolean = false,
        onOpenPlaylistBackup: () -> Unit = {},
        onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit = {},
        onConfirmPlaylistBackup: () -> Unit = {},
        desktopDropTarget: () -> DesktopDropTargetPresentation = {
            DesktopDropTargetPresentation.Disabled
        },
        onDesktopDropAvailabilityChanged: (Boolean) -> Unit = {},
        onLoadTrackMetadata: (suspend (String) -> TrackMetadataEditorData?)? =
            null,
        onSetTrackMetadataOverride:
            (suspend (String, TrackMetadataOverride) -> Boolean)? =
            null,
        settingsAppearanceProvider:
            (() -> com.eterocell.rhythhaus.settings.SettingsAppearanceState)? =
            null,
    ) {
        mount(
            width = { width },
            height = height,
            source = source,
            scanSession = scanSession,
            scanErrors = scanErrors,
            tracks = tracks,
            libraryTracks = libraryTracks,
            picker = picker,
            callbacks = callbacks,
            playbackController = playbackController,
            favoriteTrackIds = favoriteTrackIds,
            playHistory = playHistory,
            createdAtByTrackId = createdAtByTrackId,
            onSetTrackFavorite = onSetTrackFavorite,
            playlistRepository = playlistRepository,
            playlistState = playlistState,
            onPlaylistMutation = onPlaylistMutation,
            onPlaylistStateAction = onPlaylistStateAction,
            playlistBackupState = playlistBackupState,
            backupDocumentAvailable = backupDocumentAvailable,
            onOpenPlaylistBackup = onOpenPlaylistBackup,
            onPlaylistBackupAction = onPlaylistBackupAction,
            onConfirmPlaylistBackup = onConfirmPlaylistBackup,
            desktopDropTarget = desktopDropTarget,
            onDesktopDropAvailabilityChanged = onDesktopDropAvailabilityChanged,
            onLoadTrackMetadata = onLoadTrackMetadata,
            onSetTrackMetadataOverride = onSetTrackMetadataOverride,
            settingsAppearanceProvider = settingsAppearanceProvider,
        )
    }

    @OptIn(ExperimentalTestApi::class)
    private fun androidx.compose.ui.test.ComposeUiTest.mount(
        width: () -> Dp,
        height: Dp = 900.dp,
        source: LibrarySource,
        scanSession: ScanSession,
        scanErrors: List<ScanError> = emptyList(),
        tracks: List<Track> = emptyList(),
        libraryTracks: List<LibraryTrack> = emptyList(),
        picker: CountingPicker,
        callbacks: CallbackRecorder,
        playbackController: PlaybackController =
            PlaybackController(FakePlaybackEngine()),
        favoriteTrackIds: () -> Set<String> = { emptySet() },
        playHistory: Map<String, TrackPlayHistory> = emptyMap(),
        createdAtByTrackId: Map<String, Long> = emptyMap(),
        onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
        playlistRepository: PlaylistRepository = EmptyPlaylistRepository,
        playlistState: PlaylistState = PlaylistState(),
        playlistStateProvider: () -> PlaylistState = { playlistState },
        onPlaylistMutation:
            (
                PlaylistRepository.() -> Unit,
                (PlaylistStateAction) -> Unit) -> Unit =
            { _, _ ->
            },
        onPlaylistStateAction: (PlaylistStateAction) -> Unit = {},
        playlistBackupState: () -> PlaylistBackupUiState = {
            PlaylistBackupUiState()
        },
        backupDocumentAvailable: Boolean = false,
        onOpenPlaylistBackup: () -> Unit = {},
        onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit = {},
        onConfirmPlaylistBackup: () -> Unit = {},
        desktopDropTarget: () -> DesktopDropTargetPresentation = {
            DesktopDropTargetPresentation.Disabled
        },
        onDesktopDropAvailabilityChanged: (Boolean) -> Unit = {},
        onLoadTrackMetadata: (suspend (String) -> TrackMetadataEditorData?)? =
            null,
        onSetTrackMetadataOverride:
            (suspend (String, TrackMetadataOverride) -> Boolean)? =
            null,
        settingsAppearanceProvider:
            (() -> com.eterocell.rhythhaus.settings.SettingsAppearanceState)? =
            null,
    ) {
        setContent {
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides
                    TestNavigationOwner,
            ) {
                Box(Modifier.size(width(), height)) {
                    val displayedPlaylistState = playlistStateProvider()
                    val settingsAppearance =
                        settingsAppearanceProvider?.invoke()
                            ?: defaultSettingsAppearance()
                    Box(
                        Modifier.size(1.dp).semantics {
                            this[SemanticsProperties.ContentDescription] =
                                listOf(
                                    "debug-browser-selection-count-${displayedPlaylistState.browser?.selectedTrackIds?.size ?: -1}",
                                )
                        },
                    )
                    val renderShell: @Composable () -> Unit = {
                        LibraryHomeScreen(
                            snapshot =
                                LibrarySnapshot(
                                    "Library",
                                    "",
                                    tracks,
                                    playbackController.state.value.currentTrack
                                        ?.id,
                                ),
                            libraryTracks = libraryTracks,
                            tagLibReader = UnusedTagLibReader,
                            playbackController = playbackController,
                            playlistRepository = playlistRepository,
                            playlistState = displayedPlaylistState,
                            playlistBackupState = playlistBackupState(),
                            backupDocumentAvailable = backupDocumentAvailable,
                            onPlaylistStateAction = onPlaylistStateAction,
                            onRefreshPlaylists = {},
                            onPlaylistMutation = onPlaylistMutation,
                            onExportPlaylists = {},
                            onOpenPlaylistBackup = onOpenPlaylistBackup,
                            onExportPlaylistFormat = {
                                _,
                                _: PlaylistDocumentFormat ->
                            },
                            onOpenPlaylistFormat = { _: PlaylistDocumentFormat
                                ->
                            },
                            onConfirmPlaylistBackup = onConfirmPlaylistBackup,
                            onPlaylistBackupAction = onPlaylistBackupAction,
                            sources = listOf(source),
                            folderPickerLauncher = picker,
                            sourcePickerActionVisible = true,
                            importMessage = null,
                            scanProgress = ScanProgress(scanSession),
                            latestCompletedScanSession = null,
                            scanErrors = scanErrors,
                            scanJob = null,
                            coordinatorMutationsEnabled = true,
                            settingsAppearance = settingsAppearance,
                            replayGainState =
                                com.eterocell.rhythhaus.playback
                                    .ReplayGainPreferenceState(isBusy = false),
                            onReplayGainModeSelected = {},
                            crossfadeState =
                                com.eterocell.rhythhaus.playback
                                    .CrossfadePreferenceState(isBusy = false),
                            onCrossfadeDurationSelected = {},
                            onThemeModeSelected = {},
                            onPaletteSelected = {},
                            onDynamicColorSelected = {},
                            onRetryAppearanceSave = {},
                            onClearLibrary = {},
                            onRescanSource = {},
                            onRemoveSource = {},
                            onRemoveMissingTracks = { _, _ -> },
                            onCancelScan = { callbacks.cancelCalls++ },
                            sourceIdByTrackId = emptyMap(),
                            modifiedAtByTrackId = emptyMap(),
                            favoriteTrackIds = favoriteTrackIds(),
                            playHistory = playHistory,
                            createdAtByTrackId = createdAtByTrackId,
                            onSetTrackFavorite = onSetTrackFavorite,
                            desktopDropTarget = desktopDropTarget(),
                            onDesktopDropAvailabilityChanged =
                                onDesktopDropAvailabilityChanged,
                            onLoadTrackMetadata = onLoadTrackMetadata,
                            onSetTrackMetadataOverride =
                                onSetTrackMetadataOverride,
                        )
                    }
                    if (settingsAppearanceProvider != null) {
                        val preferences =
                            com.eterocell.rhythhaus.theme.ThemePreferenceState(
                                mode = settingsAppearance.themeMode,
                                paletteId = settingsAppearance.paletteId,
                                dynamicColorEnabled =
                                    settingsAppearance.dynamicColorEnabled,
                            )
                        val resolvedTheme =
                            com.eterocell.rhythhaus.resolveSharedHausTheme(
                                preferences = preferences,
                                systemIsDark = settingsAppearance.isDark,
                                dynamicColorAvailable =
                                    settingsAppearance.dynamicColorAvailable,
                                dynamicRoles = null,
                            )
                        com.eterocell.rhythhaus.RhythHausTheme(resolvedTheme) {
                            renderShell()
                        }
                    } else {
                        renderShell()
                    }
                }
            }
        }
        waitForIdle()
    }

    private fun playlistBackupPreview() =
        PlaylistBackupPreview(
            0,
            emptyList(),
            emptyList(),
            PlaylistBackupCounts(0, 0, 0),
            true,
        )

    private fun source() =
        LibrarySource(
            id = "source",
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = "Music",
            handle = "/music",
            createdAtEpochMillis = 1L,
        )

    private fun defaultSettingsAppearance() =
        com.eterocell.rhythhaus.settings.SettingsAppearanceState(
            themeMode = RhythHausThemeMode.System,
            paletteId = com.eterocell.rhythhaus.theme.HausPaletteId.Moss,
            isDark = false,
            dynamicColorAvailable = false,
            dynamicColorEnabled = false,
            colorSource =
                com.eterocell.rhythhaus.theme.HausThemeColorSource.Curated,
            isSaving = false,
            saveFailed = false,
        )

    private fun libraryTrack(track: Track) =
        LibraryTrack(
            id = track.id,
            sourceId = "source",
            sourceLocalKey = "${track.id}.mp3",
            audioSource = track.source,
            displayName = "${track.title}.mp3",
            title = track.title,
            artist = track.artist,
            album = track.album,
            durationMillis = track.durationSeconds.toLong() * 1_000L,
            sizeBytes = null,
            modifiedAtEpochMillis = null,
            lastSeenScanId = "test-scan",
            createdAtEpochMillis = 0L,
            updatedAtEpochMillis = 0L,
        )

    private class CallbackRecorder {
        var cancelCalls = 0
    }

    private object TestNavigationOwner : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher = NavigationEventDispatcher()
    }

    private class TestMotionDurationScale(
        override val scaleFactor: Float,
    ) : MotionDurationScale

    private class CountingPicker : PlatformFolderPickerLauncher {
        override val isAvailable = true
        override val supportsAdditionalSources = true
        var launchCalls = 0

        override fun launch() {
            launchCalls++
        }
    }

    private object UnusedTagLibReader : TagLibReader {
        override fun readPath(path: String) =
            TagReadResult.Unsupported("unused")

        override fun readProperties(path: String) = emptyMap<String, String>()
    }

    private object EmptyPlaylistRepository : PlaylistRepository {
        override fun smartPlaylists() =
            emptyList<com.eterocell.rhythhaus.library.SmartPlaylistSummary>()

        override fun createSmartPlaylist(
            name: String,
            rule: com.eterocell.rhythhaus.library.SmartPlaylistRule,
        ): com.eterocell.rhythhaus.library.SmartPlaylistSummary =
            error("Not used by this test")

        override fun updateSmartPlaylist(
            id: String,
            name: String,
            rule: com.eterocell.rhythhaus.library.SmartPlaylistRule,
        ) = error("Not used by this test")

        override fun deleteSmartPlaylist(id: String) =
            error("Not used by this test")

        override fun playlists() = emptyList<PlaylistSummary>()

        override fun playlist(id: String) = null

        override fun entries(playlistId: String) = emptyList<PlaylistEntry>()

        override fun create(name: String): PlaylistSummary = error("unused")

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
