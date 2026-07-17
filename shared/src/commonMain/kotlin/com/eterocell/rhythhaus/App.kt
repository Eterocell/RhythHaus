package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.LibraryScanner
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.PlaylistRepository
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.library.ui.loadPlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.mutatePlaylistAndRefresh
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.theme.resolveHausPalette
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.scan_complete_format
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
@Preview
fun App() {
    val controller = koinInject<PlaybackController>()
    val tagLibReader = koinInject<TagLibReader>()
    val repository = koinInject<LibraryRepository>()
    val playlistRepository = koinInject<PlaylistRepository>()
    val platformAccess = koinInject<PlatformSourceAccess>()
    val scanner = koinInject<LibraryScanner>()
    val themePreferenceStore = koinInject<ThemePreferenceStore>()
    val playbackLifecycle = koinInject<PlaybackProcessLifecycle>()
    val playbackReconciler = koinInject<PlaybackSessionReconciler>()
    val initialLibraryContent = remember { loadLibraryContent(repository, platformAccess) }
    val playlistStateOwner = remember { PlaylistStateOwner(playlistRepository, Dispatchers.Default) }
    var initialPublication by remember { mutableStateOf(InitialLibraryPublicationState()) }
    var librarySources by remember { mutableStateOf(emptyList<LibrarySource>()) }
    var libraryTracks by remember { mutableStateOf(emptyList<LibraryTrack>()) }
    var playlistState by remember { mutableStateOf(PlaylistState(isLoading = true)) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scanCancellationRequested = remember { MutableStateFlow(false) }
    val scope = rememberCoroutineScope()
    val scanCompleteFormat = stringResource(Res.string.scan_complete_format)
    val selectedThemeMode by themePreferenceStore.selectedThemeMode.collectAsState(RhythHausThemeMode.System)

    LaunchedEffect(initialLibraryContent) {
        publishInitialLibraryContent(
            lifecycle = playbackLifecycle,
            reconciler = playbackReconciler,
            content = initialLibraryContent,
            updateState = { state ->
                initialPublication = state
                state.content?.let {
                    librarySources = it.sources
                    libraryTracks = it.tracks
                }
                state.errorMessage?.let { importMessage = it }
            },
        )
    }

    fun refreshPlaylists() {
        playlistState = reducePlaylistState(playlistState, PlaylistStateAction.LoadStarted)
        scope.launch {
            playlistState = try {
                reducePlaylistState(playlistState, PlaylistStateAction.SnapshotConfirmed(playlistStateOwner.refresh()))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                reducePlaylistState(playlistState, PlaylistStateAction.ReadFailed(PlaylistReadFailedMessage))
            }
        }
    }

    LaunchedEffect(playlistRepository) {
        refreshPlaylists()
    }

    fun updateLibraryContent(content: LibraryContentState) {
        librarySources = content.sources
        libraryTracks = content.tracks
    }

    fun launchSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed || !sourceMutationsAllowed(
                isProgressActive = scanProgress?.isActive == true,
                isJobActive = scanJob?.isActive == true,
            )
        ) return
        scanCancellationRequested.value = false
        scanJob = scope.launch(Dispatchers.Default) {
            val progress = ScanProgress(
                session = ScanSession(id = "", sourceId = source.id, status = ScanStatus.Scanning, startedAtEpochMillis = 0L),
            )
            withContext(Dispatchers.Main) { scanProgress = progress }

            val session = scanner.scan(
                source = source,
                isCancelled = { scanCancellationRequested.value },
                onProgress = { latestProgress ->
                    scope.launch(Dispatchers.Main) {
                        scanProgress = latestProgress
                    }
                },
            )

            val content = loadLibraryContent(repository, platformAccess)
            publishScanContentAfterReconcile(
                reconciler = playbackReconciler,
                loadPlaylists = playlistStateOwner::refresh,
                content = content,
                session = session,
                ownerIsActive = { currentCoroutineContext().isActive },
                publish = { publication ->
                    withContext(Dispatchers.Main) {
                        scanProgress = publication.progress
                        importMessage = publication.errorMessage ?: scanCompleteFormat
                            .replaceFirst("%1\$d", session.tracksAdded.toString())
                            .replaceFirst("%2\$d", session.tracksUpdated.toString())
                        updateLibraryContent(publication.content)
                        playlistState = reducePlaylistState(
                            playlistState,
                            PlaylistStateAction.SnapshotConfirmed(publication.playlists),
                        )
                    }
                },
            )
        }
    }

    fun mutationError(message: String) {
        importMessage = message
    }

    fun launchLibraryMutation(block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                mutationError(failure.appFailureMessage())
            }
        }
    }

    fun launchPlaylistMutation(mutation: PlaylistRepository.() -> Unit) {
        scope.launch {
            playlistState = try {
                reducePlaylistState(
                    playlistState,
                    PlaylistStateAction.SnapshotConfirmed(playlistStateOwner.mutate(mutation)),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                reducePlaylistState(playlistState, PlaylistStateAction.MutationFailed(PlaylistMutationFailedMessage))
            }
        }
    }

    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        when (result) {
            is PlatformFolderPickResult.Success -> launchSourceScan(
                normalizePickedSource(result.source, librarySources),
            )

            is PlatformFolderPickResult.Unavailable -> importMessage = result.message

            is PlatformFolderPickResult.Failure -> importMessage = result.message
        }
    }
    val snapshot = remember(libraryTracks) { librarySnapshot(libraryTracks) }

    RhythHausTheme(selectedThemeMode = selectedThemeMode) {
        CompositionLocalProvider(
            LocalTrackArtworkLoader provides { trackId -> repository.artworkForTrack(trackId) },
        ) {
            LibraryHomeScreen(
                snapshot = snapshot,
                libraryTracks = libraryTracks,
                tagLibReader = tagLibReader,
                playbackController = controller,
                playlistRepository = playlistRepository,
                playlistState = playlistState,
                onPlaylistStateAction = { action -> playlistState = reducePlaylistState(playlistState, action) },
                onRefreshPlaylists = ::refreshPlaylists,
                onPlaylistMutation = ::launchPlaylistMutation,
                sources = librarySources,
                folderPickerLauncher = folderPickerLauncher,
                sourcePickerActionVisible = sourcePickerActionVisible(
                    supportsAdditionalSources = folderPickerLauncher.supportsAdditionalSources,
                    sourceCount = librarySources.size,
                ),
                importMessage = importMessage,
                scanProgress = scanProgress,
                scanJob = scanJob,
                currentThemeMode = selectedThemeMode,
                onThemeModeSelected = { mode ->
                    scope.launch {
                        themePreferenceStore.setSelectedThemeMode(mode)
                    }
                },
                onClearLibrary = {
                    if (initialPublication.mutationsAllowed && sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        launchLibraryMutation {
                            clearLibraryInBackground(
                                repository = repository,
                                platformAccess = platformAccess,
                                reconciler = playbackReconciler,
                                ioDispatcher = Dispatchers.Default,
                                ownerIsActive = { currentCoroutineContext().isActive },
                                updateLibrary = ::updateLibraryContent,
                                loadPlaylists = playlistStateOwner::refresh,
                                updatePlaylists = { state ->
                                    playlistState = reducePlaylistState(playlistState, PlaylistStateAction.SnapshotConfirmed(state))
                                },
                                updateError = ::mutationError,
                            )
                        }
                    }
                },
                onRescanSource = ::launchSourceScan,
                onRemoveSource = { source ->
                    if (initialPublication.mutationsAllowed && sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        launchLibraryMutation {
                            removeSourceInBackground(
                                sourceId = source.id,
                                repository = repository,
                                platformAccess = platformAccess,
                                reconciler = playbackReconciler,
                                ioDispatcher = Dispatchers.Default,
                                ownerIsActive = { currentCoroutineContext().isActive },
                                updateLibrary = ::updateLibraryContent,
                                loadPlaylists = playlistStateOwner::refresh,
                                updatePlaylists = { state ->
                                    playlistState = reducePlaylistState(playlistState, PlaylistStateAction.SnapshotConfirmed(state))
                                },
                                updateError = ::mutationError,
                            )
                        }
                    }
                },
                onCancelScan = {
                    scanCancellationRequested.value = true
                    scanProgress = scanProgress.requestScanCancellation()
                },
            )
        }
    }
}

