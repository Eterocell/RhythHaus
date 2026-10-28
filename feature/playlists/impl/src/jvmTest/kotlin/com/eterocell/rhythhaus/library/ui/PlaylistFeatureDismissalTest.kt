package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.SmartPlaylistSummary
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.Res
import rhythhaus.feature.playlists.generated.resources.playlist_modal_mutation_failed

class PlaylistFeatureDismissalTest {
    private var previousLocale: Locale? = null

    @BeforeTest
    fun setEnglishLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @AfterTest
    fun restoreLocale() {
        previousLocale?.let { Locale.setDefault(it) }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hubCreatePresentationPublishesFromProduction() = runComposeUiTest {
        val publisher = RecordingPublisher()
        val destination = PlaylistFeatureDestination("hub-destination")
        var unrelated by mutableStateOf(0)
        var state by
            mutableStateOf(
                PlaylistState(
                    confirmedSnapshot = PlaylistSnapshot(),
                    hasConfirmedSnapshot = true,
                ),
            )
        setContent {
            val source = rememberPlaylistFeatureAppearanceSource(destination)
            PlaylistHubScreen(
                state = state,
                destination = destination,
                appearanceSource = source,
                dismissalPublisher = publisher,
                playlistsLabel = "Playlists",
                loadingLabel = "Loading",
                loadFailedLabel = "Failed",
                retryLabel = "Retry",
                mutationFailedLabel = "Could not save playlist changes",
                libraryTracks = emptyList(),
                onBack = {},
                onOpenPlaylist = {},
                onOpenSmartPlaylist = { error("Unexpected smart route") },
                onCreateSmartPlaylist = { _, _, _ ->
                    error("Unexpected smart creation")
                },
                onCreate = { _, _ -> },
                onRetry = {},
                documentActions = {},
            )
            unrelated
        }
        waitForIdle()
        publisher.assertNoActive()

        onAllNodes(hasText("Create playlist"))[0].performClick()
        waitForIdle()
        onAllNodes(hasText("Static playlist")).assertCountEquals(1)
        onAllNodes(hasText("Smart playlist")).assertCountEquals(1)
        val chooser = requireNotNull(publisher.current)
        assertEquals(1, publisher.activeCount)
        unrelated++
        waitForIdle()
        assertEquals(chooser, publisher.current)
        publisher.dispatchCurrent()
        waitForIdle()
        publisher.assertNoActive()
        onAllNodes(hasText("Create playlist"))[0].performClick()
        waitForIdle()
        onAllNodes(hasText("Static playlist"))[0].performClick()
        waitForIdle()
        val reopenedCreate = requireNotNull(publisher.current)
        assertEquals(1, publisher.activeCount)
        assertNotEquals(chooser.appearance, reopenedCreate.appearance)
        publisher.dispatchCurrent()
        waitForIdle()
        publisher.assertNoActive()

        assertTrue(
            publisher.history.none { it is PlaylistFeatureDismissal.Edit })
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hubNamesStaticAndSmartSectionsAndOpensTheirOwnDetails() =
        runComposeUiTest {
            val openedStatic = mutableListOf<String>()
            val openedSmart = mutableListOf<String>()
            val static = PlaylistSummary("static", "Static mix", 1, 1)
            val smart =
                SmartPlaylistSummary(
                    "smart",
                    "Favorites mix",
                    com.eterocell.rhythhaus.library.SmartPlaylistRule.Favorites,
                    1,
                    1,
                )
            val destination = PlaylistFeatureDestination("playlist-types")
            val publisher = RecordingPublisher()
            setContent {
                PlaylistHubScreen(
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(
                                    playlists = listOf(static),
                                    smartPlaylists = listOf(smart),
                                ),
                            hasConfirmedSnapshot = true,
                        ),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    playlistsLabel = "Playlists",
                    loadingLabel = "Loading",
                    loadFailedLabel = "Failed",
                    retryLabel = "Retry",
                    mutationFailedLabel = "Failed",
                    onBack = {},
                    onOpenPlaylist = { openedStatic += it },
                    onCreate = { _, _ -> },
                    libraryTracks = emptyList(),
                    onOpenSmartPlaylist = { openedSmart += it },
                    onCreateSmartPlaylist = { _, _, _ -> },
                    onRetry = {},
                    documentActions = {},
                )
            }

            onAllNodes(hasText("Static playlists")).assertCountEquals(1)
            onAllNodes(hasText("Smart playlists")).assertCountEquals(1)
            onAllNodes(hasText("Static mix"))[0].performClick()
            onAllNodes(hasText("Favorites mix"))[0].performClick()

            assertEquals(listOf("static"), openedStatic)
            assertEquals(listOf("smart"), openedSmart)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun queueClearConfirmationBackDismissalDoesNotClearUpcoming() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("queue-destination")
            val track = playableTrack()
            val playbackState =
                PlaybackState(
                    currentOccurrenceId = "current",
                    queue =
                        listOf(
                            QueueOccurrence("current", track),
                            QueueOccurrence("next", track),
                        ),
                )
            var clearCalls = 0
            setContent {
                PlaybackQueueContent(
                    playbackState = playbackState,
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    windowOverlayPublisher = null,
                    modifier = Modifier.fillMaxSize(),
                    onClose = {},
                    onSaveQueueAsPlaylist = { _, _, _ ->
                        error("Queue save is not part of this test")
                    },
                    onReorderUpcoming = { _, _ ->
                        QueueMutationFeedback(playbackState, false)
                    },
                    onRemoveUpcoming = {
                        QueueMutationFeedback(playbackState, false)
                    },
                    onClearUpcoming = {
                        clearCalls++
                        QueueMutationFeedback(playbackState, false)
                    },
                )
            }
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasContentDescription("Clear upcoming"))[0]
                .performClick()
            waitForIdle()
            publisher.requireOnlyStem("queue")
            assertEquals(0, clearCalls)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            assertEquals(0, clearCalls)
            onAllNodes(hasText(track.title)).assertCountEquals(2)

            onAllNodes(hasContentDescription("Clear upcoming"))[0]
                .performClick()
            waitForIdle()
            val confirmAction =
                onAllNodes(
                    hasText("Clear upcoming tracks") and hasClickAction())
            confirmAction.assertCountEquals(1)
            confirmAction[0].performClick()
            waitForIdle()
            assertEquals(1, clearCalls)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailRenameDeleteRemoveAndEditPresentationsPublishFromProduction() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("detail-destination")
            val playlist = PlaylistSummary("playlist", "Saved", 1, 1)
            var unrelated by mutableStateOf(0)
            setContent {
                val source =
                    rememberPlaylistFeatureAppearanceSource(destination)
                PlaylistDetailScreen(
                    playlist = playlist,
                    entries =
                        listOf(
                            PlaylistEntry("entry", "playlist", "track", 0, 1)),
                    playableTracksById =
                        mapOf(playableTrack().id to playableTrack()),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(playlists = listOf(playlist)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource = source,
                    dismissalPublisher = publisher,
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
                    initialEditMode = false,
                )
                unrelated
            }
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasContentDescription("Rename playlist"))[0]
                .performClick()
            waitForIdle()
            val rename = publisher.requireOnlyStem("rename")
            unrelated++
            waitForIdle()
            assertEquals(rename, publisher.current)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasContentDescription("Rename playlist"))[0]
                .performClick()
            waitForIdle()
            val reopenedRename = publisher.requireOnlyStem("rename")
            assertNotEquals(rename.appearance, reopenedRename.appearance)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasContentDescription("Delete playlist"))[0]
                .performClick()
            waitForIdle()
            val delete = publisher.requireOnlyStem("delete")
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasContentDescription("Delete playlist"))[0]
                .performClick()
            waitForIdle()
            val reopenedDelete = publisher.requireOnlyStem("delete")
            assertNotEquals(delete.appearance, reopenedDelete.appearance)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))[0]
                .performTouchInput { longClick() }
            waitForIdle()
            val edit = publisher.requireOnlyStem("edit")
            unrelated++
            waitForIdle()
            assertEquals(edit, publisher.current)
            onAllNodes(hasContentDescription("Remove Track from playlist"))[0]
                .performClick()
            waitForIdle()
            val remove = publisher.requireOnlyStem("remove")
            assertNotEquals(edit.appearance, remove.appearance)
            unrelated++
            waitForIdle()
            assertEquals(remove, publisher.current)
            publisher.dispatchCurrent()
            waitForIdle()
            assertEquals(edit, publisher.requireOnlyStem("edit"))
            onAllNodes(hasContentDescription("Remove Track from playlist"))[0]
                .performClick()
            waitForIdle()
            val reopenedRemove = publisher.requireOnlyStem("remove")
            assertNotEquals(remove.appearance, reopenedRemove.appearance)
            publisher.dispatchCurrent()
            waitForIdle()
            assertEquals(edit, publisher.requireOnlyStem("edit"))
            onAllNodes(hasContentDescription("Exit playlist editing"))[0]
                .performClick()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))[0]
                .performTouchInput { longClick() }
            waitForIdle()
            val reopenedEdit = publisher.requireOnlyStem("edit")
            assertNotEquals(
                edit.appearance,
                reopenedEdit.appearance,
                "first=${edit.appearance.value}, reopened=${reopenedEdit.appearance.value}",
            )
            unrelated++
            waitForIdle()
            assertEquals(reopenedEdit, publisher.current)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerAndBrowserOverlaysPublishFromProduction() = runComposeUiTest {
        val publisher = RecordingPublisher()
        val pickerDestination = PlaylistFeatureDestination("picker-destination")
        val browserDestination =
            PlaylistFeatureDestination("browser-destination")
        val equivalentPickerDestination =
            PlaylistFeatureDestination("equivalent-picker-destination")
        var pickerVisible by mutableStateOf(true)
        var browserVisible by mutableStateOf(false)
        var equivalentPickerVisible by mutableStateOf(false)
        var pickerState by
            mutableStateOf(
                PlaylistPickerState(
                    listOf("picker-track-one", "picker-track-two")))
        var browserState by
            mutableStateOf(PlaylistBrowserState("browser-playlist"))
        var pickerDismisses = 0
        var browserDismisses = 0
        var appendPayload: Pair<String, List<String>>? = null
        var inlinePayload: Pair<String, List<String>>? = null
        var browserPayload: Pair<String, List<String>>? = null
        var appendCompletion: ((PlaylistStateAction) -> Unit)? = null
        var inlineCompletion: ((PlaylistStateAction) -> Unit)? = null
        var browserCompletion: ((PlaylistStateAction) -> Unit)? = null
        var unrelated by mutableStateOf(0)
        var mutationFailureLabel: String? = null
        setContent {
            mutationFailureLabel =
                stringResource(Res.string.playlist_modal_mutation_failed)
            val pickerSource =
                rememberPlaylistFeatureAppearanceSource(pickerDestination)
            val browserSource =
                rememberPlaylistFeatureAppearanceSource(browserDestination)
            val equivalentPickerSource =
                rememberPlaylistFeatureAppearanceSource(
                    equivalentPickerDestination)
            if (pickerVisible)
                AddToPlaylistPickerOverlay(
                    playlists =
                        listOf(
                            PlaylistSummary(
                                "existing-playlist",
                                "Existing playlist",
                                0,
                                1)),
                    state = pickerState,
                    destination = pickerDestination,
                    appearanceSource = pickerSource,
                    dismissalPublisher = publisher,
                    onStateChange = { pickerState = it },
                    onDismiss = {
                        pickerDismisses++
                        pickerVisible = false
                    },
                    onConfirmed = {
                        pickerDismisses++
                        pickerVisible = false
                    },
                    onAppend = { id, trackIds, done ->
                        appendPayload = id to trackIds
                        appendCompletion = done
                    },
                    onInlineCreate = { name, trackIds, done ->
                        inlinePayload = name to trackIds
                        inlineCompletion = done
                    },
                )
            if (browserVisible)
                PlaylistTrackBrowserOverlay(
                    playlistName = "Browser destination",
                    libraryTracks =
                        listOf(track("browser-track", "Browser track")),
                    state = browserState,
                    destination = browserDestination,
                    appearanceSource = browserSource,
                    dismissalPublisher = publisher,
                    onStateChange = { browserState = it },
                    onDismiss = {
                        browserDismisses++
                        browserVisible = false
                    },
                    onConfirmed = {
                        browserDismisses++
                        browserVisible = false
                    },
                    onConfirm = { id, trackIds, done ->
                        browserPayload = id to trackIds
                        browserCompletion = done
                    },
                )
            if (equivalentPickerVisible)
                AddToPlaylistPickerOverlay(
                    playlists =
                        listOf(
                            PlaylistSummary(
                                "existing-playlist",
                                "Existing playlist",
                                0,
                                1)),
                    state =
                        PlaylistPickerState(
                            listOf("picker-track-one", "picker-track-two")),
                    destination = equivalentPickerDestination,
                    appearanceSource = equivalentPickerSource,
                    dismissalPublisher = publisher,
                    onStateChange = {},
                    onDismiss = { equivalentPickerVisible = false },
                    onConfirmed = { equivalentPickerVisible = false },
                    onAppend = { _, _, _ -> },
                    onInlineCreate = { _, _, _ -> },
                )
            unrelated
        }
        waitForIdle()

        val pickerAppend = publisher.requireOnlyStem("picker")
        unrelated++
        waitForIdle()
        assertEquals(pickerAppend, publisher.current)
        onAllNodes(hasContentDescription("Existing playlist"))[0].performClick()
        onAllNodes(hasText("Add to playlist"))[1].performClick()
        waitForIdle()
        assertEquals(
            "existing-playlist" to
                listOf("picker-track-one", "picker-track-two"),
            appendPayload)
        runOnIdle {
            requireNotNull(appendCompletion)(
                PlaylistStateAction.MutationFailed("failed", 1))
        }
        waitForIdle()
        onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
            .assertCountEquals(1)
        assertEquals(0, pickerDismisses)
        assertEquals(pickerAppend, publisher.requireOnlyStem("picker"))
        onAllNodes(hasText("Add to playlist"))[1].performClick()
        waitForIdle()
        runOnIdle {
            requireNotNull(appendCompletion)(
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
        }
        waitForIdle()
        publisher.assertNoActive()
        assertEquals(1, pickerDismisses)

        pickerState =
            PlaylistPickerState(listOf("picker-track-one", "picker-track-two"))
        pickerVisible = true
        waitForIdle()
        val pickerInline = publisher.requireOnlyStem("picker")
        assertNotEquals(pickerAppend.appearance, pickerInline.appearance)
        onAllNodes(hasText("Playlist name"))[0].performTextInput(
            " Inline playlist ")
        onAllNodes(hasText("Create playlist"))[0].performClick()
        waitForIdle()
        assertEquals(
            "Inline playlist" to listOf("picker-track-one", "picker-track-two"),
            inlinePayload)
        runOnIdle {
            requireNotNull(inlineCompletion)(
                PlaylistStateAction.MutationFailed("failed", 1))
        }
        waitForIdle()
        assertEquals(pickerInline, publisher.requireOnlyStem("picker"))
        onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
            .assertCountEquals(1)
        assertEquals(1, pickerDismisses)
        onAllNodes(hasText("Create playlist"))[0].performClick()
        waitForIdle()
        runOnIdle {
            requireNotNull(inlineCompletion)(
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
        }
        waitForIdle()
        publisher.assertNoActive()
        assertEquals(2, pickerDismisses)

        browserVisible = true
        waitForIdle()
        val browser = publisher.requireOnlyStem("browser")
        assertNotEquals(pickerInline.appearance, browser.appearance)
        unrelated++
        waitForIdle()
        assertEquals(browser, publisher.current)
        onAllNodes(hasText("Browser track"))[0].performClick()
        onAllNodes(hasText("Add selected tracks"))[0].performClick()
        waitForIdle()
        assertEquals(
            "browser-playlist" to listOf("browser-track"), browserPayload)
        runOnIdle {
            requireNotNull(browserCompletion)(
                PlaylistStateAction.MutationFailed("failed", 1))
        }
        waitForIdle()
        assertEquals(browser, publisher.requireOnlyStem("browser"))
        onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
            .assertCountEquals(1)
        assertEquals(0, browserDismisses)
        onAllNodes(hasText("Add selected tracks"))[0].performClick()
        waitForIdle()
        runOnIdle {
            requireNotNull(browserCompletion)(
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
        }
        waitForIdle()
        publisher.assertNoActive()
        assertEquals(1, browserDismisses)

        browserState = PlaylistBrowserState("browser-playlist")
        browserVisible = true
        waitForIdle()
        val replacementBrowser = publisher.requireOnlyStem("browser")
        assertNotEquals(browser.appearance, replacementBrowser.appearance)
        val staleRegistration = publisher.registrationFor(browser)
        equivalentPickerVisible = true
        waitForIdle()
        val equivalentPicker = publisher.requireStem("picker")
        assertEquals(2, publisher.activeCount)
        assertNotEquals(
            replacementBrowser.appearance, equivalentPicker.appearance)
        equivalentPickerVisible = false
        waitForIdle()
        publisher.requireActive(replacementBrowser)
        staleRegistration.dispose()
        publisher.requireActive(replacementBrowser)
        assertEquals(
            PlaylistFeatureDismissalDispatch.Rejected,
            staleRegistration.dispatch(browser))
        publisher.requireActive(replacementBrowser)
        assertEquals(1, browserDismisses)
        browserVisible = false
        waitForIdle()
        assertNull(publisher.current)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailEditCloseReopenAllocatesNewAppearanceWithSharedDestinationSource() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination =
                PlaylistFeatureDestination("precedence-destination")
            val playlist = PlaylistSummary("playlist", "Saved", 1, 1)
            var detailVisible by mutableStateOf(true)
            var unrelated by mutableStateOf(0)
            setContent {
                val source =
                    rememberPlaylistFeatureAppearanceSource(destination)
                if (detailVisible) {
                    PlaylistDetailScreen(
                        playlist = playlist,
                        entries =
                            listOf(
                                PlaylistEntry(
                                    "entry", "playlist", "track", 0, 1)),
                        playableTracksById =
                            mapOf(playableTrack().id to playableTrack()),
                        state =
                            PlaylistState(
                                confirmedSnapshot =
                                    PlaylistSnapshot(
                                        playlists = listOf(playlist)),
                                hasConfirmedSnapshot = true),
                        destination = destination,
                        appearanceSource = source,
                        dismissalPublisher = publisher,
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
                unrelated
            }
            waitForIdle()
            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))[0]
                .performTouchInput { longClick() }
            waitForIdle()
            val edit = publisher.requireOnlyStem("edit")
            unrelated++
            waitForIdle()
            assertEquals(edit, publisher.requireOnlyStem("edit"))
            onAllNodes(hasContentDescription("Remove Track from playlist"))[0]
                .performClick()
            waitForIdle()
            val remove = publisher.requireOnlyStem("remove")
            assertEquals(PlaylistFeatureDismissal.Modal::class, remove::class)
            assertEquals(1, publisher.activeCount)
            publisher.dispatchCurrent()
            waitForIdle()
            assertEquals(edit, publisher.requireOnlyStem("edit"))
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasContentDescription("Track, Artist, Album, 0:01"))[0]
                .performTouchInput { longClick() }
            waitForIdle()
            val reopenedEdit = publisher.requireOnlyStem("edit")
            assertNotEquals(
                edit.appearance,
                reopenedEdit.appearance,
                "first=${edit.appearance.value}, reopened=${reopenedEdit.appearance.value}",
            )
            unrelated++
            waitForIdle()
            assertEquals(reopenedEdit, publisher.requireOnlyStem("edit"))
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun hubCreatePendingBlocksDuplicateConfirmAndStaleCompletionCannotCloseReplacement() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("create-pending")
            val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
            var attempts = 0
            setContent {
                PlaylistHubScreen(
                    state =
                        PlaylistState(
                            confirmedSnapshot = PlaylistSnapshot(),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    playlistsLabel = "Playlists",
                    loadingLabel = "Loading",
                    loadFailedLabel = "Failed",
                    retryLabel = "Retry",
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onOpenPlaylist = {},
                    onCreate = { _, done ->
                        attempts++
                        completions += done
                    },
                    libraryTracks = emptyList(),
                    onOpenSmartPlaylist = { error("Unexpected smart route") },
                    onCreateSmartPlaylist = { _, _, _ ->
                        error("Unexpected smart creation")
                    },
                    onRetry = {},
                    documentActions = {},
                )
            }
            waitForIdle()

            onAllNodes(hasText("Create playlist"))[0].performClick()
            waitForIdle()
            onAllNodes(hasText("Static playlist"))[0].performClick()
            waitForIdle()
            onAllNodes(hasText("Playlist name"))[0].performTextInput("First")
            onNodeWithTag("playlist-name-confirm").performClick()
            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(1, attempts)
            onNodeWithTag("playlist-name-confirm").assertIsNotEnabled()
            onNodeWithTag("playlist-name-field").assertIsNotEnabled()

            onAllNodes(hasText("Cancel"))[0].performClick()
            waitForIdle()
            onAllNodes(hasText("Create playlist"))[0].performClick()
            waitForIdle()
            onAllNodes(hasText("Static playlist"))[0].performClick()
            waitForIdle()
            onAllNodes(hasText("Playlist name"))[0].performTextInput("Second")
            completions[0](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            onNodeWithTag("playlist-name-confirm").assertExists()
            onNodeWithTag("playlist-name-field").assertTextContains("Second")
            assertEquals(1, attempts)

            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(2, attempts)
            completions[1](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            assertTrue(
                onAllNodes(hasText("Could not save playlist changes"))
                    .fetchSemanticsNodes()
                    .isNotEmpty())
            onNodeWithTag("playlist-name-field").assertTextContains("Second")
            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(3, attempts)
            completions[2](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            onAllNodes(hasText("Cancel")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun renamePendingBlocksDuplicateConfirmAndStaleCompletionCannotCloseReplacement() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("rename-pending")
            val playlist = PlaylistSummary("playlist", "Saved", 1, 1)
            val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
            var attempts = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist,
                    entries = emptyList(),
                    playableTracksById = emptyMap(),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(playlists = listOf(playlist)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onRetry = {},
                    onRename = { _, done ->
                        attempts++
                        completions += done
                    },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Rename playlist"))[0]
                .performClick()
            waitForIdle()
            onNodeWithTag("playlist-name-field")
                .performTextReplacement("Renamed")
            onNodeWithTag("playlist-name-confirm").performClick()
            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(1, attempts)
            onNodeWithTag("playlist-name-confirm").assertIsNotEnabled()
            onNodeWithTag("playlist-name-field").assertIsNotEnabled()

            onAllNodes(hasText("Cancel"))[0].performClick()
            waitForIdle()
            onAllNodes(hasContentDescription("Rename playlist"))[0]
                .performClick()
            waitForIdle()
            completions[0](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            onNodeWithTag("playlist-name-confirm").assertExists()
            assertEquals(1, attempts)

            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(2, attempts)
            completions[1](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            onNodeWithTag("playlist-name-confirm").assertExists()
            onNodeWithTag("playlist-name-field").assertTextContains("Saved")
            onNodeWithTag("playlist-name-confirm").performClick()
            waitForIdle()
            assertEquals(3, attempts)
            completions[2](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            onAllNodes(hasText("Cancel")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun deletePendingBlocksDuplicateConfirmAndStaleCompletionCannotCloseReplacement() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("delete-pending")
            val playlist = PlaylistSummary("playlist", "Saved", 1, 1)
            val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
            val deleted = mutableListOf<PlaylistSnapshot>()
            var attempts = 0
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist,
                    entries = emptyList(),
                    playableTracksById = emptyMap(),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(playlists = listOf(playlist)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = { done ->
                        attempts++
                        completions += done
                    },
                    onDeleteConfirmed = { deleted += it },
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = {},
                    onReorder = {},
                )
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Delete playlist"))[0]
                .performClick()
            waitForIdle()
            onNodeWithTag("playlist-confirm").performClick()
            onNodeWithTag("playlist-confirm").performClick()
            waitForIdle()
            assertEquals(1, attempts)
            onNodeWithTag("playlist-confirm").assertIsNotEnabled()

            onAllNodes(hasText("Cancel"))[0].performClick()
            waitForIdle()
            onAllNodes(hasContentDescription("Delete playlist"))[0]
                .performClick()
            waitForIdle()
            completions[0](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            onNodeWithTag("playlist-confirm").assertExists()
            assertTrue(deleted.isEmpty())
            assertEquals(1, attempts)

            onNodeWithTag("playlist-confirm").performClick()
            waitForIdle()
            assertEquals(2, attempts)
            completions[1](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            onNodeWithTag("playlist-confirm").assertExists()
            onNodeWithTag("playlist-confirm").performClick()
            waitForIdle()
            assertEquals(3, attempts)
            completions[2](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            assertEquals(1, deleted.size)
            onAllNodes(hasText("Cancel")).assertCountEquals(0)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun detailRemoveConfirmationAdmitsOnlyOneOccurrenceRemoval() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("remove-admission")
            val playlist = PlaylistSummary("playlist", "Saved", 1, 1)
            val entry = PlaylistEntry("entry", "playlist", "track", 0, 1)
            val removed = mutableListOf<String>()
            setContent {
                PlaylistDetailScreen(
                    playlist = playlist,
                    entries = listOf(entry),
                    playableTracksById = mapOf("track" to playableTrack()),
                    state =
                        PlaylistState(
                            confirmedSnapshot =
                                PlaylistSnapshot(playlists = listOf(playlist)),
                            hasConfirmedSnapshot = true),
                    destination = destination,
                    appearanceSource =
                        rememberPlaylistFeatureAppearanceSource(destination),
                    dismissalPublisher = publisher,
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onRetry = {},
                    onRename = { _, _ -> },
                    onDelete = {},
                    onDeleteConfirmed = {},
                    onOpenBrowser = {},
                    onPlayEntry = {},
                    onRemoveEntry = removed::add,
                    onReorder = {},
                    initialEditMode = true,
                )
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Remove Track from playlist"))
                .assertCountEquals(1)[0]
                .performClick()
            waitForIdle()
            onNodeWithTag("playlist-confirm").performTouchInput {
                click()
                click()
            }
            waitForIdle()

            assertEquals(listOf("entry"), removed)
            publisher.requireOnlyStem("edit")
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun browserAppendPendingBlocksDuplicateConfirmAndStaleCompletionCannotClearReplacement() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val selectionCheckbox =
                SemanticsMatcher.expectValue(
                    SemanticsProperties.Role,
                    Role.Checkbox,
                )
            val destination = PlaylistFeatureDestination("browser-pending")
            var browserVisible by mutableStateOf(true)
            var browserState by
                mutableStateOf(PlaylistBrowserState("browser-playlist"))
            var dismisses = 0
            var confirmed = 0
            var attempts = 0
            var mutationFailureLabel: String? = null
            val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
            setContent {
                mutationFailureLabel =
                    stringResource(Res.string.playlist_modal_mutation_failed)
                val source =
                    rememberPlaylistFeatureAppearanceSource(destination)
                if (browserVisible)
                    PlaylistTrackBrowserOverlay(
                        playlistName = "Browser destination",
                        libraryTracks =
                            listOf(track("browser-track", "Browser track")),
                        state = browserState,
                        destination = destination,
                        appearanceSource = source,
                        dismissalPublisher = publisher,
                        onStateChange = { browserState = it },
                        onDismiss = {
                            dismisses++
                            browserVisible = false
                        },
                        onConfirmed = {
                            confirmed++
                            browserVisible = false
                        },
                        onConfirm = { _, _, done ->
                            attempts++
                            completions += done
                        },
                    )
            }
            waitForIdle()

            onNode(selectionCheckbox, useUnmergedTree = true)
                .assertIsEnabled()
                .assertHasClickAction()
            onAllNodes(hasText("Browser track"))[0].performClick()
            waitForIdle()
            assertEquals(setOf("browser-track"), browserState.selectedTrackIds)
            onNodeWithTag("playlist-browser-confirm").performClick()
            onNodeWithTag("playlist-browser-confirm").performClick()
            waitForIdle()
            assertEquals(1, attempts)
            val checkboxNode = onNode(selectionCheckbox, useUnmergedTree = true)
            checkboxNode.assertIsNotEnabled().assertHasNoClickAction()
            val checkboxSemantics = checkboxNode.fetchSemanticsNode().config
            assertEquals(
                null,
                checkboxSemantics.getOrNull(SemanticsProperties.Focused),
            )
            assertEquals(
                null,
                checkboxSemantics.getOrNull(SemanticsActions.RequestFocus),
            )
            onNodeWithTag("playlist-browser-confirm").assertIsNotEnabled()
            onNodeWithTag("playlist-browser-search").assertIsNotEnabled()

            completions[0](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            assertEquals(setOf("browser-track"), browserState.selectedTrackIds)
            assertEquals(
                1,
                onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                    .fetchSemanticsNodes()
                    .size)
            onNodeWithTag("playlist-browser-confirm").performClick()
            waitForIdle()
            assertEquals(2, attempts)
            val dismissed = publisher.requireOnlyStem("browser")
            publisher.dispatchCurrent()
            waitForIdle()
            assertEquals(1, dismisses)

            browserVisible = true
            waitForIdle()
            val replacement = publisher.requireOnlyStem("browser")
            assertNotEquals(dismissed.appearance, replacement.appearance)
            onNodeWithTag("playlist-browser-confirm").performClick()
            waitForIdle()
            assertEquals(3, attempts)
            onNodeWithTag("playlist-browser-confirm").assertIsNotEnabled()

            completions[1](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            assertTrue(browserVisible)
            assertEquals(0, confirmed)
            onNodeWithTag("playlist-browser-confirm").assertIsNotEnabled()
            onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                .assertCountEquals(0)

            completions[2](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            onNodeWithTag("playlist-browser-confirm").assertIsEnabled()
            assertEquals(
                1,
                onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                    .fetchSemanticsNodes()
                    .size)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun pickerAppendPendingBlocksDuplicateConfirmAndStaleCompletionCannotClearReplacement() =
        runComposeUiTest {
            val publisher = RecordingPublisher()
            val destination = PlaylistFeatureDestination("picker-pending")
            var pickerVisible by mutableStateOf(true)
            var pickerState by
                mutableStateOf(PlaylistPickerState(listOf("picker-track")))
            var dismisses = 0
            var confirmed = 0
            var attempts = 0
            var mutationFailureLabel: String? = null
            val completions = mutableListOf<(PlaylistStateAction) -> Unit>()
            setContent {
                mutationFailureLabel =
                    stringResource(Res.string.playlist_modal_mutation_failed)
                val source =
                    rememberPlaylistFeatureAppearanceSource(destination)
                if (pickerVisible)
                    AddToPlaylistPickerOverlay(
                        playlists =
                            listOf(
                                PlaylistSummary(
                                    "existing-playlist",
                                    "Existing playlist",
                                    0,
                                    1)),
                        state = pickerState,
                        destination = destination,
                        appearanceSource = source,
                        dismissalPublisher = publisher,
                        onStateChange = { pickerState = it },
                        onDismiss = {
                            dismisses++
                            pickerVisible = false
                        },
                        onConfirmed = {
                            confirmed++
                            pickerVisible = false
                        },
                        onAppend = { _, _, done ->
                            attempts++
                            completions += done
                        },
                        onInlineCreate = { _, _, _ ->
                            error("Unexpected inline creation")
                        },
                    )
            }
            waitForIdle()

            onAllNodes(hasContentDescription("Existing playlist"))[0]
                .performClick()
            waitForIdle()
            onNodeWithTag("playlist-picker-append").performClick()
            onNodeWithTag("playlist-picker-append").performClick()
            waitForIdle()
            assertEquals(1, attempts)
            onNodeWithTag("playlist-picker-append").assertIsNotEnabled()
            onNodeWithTag("playlist-picker-name-field").assertIsNotEnabled()

            completions[0](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            assertEquals("existing-playlist", pickerState.selectedPlaylistId)
            assertEquals(
                1,
                onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                    .fetchSemanticsNodes()
                    .size)
            onNodeWithTag("playlist-picker-append").performClick()
            waitForIdle()
            assertEquals(2, attempts)
            publisher.dispatchCurrent()
            waitForIdle()
            assertEquals(1, dismisses)

            pickerVisible = true
            waitForIdle()
            onNodeWithTag("playlist-picker-append").performClick()
            waitForIdle()
            assertEquals(3, attempts)
            onNodeWithTag("playlist-picker-append").assertIsNotEnabled()

            completions[1](
                PlaylistStateAction.SnapshotConfirmed(PlaylistSnapshot()))
            waitForIdle()
            assertTrue(pickerVisible)
            assertEquals(0, confirmed)
            onNodeWithTag("playlist-picker-append").assertIsNotEnabled()
            onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                .assertCountEquals(0)

            completions[2](PlaylistStateAction.MutationFailed("failed"))
            waitForIdle()
            assertEquals(
                1,
                onAllNodes(hasText(requireNotNull(mutationFailureLabel)))
                    .fetchSemanticsNodes()
                    .size)
        }

    private fun track(id: String = "track", title: String = "Track") =
        LibraryTrack(
            id = id,
            sourceId = "source",
            sourceLocalKey = id,
            audioSource = AudioSource.FilePath("/$id"),
            displayName = title,
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000,
            sizeBytes = 1,
            modifiedAtEpochMillis = 1,
            lastSeenScanId = "scan",
            createdAtEpochMillis = 1,
            updatedAtEpochMillis = 1,
        )

    private fun playableTrack(id: String = "track", title: String = "Track") =
        PlayableTrack(
            id = id,
            title = title,
            artist = "Artist",
            album = "Album",
            durationMillis = 1_000,
            source = AudioSource.FilePath("/$id"),
        )

    private class RecordingPublisher : PlaylistFeatureDismissalPublisher {
        var current: PlaylistFeatureDismissal? = null
        val history = mutableListOf<PlaylistFeatureDismissal>()
        val disposers = mutableListOf<() -> Unit>()
        private val active = mutableMapOf<Int, PlaylistFeatureDismissal>()
        private val registrations = mutableMapOf<Int, Registration>()
        private var nextRegistration = 0
        private var currentRegistration: Int? = null

        class Registration
        internal constructor(
            val dismissal: PlaylistFeatureDismissal?,
            private val callback:
                (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch,
            private val onDispose: () -> Unit,
        ) {
            fun dispatch(
                target: PlaylistFeatureDismissal
            ): PlaylistFeatureDismissalDispatch = callback(target)

            fun dispose() = onDispose()
        }

        override fun publish(
            dismissal: PlaylistFeatureDismissal?,
            dispatch:
                (PlaylistFeatureDismissal) -> PlaylistFeatureDismissalDispatch
        ): () -> Unit {
            val registration = ++nextRegistration
            current = dismissal
            currentRegistration = registration
            dismissal?.let(history::add)
            dismissal?.let { active[registration] = it }
            var disposed = false
            val recorded =
                Registration(dismissal, dispatch) {
                    if (!disposed) {
                        disposed = true
                        active.remove(registration)
                        if (currentRegistration == registration) {
                            currentRegistration = null
                            current = null
                        }
                    }
                }
            registrations[registration] = recorded
            return recorded::dispose.also(disposers::add)
        }

        fun requireStem(stem: String): PlaylistFeatureDismissal =
            requireNotNull(current).also {
                assertEquals(stem, it.appearance.value.substringBefore('-'))
            }

        fun requireOnlyStem(stem: String): PlaylistFeatureDismissal =
            requireStem(stem).also { assertEquals(1, active.size) }

        fun assertNoActive() {
            assertNull(current)
            assertEquals(0, active.size)
        }

        fun requireActive(dismissal: PlaylistFeatureDismissal) {
            assertEquals(1, active.values.count { it == dismissal })
        }

        val activeCount: Int
            get() = active.size

        fun dispatchCurrent() = dispatch(requireNotNull(current))

        fun dispatch(target: PlaylistFeatureDismissal) {
            registrations.values
                .lastOrNull { it.dismissal == target }
                ?.dispatch(target)
        }

        fun registrationFor(target: PlaylistFeatureDismissal): Registration =
            requireNotNull(
                registrations.values.lastOrNull { it.dismissal == target })
    }
}

// Library extraction
