package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.*
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupImportResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsLabels
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupSettingsSection
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsPlaylistBackupEmbeddingTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsPlaylistBackupEmbeddingDoesNotPublishSearchSelection() =
        runComposeUiTest {
            val state =
                LibraryAppState(null).also {
                    it.pushRoute(LibraryRoute.Settings)
                }
            val backupState =
                androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
            val selectionActions = mutableListOf<TrackSelectionAction>()
            setContent {
                SettingsHarness(
                    state,
                    backupState,
                    onTrackSelectionAction = { selectionActions += it },
                )
            }
            waitForIdle()

            assertTrue(selectionActions.isEmpty())
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsPreviewCloseReopenAllocatesNewAppearance() = runComposeUiTest {
        val state =
            LibraryAppState(null).also { it.pushRoute(LibraryRoute.Settings) }
        val backupState =
            androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
        setContent { SettingsHarness(state, backupState) }
        backupState.value = backupState.value.copy(preview = preview())
        waitForIdle()
        val first = currentFeatureAppearance(state)
        backupState.value = backupState.value.copy(preview = null)
        waitForIdle()
        backupState.value = backupState.value.copy(preview = preview())
        waitForIdle()
        assertNotEquals(first, currentFeatureAppearance(state))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsResultCloseReopenAllocatesNewAppearance() = runComposeUiTest {
        val state =
            LibraryAppState(null).also { it.pushRoute(LibraryRoute.Settings) }
        val backupState =
            androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
        setContent { SettingsHarness(state, backupState) }
        backupState.value = backupState.value.copy(result = result())
        waitForIdle()
        val first = currentFeatureAppearance(state)
        backupState.value = backupState.value.copy(result = null)
        waitForIdle()
        backupState.value = backupState.value.copy(result = result())
        waitForIdle()
        assertNotEquals(first, currentFeatureAppearance(state))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsBackWaitsForAuthoritativePortRemoval() = runComposeUiTest {
        val state =
            LibraryAppState(null).also { it.pushRoute(LibraryRoute.Settings) }
        val backupState =
            androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
        setContent { SettingsHarness(state, backupState) }
        backupState.value = backupState.value.copy(preview = preview())
        waitForIdle()
        var callbackReturned = false
        assertEquals(
            LibraryBackAdapterResult.Handled,
            performLibraryBack(state, null) { callbackReturned = true })
        assertEquals(false, callbackReturned)
        assertNotNull(state.pendingBackSession)
        waitForIdle()
        assertEquals(null, state.pendingBackSession)
        assertEquals(null, backupState.value.preview)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsStaleDisposerCannotRemoveReplacement() = runComposeUiTest {
        val state =
            LibraryAppState(null).also { it.pushRoute(LibraryRoute.Settings) }
        val backupState =
            androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
        setContent { SettingsHarness(state, backupState) }
        backupState.value = backupState.value.copy(preview = preview())
        waitForIdle()
        val first = currentFeatureAppearance(state)
        backupState.value =
            backupState.value.copy(preview = null, result = result())
        waitForIdle()
        val replacement = currentFeatureAppearance(state)
        assertNotEquals(first, replacement)
        assertEquals(replacement, currentFeatureAppearance(state))
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistHubAndSettingsExposeOneSharedDocumentHostAtATime() =
        runComposeUiTest {
            val state =
                LibraryAppState(null).also {
                    it.pushRoute(LibraryRoute.PlaylistHub)
                }
            val route = mutableStateOf<LibraryRoute>(LibraryRoute.PlaylistHub)
            val backupState = mutableStateOf(PlaylistBackupUiState())
            var opens = 0
            setContent {
                SettingsHarness(
                    state,
                    backupState,
                    route = route.value,
                    onOpenPlaylistBackup = { opens++ },
                )
            }

            onAllNodes(hasTestTag("playlist-backup-json-import"))
                .assertCountEquals(1)
            onAllNodes(hasTestTag("playlist-backup-json-import"))[0]
                .performClick()
            assertEquals(1, opens)

            backupState.value = backupState.value.copy(preview = preview())
            waitForIdle()
            onAllNodes(hasTestTag("playlist-backup-preview-confirm"))
                .assertCountEquals(1)

            state.pushRoute(LibraryRoute.Settings)
            route.value = LibraryRoute.Settings
            waitForIdle()
            onAllNodes(hasTestTag("playlist-backup-preview-confirm"))
                .assertCountEquals(1)
            onAllNodes(hasTestTag("playlist-backup-preview-dismiss"))[0]
                .performClick()
            waitForIdle()
            assertEquals(null, backupState.value.preview)
            onAllNodes(hasTestTag("playlist-backup-preview-confirm"))
                .assertCountEquals(0)

            onNodeWithTag("settings-list")
                .performScrollToNode(hasTestTag("playlist-backup-json-import"))
            onAllNodes(hasTestTag("playlist-backup-json-import"))
                .assertCountEquals(1)
            onAllNodes(hasTestTag("playlist-backup-json-import"))[0]
                .performClick()
            assertEquals(2, opens)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun playlistHubBackupPreviewConsumesOutsideTapInsteadOfOpeningCreate() =
        runComposeUiTest {
            val oldLocale = Locale.getDefault()
            Locale.setDefault(Locale.ENGLISH)
            try {
                val state =
                    LibraryAppState(null).also {
                        it.pushRoute(LibraryRoute.PlaylistHub)
                    }
                val backupState =
                    mutableStateOf(
                        PlaylistBackupUiState().copy(preview = preview()))
                setContent {
                    SettingsHarness(
                        state,
                        backupState,
                        route = LibraryRoute.PlaylistHub,
                    )
                }

                onAllNodes(hasTestTag("playlist-backup-preview-confirm"))
                    .assertCountEquals(1)
                onAllNodes(hasText("Create playlist", substring = false))[0]
                    .performClick()

                waitForIdle()
                onAllNodes(hasTestTag("playlist-backup-preview-confirm"))
                    .assertCountEquals(0)
                onAllNodes(hasText("Choose a playlist type", substring = false))
                    .assertCountEquals(0)
            } finally {
                Locale.setDefault(oldLocale)
            }
        }

    @androidx.compose.runtime.Composable
    private fun SettingsHarness(
        state: LibraryAppState,
        backupState:
            androidx.compose.runtime.MutableState<PlaylistBackupUiState>,
        route: LibraryRoute = LibraryRoute.Settings,
        onOpenPlaylistBackup: () -> Unit = {},
        onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    ) {
        val source =
            rememberPlaylistFeatureAppearanceSource(
                PlaylistFeatureDestination(
                    state.activeDestinationId.instanceToken))
        val snapshot = LibrarySnapshot("Library", "", emptyList(), null)
        val playlistState = PlaylistState(hasConfirmedSnapshot = true)
        Box(Modifier.fillMaxSize()) {
            if (route == LibraryRoute.PlaylistHub) {
                LibraryRouteContent(
                    route = route,
                    tracks = emptyList(),
                    snapshot = snapshot,
                    libraryTracks = emptyList(),
                    playbackController =
                        PlaybackController(FakePlaybackEngine()),
                    playbackState = PlaybackState(),
                    playlistRepository = EmptyPlaylistRepository,
                    playlistState = playlistState,
                    onPlaylistStateAction = {},
                    onRefreshPlaylists = {},
                    onPlaylistMutation = { _, _ -> },
                    onRecoverStalePlaylistDetail = {},
                    selectedTrackId = null,
                    isNowPlayingBarVisible = false,
                    onBack = {},
                    destinationId = state.activeDestinationId,
                    playlistAppearanceSource = source,
                    registerBackSurface = state::registerBackSurface,
                    onOpenDetailRoute = {},
                    onTrackSelected = {},
                    onTrackClickFromTracks = { _, _ -> },
                    onExpandNowPlaying = {},
                    onScrollPositionChanged = {},
                    artworkLoader = { null },
                    homeContent = { _ -> },
                    favoriteTrackIds = emptySet(),
                    onSetTrackFavorite = { _, _ -> },
                    documentActions = {
                        PlaylistBackupSettingsSection(
                            state = backupState.value,
                            launcherAvailable = true,
                            labels =
                                PlaylistBackupSettingsLabels(
                                    cancel = "Cancel",
                                    close = "Close",
                                ),
                            onExport = {},
                            onOpen = onOpenPlaylistBackup,
                            onAction = {
                                backupState.value =
                                    reduceBackup(backupState.value, it)
                            },
                            staticPlaylists =
                                playlistState.confirmedSnapshot.playlists,
                        )
                    },
                    onTrackSelectionAction = onTrackSelectionAction,
                )
            }
            LibraryRouteScreen(
                route = route,
                snapshot = snapshot,
                libraryTracks = emptyList(),
                playbackController = PlaybackController(FakePlaybackEngine()),
                playbackState = PlaybackState(),
                playlistRepository = EmptyPlaylistRepository,
                playlistState = playlistState,
                playlistBackupState = backupState.value,
                backupDocumentAvailable = true,
                destinationId = state.activeDestinationId,
                playlistAppearanceSource = source,
                registerBackSurface = state::registerBackSurface,
                onPlaylistStateAction = {},
                onRefreshPlaylists = {},
                onPlaylistMutation = { _, _ -> },
                onExportPlaylists = {},
                onOpenPlaylistBackup = onOpenPlaylistBackup,
                onConfirmPlaylistBackup = {},
                onPlaylistBackupAction = {
                    backupState.value = reduceBackup(backupState.value, it)
                },
                sources = emptyList(),
                folderPickerLauncher = unavailablePicker,
                sourcePickerActionVisible = false,
                importMessage = null,
                scanProgress = null,
                latestCompletedScanSession = null,
                scanJob = null,
                currentThemeMode =
                    com.eterocell.rhythhaus.theme.RhythHausThemeMode.System,
                replayGainState =
                    com.eterocell.rhythhaus.playback.ReplayGainPreferenceState(
                        isBusy = false),
                onReplayGainModeSelected = {},
                crossfadeState =
                    com.eterocell.rhythhaus.playback.CrossfadePreferenceState(
                        isBusy = false),
                onCrossfadeDurationSelected = {},
                onThemeModeSelected = {},
                onClearLibrary = {},
                onRescanSource = {},
                onRemoveSource = {},
                onCancelScan = {},
                pushRoute = {},
                onShowSettingsAbout = {},
                onShowOpenSourceLibraries = {},
                onDismiss = {},
                onScrollPositionChanged = {},
                onTrackSelectionAction = onTrackSelectionAction,
            )
        }
    }

    private fun currentFeatureAppearance(state: LibraryAppState): String {
        val session =
            (state.beginBack() as LibraryBackBeginResult.Started).session
        val appearance = session.target.id.instanceToken
        session.reject()
        return appearance
    }

    private fun reduceBackup(
        state: PlaylistBackupUiState,
        action: PlaylistBackupUiAction
    ) =
        when (action) {
            is PlaylistBackupUiAction.PreviewReady ->
                state.copy(preview = action.preview, result = null)
            PlaylistBackupUiAction.DismissPreview -> state.copy(preview = null)
            PlaylistBackupUiAction.DismissResult -> state.copy(result = null)
            is PlaylistBackupUiAction.ImportSucceeded ->
                state.copy(preview = null, result = action.result)
            else -> state
        }

    private fun preview() =
        com.eterocell.rhythhaus.playlistbackup.PlaylistBackupPreview(
            0,
            emptyList(),
            emptyList(),
            com.eterocell.rhythhaus.playlistbackup.PlaylistBackupCounts(
                0, 0, 0),
            true)

    private fun result() =
        PlaylistBackupImportResult(
            1,
            0,
            com.eterocell.rhythhaus.playlistbackup.PlaylistBackupCounts(
                1, 0, 0))

    private val unavailablePicker =
        object : PlatformFolderPickerLauncher {
            override val isAvailable = false
            override val supportsAdditionalSources = false

            override fun launch() = Unit
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
