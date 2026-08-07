package com.eterocell.rhythhaus.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.FakePlaybackEngine
import com.eterocell.rhythhaus.LibrarySnapshot
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
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
import com.eterocell.rhythhaus.library.ui.LibraryAppState
import com.eterocell.rhythhaus.library.ui.LibraryBackBeginResult
import com.eterocell.rhythhaus.library.ui.LibraryBackTarget
import com.eterocell.rhythhaus.library.ui.LibraryRoute
import com.eterocell.rhythhaus.library.ui.LibraryRouteOverlays
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDestination
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.rememberPlaylistFeatureAppearanceSource
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupCounts
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupImportResult
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupPlaylistReport
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupPreview
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiAction
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import rhythhaus.core.ui.generated.resources.Res as CoreUiRes
import rhythhaus.core.ui.generated.resources.back as coreBack
import rhythhaus.shared.generated.resources.Res as SharedRes
import rhythhaus.shared.generated.resources.add_music_folder as sharedAddMusicFolder
import rhythhaus.shared.generated.resources.cancel as sharedCancel
import rhythhaus.shared.generated.resources.clear as sharedClear
import rhythhaus.shared.generated.resources.clear_library as sharedClearLibrary
import rhythhaus.shared.generated.resources.folder_picker_unavailable as sharedFolderPickerUnavailable
import rhythhaus.shared.generated.resources.remove as sharedRemove
import rhythhaus.shared.generated.resources.settings as sharedSettings

class SettingsRouteAdapterJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun projectsSourcesAndSuppliesPickerScanningPlaylistAndClearSlots() =
        runComposeUiTest {
            fun clickableTextLabels(): List<String> =
                onAllNodes(
                        SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick),
                        useUnmergedTree = false,
                    )
                    .fetchSemanticsNodes()
                    .mapNotNull { node ->
                        node.config
                            .getOrNull(SemanticsProperties.Text)
                            ?.joinToString(separator = "") { it.text }
                            ?.takeIf(String::isNotBlank)
                    }

            val authoritativeSource =
                source(
                    id = "private-source-id-alpha",
                    name = "Rendered source marker",
                    handle = "/private/path/alpha",
                    scanned = true,
                )
            val picker = CountingPicker()
            val rescans = mutableListOf<LibrarySource>()
            var cancelCalls = 0
            var clearCalls = 0
            val backupState = mutableStateOf(PlaylistBackupUiState())
            val backupActions = mutableListOf<PlaylistBackupUiAction>()
            val scanProgress = mutableStateOf<ScanProgress?>(null)
            setContent {
                Harness(
                    sources = listOf(authoritativeSource),
                    hasTracks = true,
                    folderPickerLauncher = picker,
                    scanProgress = scanProgress.value,
                    playlistBackupState = backupState.value,
                    onPlaylistBackupAction = { backupActions += it },
                    onClear = { clearCalls++ },
                    onRescan = { rescans += it },
                    onCancelScan = { cancelCalls++ },
                )
            }
            onNodeWithTag("settings-root", useUnmergedTree = true)
                .assertExists()
            onNode(
                    hasText(authoritativeSource.displayName),
                    useUnmergedTree = true)
                .performScrollTo()
                .assertExists()
            onNode(hasText(authoritativeSource.id), useUnmergedTree = true)
                .assertDoesNotExist()
            onNode(hasText(authoritativeSource.handle), useUnmergedTree = true)
                .assertDoesNotExist()
            onNodeWithTag(
                    "settings-rescan-${authoritativeSource.id}",
                    useUnmergedTree = true)
                .assertExists()
                .performClick()
            onNodeWithTag(
                    "settings-remove-${authoritativeSource.id}",
                    useUnmergedTree = true)
                .assertExists()
            assertEquals(1, rescans.size)
            assertSame(authoritativeSource, rescans.single())

            onNodeWithTag("settings-picker", useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            assertEquals(1, picker.launchCalls)

            val labelsBeforeScan = clickableTextLabels()
            val latestItem = "latest-scan-item-marker"
            scanProgress.value =
                ScanProgress(
                    session =
                        ScanSession(
                            "scan",
                            authoritativeSource.id,
                            ScanStatus.Scanning,
                            1L),
                    latestItem = latestItem,
                )
            waitForIdle()
            onNode(hasText(latestItem), useUnmergedTree = true)
                .performScrollTo()
                .assertExists()
            val cancelLabels = clickableTextLabels() - labelsBeforeScan.toSet()
            assertEquals(1, cancelLabels.size)
            onNode(hasText(cancelLabels.single()), useUnmergedTree = true)
                .performClick()
            assertEquals(1, cancelCalls)

            scanProgress.value = null
            backupState.value =
                PlaylistBackupUiState(
                    preview = backupPreview("backup-preview-marker"))
            waitForIdle()
            onNodeWithTag(
                    "playlist-backup-preview-list", useUnmergedTree = true)
                .performScrollToIndex(2)
            assertTrue(
                onAllNodes(
                        SemanticsMatcher("text") { node ->
                            node.config
                                .getOrNull(SemanticsProperties.Text)
                                ?.isNotEmpty() == true
                        },
                        useUnmergedTree = true)
                    .fetchSemanticsNodes()
                    .any { node ->
                        node.config.getOrNull(SemanticsProperties.Text)?.any {
                            it.text.contains("backup-preview-marker")
                        } == true
                    },
                "preview text was not visible: " +
                    onAllNodes(
                            SemanticsMatcher("all") { true },
                            useUnmergedTree = true)
                        .fetchSemanticsNodes()
                        .mapNotNull { node ->
                            node.config
                                .getOrNull(SemanticsProperties.Text)
                                ?.joinToString("") { it.text }
                        },
            )
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(1)
            onNodeWithTag(
                    "playlist-backup-preview-dismiss", useUnmergedTree = true)
                .assertExists()
            onAllNodes(dialogMatcher, useUnmergedTree = true)[0]
                .performSemanticsAction(SemanticsActions.Dismiss)
            assertEquals(
                listOf<PlaylistBackupUiAction>(
                    PlaylistBackupUiAction.DismissPreview),
                backupActions)

            backupState.value = PlaylistBackupUiState(result = backupResult())
            waitForIdle()
            onNodeWithTag(
                    "playlist-backup-result-dismiss", useUnmergedTree = true)
                .assertExists()
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(1)

            backupState.value = PlaylistBackupUiState()
            waitForIdle()
            onNodeWithTag("settings-clear", useUnmergedTree = true)
                .performScrollTo()
                .performClick()
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(1)
            assertEquals(0, clearCalls)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun currentStaleAndReplacedIdsResolveAtInvocation() = runComposeUiTest {
        val sourceId = "same-id"
        val initial = source(sourceId, "initial")
        val current = source(sourceId, "current")
        val replacement = source(sourceId, "replacement")
        val sources = mutableListOf(initial)
        val rescans = mutableListOf<LibrarySource>()
        val removals = mutableListOf<LibrarySource>()
        setContent {
            Harness(
                sources = sources,
                onRescan = { rescans += it },
                onRemove = { removals += it },
            )
        }
        sources[0] = current
        onNodeWithTag("settings-rescan-$sourceId", useUnmergedTree = true)
            .performClick()
        assertEquals(1, rescans.size)
        assertSame(current, rescans.single())

        onNodeWithTag("settings-remove-$sourceId", useUnmergedTree = true)
            .performClick()
        sources[0] = replacement
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .performClick()
        assertEquals(1, removals.size)
        assertSame(replacement, removals.single())

        onNodeWithTag("settings-remove-$sourceId", useUnmergedTree = true)
            .performClick()
        sources.clear()
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .performClick()
        assertEquals(1, removals.size)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun guardChangesAndErrorsRemainSharedOwned() = runComposeUiTest {
        val job =
            CoroutineScope(Dispatchers.Unconfined).launch(
                start = CoroutineStart.LAZY) {
                    awaitCancellation()
                }
        val source = source("one")
        val removals = mutableListOf<LibrarySource>()
        val sentinel = IllegalStateException("callback sentinel")
        setContent {
            Harness(
                sources = listOf(source),
                hasTracks = true,
                scanJob = job,
                onRescan = { throw sentinel },
                onRemove = { removals += it },
            )
        }
        assertEquals(false, job.isActive)
        job.start()
        onNodeWithTag("settings-rescan-one", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-remove-one", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .performClick()
        assertEquals(emptyList(), removals)

        job.cancel()
        val failure =
            assertFailsWith<IllegalStateException> {
                onNodeWithTag("settings-rescan-one", useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.OnClick)
            }
        onNodeWithTag("settings-remove-one", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .performClick()
        assertEquals(listOf(source), removals)
        assertSame(sentinel, failure)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun injectsSharedLabelsAndCurrentCatalogLoader() = runComposeUiTest {
        val route = mutableStateOf<LibraryRoute>(LibraryRoute.Settings)
        val labels = mutableStateOf<SharedSettingsLabels?>(null)
        val picker = CountingPicker()
        val launcher = mutableStateOf<PlatformFolderPickerLauncher>(picker)
        val source = source("labels-source", "Labels source")
        var aboutCalls = 0
        var librariesCalls = 0
        setContent {
            Harness(
                route = route,
                sources = listOf(source),
                hasTracks = true,
                folderPickerLauncher = launcher.value,
                onSharedLabelsResolved = { labels.value = it },
                onShowSettingsAbout = {
                    aboutCalls++
                    route.value = LibraryRoute.SettingsAbout
                },
                onShowOpenSourceLibraries = {
                    librariesCalls++
                    route.value = LibraryRoute.OpenSourceLibraries
                },
            )
        }
        val shared = labels.value ?: error("Shared labels were not resolved")
        onNode(hasText(shared.settings), useUnmergedTree = true).assertExists()
        onNodeWithTag("settings-picker", useUnmergedTree = true)
            .performScrollTo()
            .performClick()
        assertEquals(1, picker.launchCalls)
        onNode(hasText(shared.addMusicFolder), useUnmergedTree = true)
            .assertExists()
        onNode(hasText(shared.clearLibrary), useUnmergedTree = true)
            .assertExists()

        onNodeWithTag("settings-remove-${source.id}", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-remove-dismiss", useUnmergedTree = true)
            .assertExists()
            .performClick()
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .assertDoesNotExist()

        onNodeWithTag("settings-remove-${source.id}", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-remove-confirm", useUnmergedTree = true)
            .assertExists()
        onNode(hasText(shared.cancel), useUnmergedTree = true).assertExists()
        onNode(hasText(shared.remove), useUnmergedTree = true).assertExists()

        launcher.value = unavailablePicker
        waitForIdle()
        onNodeWithTag("settings-remove-dismiss", useUnmergedTree = true)
            .performClick()
        onNodeWithTag("settings-picker", useUnmergedTree = true).assertExists()
        onNode(hasText(shared.folderPickerUnavailable), useUnmergedTree = true)
            .assertExists()

        onNodeWithTag("settings-about", useUnmergedTree = true).performClick()
        assertEquals(1, aboutCalls)
        assertEquals(LibraryRoute.SettingsAbout, route.value)
        waitForIdle()
        onNodeWithTag("about-libraries", useUnmergedTree = true).performClick()
        assertEquals(1, librariesCalls)
        assertEquals(LibraryRoute.OpenSourceLibraries, route.value)
        waitForIdle()
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithTag("about-libraries-loaded", useUnmergedTree = true)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        onNode(hasText("Activity"), useUnmergedTree = true).assertExists()
        onNodeWithTag("about-libraries-loading", useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun clearDialogRequestDismissAndConfirmFollowSharedLifecycle() =
        runComposeUiTest {
            var clearCalls = 0
            val labels = mutableStateOf<SharedSettingsLabels?>(null)
            setContent {
                Harness(
                    hasTracks = true,
                    onClear = { clearCalls++ },
                    onSharedLabelsResolved = { labels.value = it },
                )
            }
            val shared =
                labels.value ?: error("Shared labels were not resolved")
            onNodeWithTag("settings-clear", useUnmergedTree = true)
                .performClick()
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(1)
            onNode(clickableText(shared.cancel), useUnmergedTree = false)
                .performClick()
            waitForIdle()
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(0)
            assertEquals(0, clearCalls)

            onNodeWithTag("settings-clear", useUnmergedTree = true)
                .performClick()
            onNode(clickableText(shared.clearAction), useUnmergedTree = false)
                .performClick()
            waitForIdle()
            assertEquals(1, clearCalls)
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(0)
            waitForIdle()
            assertEquals(1, clearCalls)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsDismissAndSettingsToAboutClearDialogAndReturnDoesNotReopen() =
        runComposeUiTest {
            val appState = LibraryAppState(null)
            appState.pushRoute(LibraryRoute.Settings)
            val route = mutableStateOf(appState.navigation.current)
            val backLabel = mutableStateOf<String?>(null)
            var settingsDismissCalls = 0
            var aboutDismissCalls = 0
            setContent {
                Harness(
                    route = route,
                    appState = appState,
                    hasTracks = true,
                    onCoreBackLabelResolved = { backLabel.value = it },
                    onShowSettingsAbout = {
                        appState.pushRoute(LibraryRoute.SettingsAbout)
                        route.value = appState.navigation.current
                    },
                    onDismiss = {
                        when (route.value) {
                            LibraryRoute.Settings -> settingsDismissCalls++
                            LibraryRoute.SettingsAbout -> aboutDismissCalls++
                            else -> Unit
                        }
                        val session =
                            assertIs<LibraryBackBeginResult.Started>(
                                    appState.beginBack())
                                .session
                        session.complete()
                        route.value = appState.navigation.current
                    },
                )
            }
            val settingsDestination = appState.activeDestinationId
            val back =
                backLabel.value ?: error("Core Back label was not resolved")
            onNodeWithTag("settings-clear", useUnmergedTree = true)
                .performClick()
            onNode(backButton(back), useUnmergedTree = false)
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            assertEquals(1, settingsDismissCalls)
            assertEquals(0, aboutDismissCalls)
            assertEquals(LibraryRoute.Home, route.value)
            assertEquals(LibraryRoute.Home, appState.navigation.current)
            assertNull(appState.pendingBackSession)
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(0)

            appState.pushRoute(LibraryRoute.Settings)
            route.value = appState.navigation.current
            waitForIdle()
            assertTrue(appState.activeDestinationId != settingsDestination)
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(0)

            onNodeWithTag("settings-clear", useUnmergedTree = true)
                .performClick()
            onNodeWithTag("settings-about", useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()
            assertEquals(LibraryRoute.SettingsAbout, route.value)
            onNode(backButton(back), useUnmergedTree = false).performClick()
            waitForIdle()
            assertEquals(LibraryRoute.Settings, route.value)
            assertEquals(1, aboutDismissCalls)
            onAllNodes(dialogMatcher, useUnmergedTree = true)
                .assertCountEquals(0)
            assertNull(appState.pendingBackSession)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun settingsRoutesAndBackRemainSharedOwned() = runComposeUiTest {
        val appState = LibraryAppState(null)
        appState.pushRoute(LibraryRoute.Settings)
        val route = mutableStateOf(appState.navigation.current)
        val unrelatedRecomposition = mutableStateOf(false)
        val backLabel = mutableStateOf<String?>(null)
        var aboutCalls = 0
        var librariesCalls = 0
        val dismissals = mutableListOf<LibraryRoute>()

        fun synchronizeRouteFromState() {
            route.value = appState.navigation.current
        }

        setContent {
            Harness(
                route = route,
                appState = appState,
                hasTracks = unrelatedRecomposition.value,
                onCoreBackLabelResolved = { backLabel.value = it },
                onShowSettingsAbout = {
                    aboutCalls++
                    appState.pushRoute(LibraryRoute.SettingsAbout)
                    synchronizeRouteFromState()
                },
                onShowOpenSourceLibraries = {
                    librariesCalls++
                    appState.pushRoute(LibraryRoute.OpenSourceLibraries)
                    synchronizeRouteFromState()
                },
                onDismiss = {
                    val outgoing = route.value
                    dismissals += outgoing
                    val session =
                        assertIs<LibraryBackBeginResult.Started>(
                                appState.beginBack())
                            .session
                    val target =
                        assertIs<LibraryBackTarget.Route>(session.target)
                    assertEquals(
                        outgoing, target.routePreview.outgoingEntry.route)
                    assertEquals(
                        LibraryBackBeginResult.Suppressed, appState.beginBack())
                    session.complete()
                    assertNull(appState.pendingBackSession)
                    assertEquals(
                        target.routePreview.incomingEntry.route,
                        appState.navigation.current)
                    assertEquals(outgoing, route.value)
                    synchronizeRouteFromState()
                },
            )
        }
        val back = backLabel.value ?: error("Core Back label was not resolved")

        val settingsDestination = appState.activeDestinationId
        unrelatedRecomposition.value = true
        waitForIdle()
        assertEquals(settingsDestination, appState.activeDestinationId)
        assertEquals(LibraryRoute.Settings, route.value)

        onNodeWithTag("settings-about", useUnmergedTree = true).performClick()
        waitForIdle()
        assertEquals(1, aboutCalls)
        assertEquals(LibraryRoute.SettingsAbout, route.value)
        val aboutDestination = appState.activeDestinationId
        assertTrue(aboutDestination != settingsDestination)

        onNodeWithTag("about-libraries", useUnmergedTree = true).performClick()
        waitForIdle()
        assertEquals(1, librariesCalls)
        assertEquals(LibraryRoute.OpenSourceLibraries, route.value)
        val librariesDestination = appState.activeDestinationId
        assertTrue(librariesDestination != aboutDestination)

        onNode(backButton(back), useUnmergedTree = false).performClick()
        waitForIdle()
        assertEquals(LibraryRoute.SettingsAbout, route.value)
        assertEquals(1, aboutCalls)
        assertEquals(1, librariesCalls)
        assertEquals(
            listOf<LibraryRoute>(LibraryRoute.OpenSourceLibraries), dismissals)
        assertEquals(aboutDestination, appState.activeDestinationId)

        onNode(backButton(back), useUnmergedTree = false).performClick()
        waitForIdle()
        assertEquals(LibraryRoute.Settings, route.value)
        assertEquals(settingsDestination, appState.activeDestinationId)
        assertEquals(
            listOf(
                LibraryRoute.OpenSourceLibraries, LibraryRoute.SettingsAbout),
            dismissals,
        )

        onNode(backButton(back), useUnmergedTree = false).performClick()
        waitForIdle()
        assertEquals(
            listOf(
                LibraryRoute.OpenSourceLibraries,
                LibraryRoute.SettingsAbout,
                LibraryRoute.Settings,
            ),
            dismissals,
        )
        assertEquals(LibraryRoute.Home, route.value)
        assertEquals(LibraryRoute.Home, appState.navigation.current)
        assertNull(appState.pendingBackSession)
    }

    @Composable
    private fun Harness(
        route: MutableState<LibraryRoute> =
            mutableStateOf(LibraryRoute.Settings),
        appState: LibraryAppState? = null,
        sources: List<LibrarySource> = emptyList(),
        hasTracks: Boolean = false,
        scanProgress: ScanProgress? = null,
        scanJob: Job? = null,
        folderPickerLauncher: PlatformFolderPickerLauncher = unavailablePicker,
        playlistBackupState: PlaylistBackupUiState = PlaylistBackupUiState(),
        onPlaylistBackupAction: (PlaylistBackupUiAction) -> Unit = {},
        onClear: () -> Unit = {},
        onRescan: (LibrarySource) -> Unit = {},
        onRemove: (LibrarySource) -> Unit = {},
        onCancelScan: () -> Unit = {},
        onShowSettingsAbout: () -> Unit = {
            route.value = LibraryRoute.SettingsAbout
        },
        onShowOpenSourceLibraries: () -> Unit = {
            route.value = LibraryRoute.OpenSourceLibraries
        },
        onDismiss: () -> Unit = {},
        onSharedLabelsResolved: (SharedSettingsLabels) -> Unit = {},
        onCoreBackLabelResolved: (String) -> Unit = {},
    ) {
        val state =
            appState
                ?: androidx.compose.runtime.remember { LibraryAppState(null) }
        val labels =
            SharedSettingsLabels(
                settings = stringResource(SharedRes.string.sharedSettings),
                addMusicFolder =
                    stringResource(SharedRes.string.sharedAddMusicFolder),
                folderPickerUnavailable =
                    stringResource(
                        SharedRes.string.sharedFolderPickerUnavailable),
                clearLibrary =
                    stringResource(SharedRes.string.sharedClearLibrary),
                cancel = stringResource(SharedRes.string.sharedCancel),
                clearAction = stringResource(SharedRes.string.sharedClear),
                remove = stringResource(SharedRes.string.sharedRemove),
            )
        val backLabel = stringResource(CoreUiRes.string.coreBack)
        SideEffect {
            onSharedLabelsResolved(labels)
            onCoreBackLabelResolved(backLabel)
        }
        LibraryRouteOverlays(
            route = route.value,
            snapshot = LibrarySnapshot("Library", "", tracks(hasTracks), null),
            libraryTracks = emptyList(),
            playbackController = PlaybackController(FakePlaybackEngine()),
            playbackState = PlaybackState(),
            playlistRepository = EmptyPlaylistRepository,
            playlistState = PlaylistState(),
            playlistBackupState = playlistBackupState,
            backupDocumentAvailable = true,
            destinationId = state.activeDestinationId,
            playlistAppearanceSource =
                rememberPlaylistFeatureAppearanceSource(
                    PlaylistFeatureDestination("settings-test")),
            registerBackSurface = state::registerBackSurface,
            onPlaylistStateAction = {},
            onRefreshPlaylists = {},
            onPlaylistMutation = { _, _ -> },
            onExportPlaylists = {},
            onOpenPlaylistBackup = {},
            onConfirmPlaylistBackup = {},
            onPlaylistBackupAction = onPlaylistBackupAction,
            sources = sources,
            folderPickerLauncher = folderPickerLauncher,
            sourcePickerActionVisible = true,
            importMessage = null,
            scanProgress = scanProgress,
            scanJob = scanJob,
            currentThemeMode = RhythHausThemeMode.System,
            onThemeModeSelected = {},
            onClearLibrary = onClear,
            onRescanSource = onRescan,
            onRemoveSource = onRemove,
            onCancelScan = onCancelScan,
            onShowSettingsAbout = onShowSettingsAbout,
            onShowOpenSourceLibraries = onShowOpenSourceLibraries,
            onDismiss = onDismiss,
            onScrollPositionChanged = {},
        )
    }

    private fun backupPreview(marker: String) =
        PlaylistBackupPreview(
            libraryRevision = 7L,
            reports =
                listOf(
                    PlaylistBackupPlaylistReport(
                        sourcePlaylistIndex = 0,
                        sourceName = marker,
                        counts = PlaylistBackupCounts(2, 0, 0),
                    ),
                ),
            issues = emptyList(),
            totals = PlaylistBackupCounts(2, 0, 0),
            canConfirm = true,
        )

    private fun backupResult() =
        PlaylistBackupImportResult(2, 0, PlaylistBackupCounts(2, 0, 0))

    private fun tracks(hasTracks: Boolean): List<Track> =
        if (hasTracks) {
            listOf(
                Track(
                    "id",
                    "title",
                    "artist",
                    "album",
                    0,
                    TrackAccent(0, 0),
                    AudioSource.FilePath("/id.mp3"),
                ),
            )
        } else {
            emptyList()
        }

    private fun source(
        id: String,
        name: String = id,
        handle: String = "/$id",
        scanned: Boolean = false,
    ) =
        LibrarySource(
            id,
            LibraryPlatformKind.JvmFolder,
            name,
            handle,
            1L,
            if (scanned) 2L else null)

    private class CountingPicker : PlatformFolderPickerLauncher {
        var launchCalls = 0

        override val isAvailable = true
        override val supportsAdditionalSources = true

        override fun launch() {
            launchCalls++
        }
    }

    private val unavailablePicker =
        object : PlatformFolderPickerLauncher {
            override val isAvailable = false
            override val supportsAdditionalSources = false

            override fun launch() = Unit
        }

    private val dialogMatcher =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog)

    private fun clickableText(text: String) =
        SemanticsMatcher("clickable text '$text'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick) != null &&
                node.config.getOrNull(SemanticsProperties.Text)?.joinToString(
                    "") {
                        it.text
                    } == text
        }

    private fun backButton(label: String) =
        SemanticsMatcher("core Back '$label'") { node ->
            node.config.getOrNull(SemanticsActions.OnClick) != null &&
                node.config
                    .getOrNull(SemanticsProperties.ContentDescription)
                    ?.contains(label) == true
        }

    private data class SharedSettingsLabels(
        val settings: String,
        val addMusicFolder: String,
        val folderPickerUnavailable: String,
        val clearLibrary: String,
        val cancel: String,
        val clearAction: String,
        val remove: String,
    )

    private object EmptyPlaylistRepository : PlaylistRepository {
        override fun playlists() = emptyList<PlaylistSummary>()

        override fun playlist(id: String) = null

        override fun entries(playlistId: String) = emptyList<PlaylistEntry>()

        override fun create(name: String): PlaylistSummary = error("unused")

        override fun createWithEntries(
            name: String,
            trackIds: List<String>
        ): PlaylistSummary = error("unused")

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
