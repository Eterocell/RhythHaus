package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.TrackAccent
import com.eterocell.rhythhaus.library.LibraryPlatformKind
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.PlaylistEntry
import com.eterocell.rhythhaus.library.PlaylistImportMutation
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class LibraryAppShellJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun compactAndWideBranchesForwardEquivalentLibraryManagerCallbacks() =
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

                    val completedSession =
                        ScanSession(
                            id = "completed-$width",
                            sourceId = source.id,
                            status = ScanStatus.Completed,
                            startedAtEpochMillis = 2L,
                        )
                    mount(
                        width = width,
                        source = source,
                        scanSession = completedSession,
                        picker = picker,
                        callbacks = callbacks,
                    )
                    onNode(hasText("Add music folder"), useUnmergedTree = true)
                        .performScrollTo()
                        .performClick()
                    onAllNodes(hasText("Rescan"), useUnmergedTree = true)[1]
                        .performScrollTo()
                        .performClick()
                    onNode(
                            hasText("Remove missing files"),
                            useUnmergedTree = true)
                        .performScrollTo()
                        .performClick()
                    onNode(hasText("Remove folder"), useUnmergedTree = true)
                        .performScrollTo()
                        .performClick()

                    assertEquals(1, picker.launchCalls)
                    assertEquals(1, callbacks.rescannedSources.size)
                    assertSame(source, callbacks.rescannedSources.single())
                    assertEquals(1, callbacks.removeMissingCalls.size)
                    assertSame(
                        source, callbacks.removeMissingCalls.single().first)
                    assertSame(
                        completedSession,
                        callbacks.removeMissingCalls.single().second)
                    assertEquals(1, callbacks.removedSources.size)
                    assertSame(source, callbacks.removedSources.single())
                }
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
        source: LibrarySource,
        scanSession: ScanSession,
        picker: CountingPicker,
        callbacks: CallbackRecorder,
    ) {
        setContent {
            CompositionLocalProvider(
                LocalNavigationEventDispatcherOwner provides
                    TestNavigationOwner,
            ) {
                Box(Modifier.size(width, 900.dp)) {
                    LibraryHomeScreen(
                        snapshot =
                            LibrarySnapshot(
                                "Library", "", listOf(track()), null),
                        libraryTracks = emptyList(),
                        tagLibReader = UnusedTagLibReader,
                        playbackController =
                            PlaybackController(FakePlaybackEngine()),
                        playlistRepository = EmptyPlaylistRepository,
                        playlistState = PlaylistState(),
                        playlistBackupState = PlaylistBackupUiState(),
                        backupDocumentAvailable = false,
                        onPlaylistStateAction = {},
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
                        scanErrors = emptyList(),
                        scanJob = null,
                        coordinatorMutationsEnabled = true,
                        currentThemeMode = RhythHausThemeMode.System,
                        onThemeModeSelected = {},
                        onClearLibrary = {},
                        onRescanSource = callbacks.rescannedSources::add,
                        onRemoveSource = callbacks.removedSources::add,
                        onRemoveMissingTracks = {
                            selectedSource,
                            selectedSession ->
                            callbacks.removeMissingCalls +=
                                selectedSource to selectedSession
                        },
                        onCancelScan = { callbacks.cancelCalls++ },
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

    private fun track() =
        Track(
            id = "track",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 120,
            accent = TrackAccent(0xFF000000, 0xFFFFFFFF),
            source = AudioSource.FilePath("/music/track.mp3"),
        )

    private class CallbackRecorder {
        var cancelCalls = 0
        val rescannedSources = mutableListOf<LibrarySource>()
        val removedSources = mutableListOf<LibrarySource>()
        val removeMissingCalls =
            mutableListOf<Pair<LibrarySource, ScanSession>>()
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
