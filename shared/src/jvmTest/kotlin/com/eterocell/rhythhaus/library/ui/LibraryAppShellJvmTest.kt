package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.RepeatMode
import com.eterocell.rhythhaus.ShuffleMode
import com.eterocell.rhythhaus.SleepTimerMode
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.onboarding.OnboardingCloseTestTag
import com.eterocell.rhythhaus.onboarding.OnboardingRootTestTag
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.toPlayableTrack
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryAppShellJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun favoriteProjectionUpdatesHomeAndAlbumDetailsAtCompactAndWideWidths() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val track =
                    Track(
                        id = "favorite-track",
                        title = "Favorite track",
                        artist = "Favorite artist",
                        album = "Favorite album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/favorite.mp3"),
                    )
                listOf(420.dp, 1200.dp).forEach { width ->
                    var favoriteTrackIds by mutableStateOf(emptySet<String>())
                    val requests = mutableListOf<Pair<String, Boolean>>()
                    mount(
                        width = width,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "favorite-$width",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = listOf(track),
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        favoriteTrackIds = { favoriteTrackIds },
                        onSetTrackFavorite = { id, favorite ->
                            requests += id to favorite
                            favoriteTrackIds =
                                if (favorite) favoriteTrackIds + id
                                else favoriteTrackIds - id
                        },
                    )

                    onAllNodes(hasText("Songs"))[0].performClick()
                    waitForIdle()
                    onNode(
                            hasContentDescription(
                                "Add Favorite track to favorites") and
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.ToggleableState,
                                    ToggleableState.Off,
                                ),
                        )
                        .performClick()
                    waitForIdle()
                    assertEquals(
                        listOf("favorite-track" to true),
                        requests,
                    )

                    onAllNodes(hasText("Albums"))[0].performClick()
                    waitForIdle()
                    onNode(
                            hasContentDescription("Album Favorite album"),
                        )
                        .performClick()
                    waitForIdle()

                    onNode(
                            hasContentDescription(
                                "Remove Favorite track from favorites") and
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.ToggleableState,
                                    ToggleableState.On,
                                ),
                        )
                        .assertExists()

                    if (width == 1200.dp) {
                        onAllNodes(hasText("Songs"))[0].performClick()
                        waitForIdle()
                        onAllNodes(
                                hasContentDescription(
                                    "Remove Favorite track from favorites"),
                            )
                            .assertCountEquals(2)
                    }
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun flatHomeSelectionUsesEachModeVisibleOrderForPicker() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val tracks =
                    listOf(
                        track("hidden", "Hidden"),
                        track("favorite-first", "Favorite first"),
                        track("recent-latest", "Recent latest"),
                        track("favorite-last", "Favorite last"),
                    )
                val playlistActions = mutableListOf<PlaylistStateAction>()
                mount(
                    width = 1200.dp,
                    source = source(),
                    scanSession =
                        ScanSession(
                            id = "flat-selection",
                            sourceId = "source",
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    tracks = tracks,
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                    favoriteTrackIds = {
                        setOf("favorite-first", "favorite-last")
                    },
                    playHistory =
                        mapOf(
                            "recent-latest" to
                                TrackPlayHistory("recent-latest", 1L, 300L),
                            "favorite-last" to
                                TrackPlayHistory("favorite-last", 1L, 200L),
                        ),
                    createdAtByTrackId =
                        mapOf(
                            "hidden" to 10L,
                            "favorite-first" to 100L,
                            "recent-latest" to 300L,
                            "favorite-last" to 200L,
                        ),
                    onPlaylistStateAction = playlistActions::add,
                )

                fun selectAllAndOpenPicker(
                    mode: String,
                    selectedTitle: String,
                    expectedTrackIds: List<String>,
                ) {
                    onAllNodes(hasText(mode))[0].performClick()
                    waitForIdle()
                    onNode(
                            hasContentDescription(
                                "Select track $selectedTitle"),
                        )
                        .performSemanticsAction(SemanticsActions.OnLongClick)
                    waitForIdle()
                    onNode(hasContentDescription("Select all")).performClick()
                    waitForIdle()
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"),
                        )
                        .performClick()
                    waitForIdle()

                    assertEquals(
                        PlaylistStateAction.OpenPicker(
                            PlaylistPickerState(expectedTrackIds),
                        ),
                        playlistActions.last(),
                    )

                    onNode(hasContentDescription("Cancel selection"))
                        .performClick()
                    waitForIdle()
                }

                selectAllAndOpenPicker(
                    mode = "Favorites",
                    selectedTitle = "Favorite first",
                    expectedTrackIds =
                        listOf("favorite-first", "favorite-last"),
                )
                selectAllAndOpenPicker(
                    mode = "Recently played",
                    selectedTitle = "Recent latest",
                    expectedTrackIds = listOf("recent-latest", "favorite-last"),
                )
                selectAllAndOpenPicker(
                    mode = "Recently added",
                    selectedTitle = "Recent latest",
                    expectedTrackIds =
                        listOf(
                            "recent-latest",
                            "favorite-last",
                            "favorite-first",
                            "hidden",
                        ),
                )
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queryAndModeChangesReconcileSelectionAcrossAdaptiveHomeRoutesWithoutMutatingPlayback() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                fun track(id: String, title: String) =
                    Track(
                        id = id,
                        title = title,
                        artist = "Artist",
                        album = "Album",
                        durationSeconds = 180,
                        accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
                        source = AudioSource.FilePath("/$id.mp3"),
                    )

                val playing = track("playing", "Zed")
                val filtered = track("filtered", "Alpha")
                val retained = track("retained", "Bravo")
                val favoritePeer = track("favorite-peer", "Echo")
                val tracks = listOf(playing, filtered, retained, favoritePeer)

                data class PlaybackInvariant(
                    val currentTrackId: String?,
                    val queueTrackIds: List<String>,
                    val engineGeneration: Long,
                    val repeatMode: RepeatMode,
                    val shuffleMode: ShuffleMode,
                    val status: PlaybackStatus,
                    val positionMillis: Long,
                    val durationMillis: Long?,
                    val progressFraction: Float,
                )

                listOf(420.dp, 1200.dp).forEach { width ->
                    val engine = FakePlaybackEngine()
                    val controller = PlaybackController(engine)
                    controller.setQueue(
                        tracks.map { it.toPlayableTrack() },
                        selectedTrackId = playing.id,
                    )
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Paused
                    }
                    controller.setRepeatMode(RepeatMode.RepeatOne)
                    controller.setShuffleMode(ShuffleMode.On)
                    controller.play()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.status == PlaybackStatus.Playing
                    }
                    controller.seekTo(45_000L)
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.positionMillis == 45_000L
                    }

                    fun playbackInvariant() =
                        controller.state.value.let { state ->
                            PlaybackInvariant(
                                currentTrackId = state.currentTrack?.id,
                                queueTrackIds = state.queue.map { it.track.id },
                                engineGeneration =
                                    engine.activeGenerationForTest(),
                                repeatMode = state.repeatMode,
                                shuffleMode = state.shuffleMode,
                                status = state.status,
                                positionMillis = state.positionMillis,
                                durationMillis = state.durationMillis,
                                progressFraction = state.progressFraction,
                            )
                        }

                    val playbackBeforeQuery = playbackInvariant()
                    val playlistActions = mutableListOf<PlaylistStateAction>()
                    mount(
                        width = width,
                        source = source(),
                        scanSession =
                            ScanSession(
                                id = "query-selection-$width",
                                sourceId = "source",
                                status = ScanStatus.Completed,
                                startedAtEpochMillis = 1L,
                            ),
                        tracks = tracks,
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                        playbackController = controller,
                        favoriteTrackIds = {
                            setOf(retained.id, favoritePeer.id)
                        },
                        onPlaylistStateAction = playlistActions::add,
                    )
                    onNode(hasText("Songs", substring = false)).performClick()
                    waitForIdle()

                    onNode(hasContentDescription("Select track Alpha"))
                        .performSemanticsAction(SemanticsActions.OnLongClick)
                    waitForIdle()
                    onNode(
                            hasContentDescription("Select track Alpha") and
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.ToggleableState,
                                    ToggleableState.On,
                                ),
                        )
                        .assertIsDisplayed()
                    onNode(hasContentDescription("Select track Bravo"))
                        .performSemanticsAction(SemanticsActions.OnLongClick)
                    waitForIdle()
                    onNode(
                            hasContentDescription("Select track Bravo") and
                                SemanticsMatcher.expectValue(
                                    SemanticsProperties.ToggleableState,
                                    ToggleableState.On,
                                ),
                        )
                        .assertIsDisplayed()
                    onNode(hasContentDescription("Cancel selection"))
                        .assertIsDisplayed()

                    onNode(hasContentDescription("Descending")).performClick()
                    waitForIdle()
                    onNode(hasContentDescription("Cancel selection"))
                        .assertIsDisplayed()
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"))
                        .performClick()
                    waitForIdle()
                    assertEquals(
                        PlaylistStateAction.OpenPicker(
                            PlaylistPickerState(
                                listOf(retained.id, filtered.id)),
                        ),
                        playlistActions.last(),
                    )
                    assertEquals(playbackBeforeQuery, playbackInvariant())

                    onNode(hasContentDescription("Favorites only"))
                        .performClick()
                    waitForIdle()
                    onNode(hasContentDescription("Cancel selection"))
                        .assertIsDisplayed()
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"))
                        .performClick()
                    waitForIdle()
                    assertEquals(
                        PlaylistStateAction.OpenPicker(
                            PlaylistPickerState(listOf(retained.id)),
                        ),
                        playlistActions.last(),
                    )
                    assertEquals(playbackBeforeQuery, playbackInvariant())

                    onNode(hasText("Favorites", substring = false))
                        .performClick()
                    waitForIdle()
                    onNode(hasContentDescription("Cancel selection"))
                        .assertIsDisplayed()
                    onNode(
                            hasContentDescription(
                                "Add selected tracks to playlist"))
                        .performClick()
                    waitForIdle()
                    assertEquals(
                        PlaylistStateAction.OpenPicker(
                            PlaylistPickerState(listOf(retained.id)),
                        ),
                        playlistActions.last(),
                    )
                    assertEquals(playbackBeforeQuery, playbackInvariant())

                    onNode(hasContentDescription("Cancel selection"))
                        .performClick()
                    waitForIdle()
                    onNode(hasContentDescription("Select track Echo"))
                        .performClick()
                    waitUntil(timeoutMillis = 5_000) {
                        controller.state.value.currentTrack?.id ==
                            favoritePeer.id
                    }
                    assertEquals(
                        listOf(favoritePeer.id, retained.id),
                        controller.state.value.queue.map { it.track.id },
                    )
                    assertEquals(
                        favoritePeer.id,
                        controller.state.value.currentTrack?.id)
                    controller.release()
                }
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
                onNodeWithTag("NowPlayingBarSettings", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                onNode(hasText("Review onboarding"), useUnmergedTree = true)
                    .performClick()
                onNodeWithTag(OnboardingRootTestTag).assertIsDisplayed()

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
                    onNodeWithTag(
                            "NowPlayingBarSettings",
                            useUnmergedTree = true,
                        )
                        .performClick()
                    waitForIdle()
                    onNodeWithTag("settings-list", useUnmergedTree = true)
                        .performScrollToNode(hasText("Review onboarding"))
                    onNode(
                            hasText("Review onboarding"),
                            useUnmergedTree = true,
                        )
                        .performClick()

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
                    onAllNodesWithTag(
                            "NowPlayingBarSettings",
                            useUnmergedTree = true,
                        )
                        .assertCountEquals(0)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsReviewRestoresDestinationScrollState() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                mount(
                    width = 420.dp,
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
                )
                onNodeWithTag("NowPlayingBarSettings", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNode(hasText("View scan report"), useUnmergedTree = true)
                    .performClick()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                val reviewAction =
                    onNode(hasText("Review onboarding"), useUnmergedTree = true)
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
                onNodeWithTag("NowPlayingBarSettings", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
                onNode(hasText("View scan report"), useUnmergedTree = true)
                    .performClick()
                onNodeWithTag("settings-list", useUnmergedTree = true)
                    .performScrollToNode(hasText("Review onboarding"))
                onNode(hasText("Review onboarding"), useUnmergedTree = true)
                    .performClick()

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
    fun compactAndWideBranchesRenderEquivalentHomeImportAndScanStates() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                listOf(420.dp, 1200.dp).forEach { width ->
                    val source = source()
                    val picker = CountingPicker()
                    val callbacks = CallbackRecorder()

                    mount(
                        width = width,
                        source = source,
                        scanSession =
                            ScanSession(
                                id = "scanning-$width",
                                sourceId = source.id,
                                status = ScanStatus.Scanning,
                                startedAtEpochMillis = 1L,
                            ),
                        picker = picker,
                        callbacks = callbacks,
                    )
                    onNode(hasText("Cancel"), useUnmergedTree = true)
                        .performScrollTo()
                        .performClick()
                    assertEquals(1, callbacks.cancelCalls)
                    onNode(hasContentDescription("Add music folder"))
                        .performScrollTo()
                        .performClick()
                    assertEquals(1, picker.launchCalls)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactAndWideBranchesRenderEquivalentSettingsTerminalOutcome() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                listOf(420.dp, 1200.dp).forEach { width ->
                    val source = source()
                    val session =
                        ScanSession(
                            id = "completed-$width",
                            sourceId = source.id,
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                            foldersVisited = 2,
                            filesVisited = 4,
                            tracksAdded = 2,
                            tracksUpdated = 1,
                            filesSkipped = 1,
                        )
                    val errors =
                        listOf(
                            ScanError(
                                id = "error-$width",
                                scanId = session.id,
                                sourceLocalKey = "broken.mp3",
                                displayPath = "broken.mp3",
                                reason = "Unsupported file",
                                recoverable = true,
                                createdAtEpochMillis = 1L,
                            ),
                        )
                    mount(
                        width = width,
                        source = source,
                        scanSession = session,
                        scanErrors = errors,
                        picker = CountingPicker(),
                        callbacks = CallbackRecorder(),
                    )
                    onNodeWithTag(
                            "NowPlayingBarSettings", useUnmergedTree = true)
                        .performClick()
                    waitForIdle()

                    onNode(hasText("Scan complete"), useUnmergedTree = true)
                        .assertExists()
                    onNode(hasText("View scan report"), useUnmergedTree = true)
                        .performClick()
                    onNode(
                            hasText("broken.mp3: Unsupported file"),
                            useUnmergedTree = true)
                        .assertExists()
                    onNode(
                            hasText("Remove missing files"),
                            useUnmergedTree = true)
                        .assertExists()
                    onNode(hasText("Hide scan report"), useUnmergedTree = true)
                        .assertExists()
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactAndWideBranchesRenderPopulatedHomeWithoutScanOrSourceSurfaces() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                listOf(420.dp, 1200.dp).forEach { width ->
                    val source = source()
                    val populatedTracks =
                        listOf(
                            Track(
                                "populated-track",
                                "Populated track title",
                                "Artist",
                                "Test Album",
                                0,
                                TrackAccent(0, 0),
                                AudioSource.FilePath("/populated.mp3"),
                            ),
                        )
                    val activeSession =
                        ScanSession(
                            id = "active-$width",
                            sourceId = source.id,
                            status = ScanStatus.Scanning,
                            startedAtEpochMillis = 1L,
                        )
                    val terminalSession =
                        ScanSession(
                            id = "terminal-$width",
                            sourceId = source.id,
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                            foldersVisited = 2,
                            filesVisited = 4,
                            tracksAdded = 2,
                            tracksUpdated = 1,
                            filesSkipped = 1,
                        )
                    val errors =
                        listOf(
                            ScanError(
                                id = "error-$width",
                                scanId = terminalSession.id,
                                sourceLocalKey = "broken.mp3",
                                displayPath = "broken.mp3",
                                reason = "Unsupported file",
                                recoverable = true,
                                createdAtEpochMillis = 1L,
                            ),
                        )
                    val callbacks = CallbackRecorder()

                    // An active scan must not surface on a populated home.
                    mount(
                        width = width,
                        source = source,
                        scanSession = activeSession,
                        scanErrors = errors,
                        tracks = populatedTracks,
                        picker = CountingPicker(),
                        callbacks = callbacks,
                    )
                    onNode(hasText("Test Album"), useUnmergedTree = true)
                        .performScrollTo()
                        .assertExists()
                    onAllNodes(hasContentDescription("Add music folder"))
                        .assertCountEquals(0)
                    onAllNodes(hasText("Cancel"), useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodesWithTag(
                            "settings-rescan-source", useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodesWithTag(
                            "settings-remove-source", useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodes(hasText("Scan complete"), useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodes(
                            hasText("View scan report"), useUnmergedTree = true)
                        .assertCountEquals(0)

                    // A terminal outcome must not surface on a populated home.
                    mount(
                        width = width,
                        source = source,
                        scanSession = terminalSession,
                        scanErrors = errors,
                        tracks = populatedTracks,
                        picker = CountingPicker(),
                        callbacks = callbacks,
                    )
                    onNode(hasText("Test Album"), useUnmergedTree = true)
                        .performScrollTo()
                        .assertExists()
                    onAllNodes(hasText("Scan complete"), useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodes(
                            hasText("View scan report"), useUnmergedTree = true)
                        .assertCountEquals(0)
                    onAllNodes(
                            hasText("Remove missing files"),
                            useUnmergedTree = true)
                        .assertCountEquals(0)
                }
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun configuredZeroTrackSourceUsesDisplayNameInHomeFilters() =
        withDefaultLocale(Locale.ENGLISH) {
            runComposeUiTest {
                val source =
                    LibrarySource(
                        id = "opaque-source-id",
                        platformKind = LibraryPlatformKind.JvmFolder,
                        displayName = "Listening room",
                        handle = "/music",
                        createdAtEpochMillis = 1L,
                    )
                mount(
                    width = 420.dp,
                    source = source,
                    scanSession =
                        ScanSession(
                            id = "zero-track-source",
                            sourceId = source.id,
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 1L,
                        ),
                    picker = CountingPicker(),
                    callbacks = CallbackRecorder(),
                )

                onAllNodes(hasText("Songs"))[0].performClick()
                waitForIdle()

                onNode(hasContentDescription("Source Listening room"))
                    .assertIsDisplayed()
                onAllNodes(hasText("opaque-source-id", substring = false))
                    .assertCountEquals(0)
            }
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun expandedNowPlayingForwardsTimerActionsToTheActualPlaybackController() =
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

                onNodeWithTag(
                        "NowPlayingShellPlacement", useUnmergedTree = true)
                    .performClick()
                waitForIdle()
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
    private fun androidx.compose.ui.test.ComposeUiTest.mount(
        width: Dp,
        height: Dp = 900.dp,
        source: LibrarySource,
        scanSession: ScanSession,
        scanErrors: List<ScanError> = emptyList(),
        tracks: List<Track> = emptyList(),
        picker: CountingPicker,
        callbacks: CallbackRecorder,
        playbackController: PlaybackController =
            PlaybackController(FakePlaybackEngine()),
        favoriteTrackIds: () -> Set<String> = { emptySet() },
        playHistory: Map<String, TrackPlayHistory> = emptyMap(),
        createdAtByTrackId: Map<String, Long> = emptyMap(),
        onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
        onPlaylistStateAction: (PlaylistStateAction) -> Unit = {},
    ) {
        mount(
            width = { width },
            height = height,
            source = source,
            scanSession = scanSession,
            scanErrors = scanErrors,
            tracks = tracks,
            picker = picker,
            callbacks = callbacks,
            playbackController = playbackController,
            favoriteTrackIds = favoriteTrackIds,
            playHistory = playHistory,
            createdAtByTrackId = createdAtByTrackId,
            onSetTrackFavorite = onSetTrackFavorite,
            onPlaylistStateAction = onPlaylistStateAction,
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
        picker: CountingPicker,
        callbacks: CallbackRecorder,
        playbackController: PlaybackController =
            PlaybackController(FakePlaybackEngine()),
        favoriteTrackIds: () -> Set<String> = { emptySet() },
        playHistory: Map<String, TrackPlayHistory> = emptyMap(),
        createdAtByTrackId: Map<String, Long> = emptyMap(),
        onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
        onPlaylistStateAction: (PlaylistStateAction) -> Unit = {},
    ) {
        setContent {
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides
                    TestNavigationOwner,
            ) {
                Box(Modifier.size(width(), height)) {
                    LibraryHomeScreen(
                        snapshot =
                            LibrarySnapshot(
                                "Library",
                                "",
                                tracks,
                                playbackController.state.value.currentTrack?.id,
                            ),
                        libraryTracks = emptyList(),
                        tagLibReader = UnusedTagLibReader,
                        playbackController = playbackController,
                        playlistRepository = EmptyPlaylistRepository,
                        playlistState = PlaylistState(),
                        playlistBackupState = PlaylistBackupUiState(),
                        backupDocumentAvailable = false,
                        onPlaylistStateAction = onPlaylistStateAction,
                        onRefreshPlaylists = {},
                        onPlaylistMutation = { _, _ -> },
                        onExportPlaylists = {},
                        onOpenPlaylistBackup = {},
                        onConfirmPlaylistBackup = {},
                        onPlaylistBackupAction = {},
                        sources = listOf(source),
                        folderPickerLauncher = picker,
                        sourcePickerActionVisible = true,
                        importMessage = null,
                        scanProgress = ScanProgress(scanSession),
                        scanErrors = scanErrors,
                        scanJob = null,
                        coordinatorMutationsEnabled = true,
                        currentThemeMode = RhythHausThemeMode.System,
                        onThemeModeSelected = {},
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
                    )
                }
            }
        }
        waitForIdle()
    }

    private fun source() =
        LibrarySource(
            id = "source",
            platformKind = LibraryPlatformKind.JvmFolder,
            displayName = "Music",
            handle = "/music",
            createdAtEpochMillis = 1L,
        )

    private class CallbackRecorder {
        var cancelCalls = 0
    }

    private object TestNavigationOwner : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher = NavigationEventDispatcher()
    }

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
