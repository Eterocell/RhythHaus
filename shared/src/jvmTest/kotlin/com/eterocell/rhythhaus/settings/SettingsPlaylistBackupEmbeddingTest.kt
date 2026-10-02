package com.eterocell.rhythhaus.settings

import androidx.compose.ui.test.ExperimentalTestApi
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
            pushRoute = {},
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