internal data class LibraryContentState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
)

internal data class InitialLibraryPublicationState(
    val content: LibraryContentState? = null,
    val errorMessage: String? = null,
    val isReady: Boolean = false,
) {
    val mutationsAllowed: Boolean get() = isReady

    fun complete(content: LibraryContentState): InitialLibraryPublicationState = copy(
        content = content,
        errorMessage = null,
        isReady = true,
    )

    fun failSafe(content: LibraryContentState, failure: Throwable): InitialLibraryPublicationState = copy(
        content = content,
        errorMessage = failure.appFailureMessage(),
        isReady = true,
    )
}

internal suspend fun publishInitialLibraryContent(
    lifecycle: PlaybackSessionRestorer,
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    updateState: (InitialLibraryPublicationState) -> Unit,
) {
    val pending = InitialLibraryPublicationState()
    try {
        lifecycle.restoreOnce(content.tracks.map(LibraryTrack::toPlayableTrack))
        reconciler.reconcile(content.tracks)
        updateState(pending.complete(content))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        updateState(pending.failSafe(content, failure))
    }
}

internal suspend fun publishLibraryContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    updateLibrary: (LibraryContentState) -> Unit,
) {
    reconciler.reconcile(content.tracks)
    updateLibrary(content)
}

internal data class ScanPublicationState(
    val content: LibraryContentState,
    val progress: ScanProgress,
    val playlists: com.eterocell.rhythhaus.library.ui.PlaylistSnapshot = com.eterocell.rhythhaus.library.ui.PlaylistSnapshot(),
    val errorMessage: String? = null,
)

