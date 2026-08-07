package com.eterocell.rhythhaus.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
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
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
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
    fun settingsHostEmbedsSectionPreviewAndResultWithCurrentCallbacks() =
        runComposeUiTest {
            val state =
                LibraryAppState(null).also {
                    it.pushRoute(LibraryRoute.Settings)
                }
            val backupState =
                androidx.compose.runtime.mutableStateOf(PlaylistBackupUiState())
            val actions = mutableListOf<PlaylistBackupUiAction>()
            var exportCalls = 0
            var openCalls = 0
            var confirmCalls = 0
            val publishBackupAction: (PlaylistBackupUiAction) -> Unit = {
                actions += it
                backupState.value = reduceBackup(backupState.value, it)
            }

            setContent {
                val source =
                    rememberPlaylistFeatureAppearanceSource(
                        PlaylistFeatureDestination(
                            state.activeDestinationId.instanceToken),
                    )
                LibraryRouteOverlays(
                    route = LibraryRoute.Settings,
                    snapshot =
                        LibrarySnapshot("Library", "", emptyList(), null),
                    libraryTracks = emptyList(),
                    playbackController =
                        PlaybackController(FakePlaybackEngine()),
                    playbackState = PlaybackState(),
                    playlistRepository = EmptyPlaylistRepository,
                    playlistState = PlaylistState(),
                    playlistBackupState = backupState.value,
                    backupDocumentAvailable = true,
                    destinationId = state.activeDestinationId,
                    playlistAppearanceSource = source,
                    registerBackSurface = state::registerBackSurface,
                    onPlaylistStateAction = {},
                    onRefreshPlaylists = {},
                    onPlaylistMutation = { _, _ -> },
                    onExportPlaylists = { exportCalls++ },
                    onOpenPlaylistBackup = { openCalls++ },
                    onConfirmPlaylistBackup = { confirmCalls++ },
                    onPlaylistBackupAction = publishBackupAction,
                    sources = emptyList(),
                    folderPickerLauncher = unavailablePicker,
                    sourcePickerActionVisible = false,
                    importMessage = null,
                    scanProgress = null,
                    scanJob = null,
                    currentThemeMode =
                        com.eterocell.rhythhaus.theme.RhythHausThemeMode.System,
                    onThemeModeSelected = {},
                    onClearLibrary = {},
                    onRescanSource = {},
                    onRemoveSource = {},
                    onCancelScan = {},
                    onShowSettingsAbout = {},
                    onShowOpenSourceLibraries = {},
                    onDismiss = {},
                    onScrollPositionChanged = {},
                )
            }
            waitForIdle()
            onNode(hasScrollToIndexAction()).performScrollToIndex(3)
            waitForIdle()
            onNode(hasText("导出播放列表")).performClick()
            onNode(hasScrollToIndexAction()).performScrollToIndex(3)
            waitForIdle()
            onNode(hasText("导入播放列表")).performClick()
            assertEquals(1, exportCalls)
            assertEquals(1, openCalls)

            publishBackupAction(PlaylistBackupUiAction.PreviewReady(preview()))
            waitForIdle()
            val previewAppearance = currentFeatureAppearance(state)
            onNodeWithTag(
                    "playlist-backup-preview-dismiss", useUnmergedTree = true)
                .performClick()
            waitForIdle()
            assertEquals(2, actions.size)
            assertTrue(actions.contains(PlaylistBackupUiAction.DismissPreview))
            assertEquals(null, backupState.value.preview)

            publishBackupAction(PlaylistBackupUiAction.PreviewReady(preview()))
            waitForIdle()
            onNodeWithTag(
                    "playlist-backup-preview-confirm", useUnmergedTree = true)
                .performClick()
            assertEquals(1, confirmCalls)
            assertEquals(3, actions.size)
            assertTrue(actions.contains(PlaylistBackupUiAction.DismissPreview))

            publishBackupAction(
                PlaylistBackupUiAction.ImportSucceeded(result()))
            waitForIdle()
            assertNotEquals(previewAppearance, currentFeatureAppearance(state))
            onNodeWithTag(
                    "playlist-backup-result-dismiss", useUnmergedTree = true)
                .performClick()
            waitForIdle()
            assertTrue(actions.contains(PlaylistBackupUiAction.DismissResult))
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

    @androidx.compose.runtime.Composable
    private fun SettingsHarness(
        state: LibraryAppState,
        backupState:
            androidx.compose.runtime.MutableState<PlaylistBackupUiState>,
        onTrackSelectionAction: (TrackSelectionAction) -> Unit = {},
    ) {
        val source =
            rememberPlaylistFeatureAppearanceSource(
                PlaylistFeatureDestination(
                    state.activeDestinationId.instanceToken))
        LibraryRouteOverlays(
            route = LibraryRoute.Settings,
            snapshot = LibrarySnapshot("Library", "", emptyList(), null),
            libraryTracks = emptyList(),
            playbackController = PlaybackController(FakePlaybackEngine()),
            playbackState = PlaybackState(),
            playlistRepository = EmptyPlaylistRepository,
            playlistState = PlaylistState(),
            playlistBackupState = backupState.value,
            backupDocumentAvailable = true,
            destinationId = state.activeDestinationId,
            playlistAppearanceSource = source,
            registerBackSurface = state::registerBackSurface,
            onPlaylistStateAction = {},
            onRefreshPlaylists = {},
            onPlaylistMutation = { _, _ -> },
            onExportPlaylists = {},
            onOpenPlaylistBackup = {},
            onConfirmPlaylistBackup = {},
            onPlaylistBackupAction = {
                backupState.value = reduceBackup(backupState.value, it)
            },
            sources = emptyList(),
            folderPickerLauncher = unavailablePicker,
            sourcePickerActionVisible = false,
            importMessage = null,
            scanProgress = null,
            scanJob = null,
            currentThemeMode =
                com.eterocell.rhythhaus.theme.RhythHausThemeMode.System,
            onThemeModeSelected = {},
            onClearLibrary = {},
            onRescanSource = {},
            onRemoveSource = {},
            onCancelScan = {},
            onShowSettingsAbout = {},
            onShowOpenSourceLibraries = {},
            onDismiss = {},
            onScrollPositionChanged = {},
            onTrackSelectionAction = onTrackSelectionAction,
        )
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
