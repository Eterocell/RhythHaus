package com.eterocell.rhythhaus.library.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
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
    fun hubCreateAndQueueClearPresentationsPublishFromProduction() =
        runComposeUiTest {
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
                    queue =
                        listOf(
                            QueueOccurrence("current", queueTrack),
                            QueueOccurrence("upcoming-one", queueTrack),
                            QueueOccurrence("upcoming-two", queueTrack),
                        ),
                )
            setContent {
                val source =
                    rememberPlaylistFeatureAppearanceSource(destination)
                PlaylistHubScreen(
                    state = state,
                    playbackState = playbackState,
                    destination = destination,
                    appearanceSource = source,
                    dismissalPublisher = publisher,
                    playlistsLabel = "Playlists",
                    loadingLabel = "Loading",
                    loadFailedLabel = "Failed",
                    retryLabel = "Retry",
                    mutationFailedLabel = "Could not save playlist changes",
                    onBack = {},
                    onOpenPlaylist = {},
                    onSelectTab = { state = state.copy(selectedTab = it) },
                    onCreate = { _, _ -> },
                    onRetry = {},
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
                unrelated
            }
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasText("Create playlist"))[0].performClick()
            waitForIdle()
            val create = publisher.requireOnlyStem("create")
            unrelated++
            waitForIdle()
            assertEquals(create, publisher.current)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasText("Create playlist"))[0].performClick()
            waitForIdle()
            val reopenedCreate = publisher.requireOnlyStem("create")
            assertNotEquals(create.appearance, reopenedCreate.appearance)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()

            onAllNodes(hasText("Queue"))[0].performClick()
            waitForIdle()
            onAllNodes(hasContentDescription("Clear upcoming"))[0]
                .performClick()
            waitForIdle()
            val queue = publisher.requireOnlyStem("queue")
            unrelated++
            waitForIdle()
            assertEquals(queue, publisher.current)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            onAllNodes(hasContentDescription("Clear upcoming"))[0]
                .performClick()
            waitForIdle()
            val reopenedQueue = publisher.requireOnlyStem("queue")
            assertNotEquals(queue.appearance, reopenedQueue.appearance)
            publisher.dispatchCurrent()
            waitForIdle()
            publisher.assertNoActive()
            assertTrue(
                publisher.history.none { it is PlaylistFeatureDismissal.Edit })
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
                    onAppend = { id, trackIds, done ->
                        appendPayload = id to trackIds
                        appendCompletion = { outcome ->
                            done(outcome)
                            if (outcome
                                is PlaylistStateAction.SnapshotConfirmed) {
                                pickerDismisses++
                                pickerVisible = false
                            }
                        }
                    },
                    onInlineCreate = { name, trackIds, done ->
                        inlinePayload = name to trackIds
                        inlineCompletion = { outcome ->
                            done(outcome)
                            if (outcome
                                is PlaylistStateAction.SnapshotConfirmed) {
                                pickerDismisses++
                                pickerVisible = false
                            }
                        }
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
                    onConfirm = { id, trackIds, done ->
                        browserPayload = id to trackIds
                        browserCompletion = { outcome ->
                            done(outcome)
                            if (outcome
                                is PlaylistStateAction.SnapshotConfirmed) {
                                browserDismisses++
                                browserVisible = false
                            }
                        }
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
        onAllNodes(hasContentDescription("Browser track"))[0].performClick()
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