internal suspend fun publishScanContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    loadPlaylists: (suspend () -> com.eterocell.rhythhaus.library.ui.PlaylistSnapshot)? = null,
    content: LibraryContentState,
    session: ScanSession,
    ownerIsActive: suspend () -> Boolean,
    publish: suspend (ScanPublicationState) -> Unit,
) {
    try {
        reconciler.reconcile(content.tracks)
        publish(
            ScanPublicationState(
                content = content,
                progress = ScanProgress(session),
                playlists = loadPlaylists?.invoke() ?: com.eterocell.rhythhaus.library.ui.PlaylistSnapshot(),
            ),
        )
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            publish(
                ScanPublicationState(
                    content = content,
                    progress = ScanProgress(session.terminalAfterCancellation()),
                    playlists = loadPlaylists?.invoke() ?: com.eterocell.rhythhaus.library.ui.PlaylistSnapshot(),
                ),
            )
        }
        throw cancelled
    } catch (failure: Throwable) {
        publish(
            ScanPublicationState(
                content = content,
                progress = ScanProgress(session.terminalAfterFailure(failure)),
                playlists = loadPlaylists?.invoke() ?: com.eterocell.rhythhaus.library.ui.PlaylistSnapshot(),
                errorMessage = failure.appFailureMessage(),
            ),
        )
    }
}

internal fun loadLibraryContent(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
): LibraryContentState = LibraryContentState(
    sources = repository.sources().map { source ->
        source.copy(accessStatus = platformAccess.accessStatus(source))
    },
    tracks = repository.tracks(),
)

internal suspend fun removeSourceInBackground(
    sourceId: String,
    repository: LibraryRepository,
    loadPlaylists: (suspend () -> com.eterocell.rhythhaus.library.ui.PlaylistSnapshot)? = null,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    updateLibrary: (LibraryContentState) -> Unit,
    updatePlaylists: (com.eterocell.rhythhaus.library.ui.PlaylistSnapshot) -> Unit = {},
    updateError: (String) -> Unit = {},
) {
    val content = withContext(ioDispatcher) {
        val source = repository.sources().firstOrNull { it.id == sourceId }
        repository.removeSource(sourceId)
        source?.let(platformAccess::releaseAccess)
        loadLibraryContent(repository, platformAccess)
    }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        loadPlaylists = loadPlaylists,
        updateLibrary = updateLibrary,
        updatePlaylists = updatePlaylists,
        updateError = updateError,
    )
}

internal suspend fun clearLibraryInBackground(
    repository: LibraryRepository,
    loadPlaylists: (suspend () -> com.eterocell.rhythhaus.library.ui.PlaylistSnapshot)? = null,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    updateLibrary: (LibraryContentState) -> Unit,
    updatePlaylists: (com.eterocell.rhythhaus.library.ui.PlaylistSnapshot) -> Unit = {},
    updateError: (String) -> Unit = {},
) {
    val content = withContext(ioDispatcher) {
        val sources = repository.sources()
        repository.clearAll()
        sources.forEach(platformAccess::releaseAccess)
        loadLibraryContent(repository, platformAccess)
    }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        loadPlaylists = loadPlaylists,
        updateLibrary = updateLibrary,
        updatePlaylists = updatePlaylists,
        updateError = updateError,
    )
}

private suspend fun publishLibraryContentAfterReconcileFailureSafe(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    ownerIsActive: suspend () -> Boolean,
    loadPlaylists: (suspend () -> com.eterocell.rhythhaus.library.ui.PlaylistSnapshot)?,
    updateLibrary: (LibraryContentState) -> Unit,
    updatePlaylists: (com.eterocell.rhythhaus.library.ui.PlaylistSnapshot) -> Unit,
    updateError: (String) -> Unit,
) {
    suspend fun publishAuthoritativeContent() {
        val playlists = loadPlaylists?.invoke()
        updateLibrary(content)
        playlists?.let(updatePlaylists)
    }
    try {
        reconciler.reconcile(content.tracks)
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            publishAuthoritativeContent()
            updateError(cancelled.appFailureMessage())
        }
        throw cancelled
    } catch (failure: Throwable) {
        publishAuthoritativeContent()
        updateError(failure.appFailureMessage())
        return
    }
    publishAuthoritativeContent()
}

internal fun ScanProgress?.requestScanCancellation(): ScanProgress? {
    val session = this?.session ?: return this
    if (session.status != ScanStatus.Scanning) return this
    return copy(session = session.copy(status = ScanStatus.Cancelling))
}

private fun Throwable.appFailureMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "Playback session unavailable"

private const val PlaylistReadFailedMessage = "playlist_load_failed"
private const val PlaylistMutationFailedMessage = "playlist_mutation_failed"

private fun ScanSession.terminalAfterCancellation(): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Cancelled, terminalMessage = "Scan cancelled")
    } else {
        this
    }

private fun ScanSession.terminalAfterFailure(failure: Throwable): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Failed, terminalMessage = failure.appFailureMessage())
    } else {
        this
    }

@Composable
private fun RhythHausTheme(
    selectedThemeMode: RhythHausThemeMode,
    content: @Composable () -> Unit,
) {
    val colors = resolveHausPalette(
        mode = selectedThemeMode,
        systemIsDark = systemPrefersDarkTheme(),
    )
    val colorScheme = if (colors == DarkHausPalette) {
        darkColorScheme(
            primary = colors.ink,
            onPrimary = colors.paper,
            secondary = colors.pulse,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceContainer = colors.panel,
            onSurfaceContainer = colors.ink,
            secondaryVariant = colors.pulse,
            onSecondaryVariant = colors.paper,
            disabledSecondaryVariant = colors.pulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = colors.paper.copy(alpha = 0.28f),
        )
    } else {
        lightColorScheme(
            primary = colors.ink,
            onPrimary = colors.paper,
            secondary = colors.pulse,
            onSecondary = colors.paper,
            background = colors.paper,
            onBackground = colors.ink,
            surface = colors.panel,
            onSurface = colors.ink,
            surfaceContainer = colors.panel,
            onSurfaceContainer = colors.ink,
            secondaryVariant = colors.pulse,
            onSecondaryVariant = colors.paper,
            disabledSecondaryVariant = colors.pulse.copy(alpha = 0.28f),
            disabledOnSecondaryVariant = colors.paper.copy(alpha = 0.28f),
        )
    }

    MiuixTheme(
        colors = colorScheme,
    ) {
        CompositionLocalProvider(LocalHausColors provides colors) {
            content()
        }
    }
}
