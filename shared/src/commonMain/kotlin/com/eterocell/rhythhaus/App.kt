package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.rememberPlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.sourcePickerActionVisible
import com.eterocell.rhythhaus.library.sourceMutationsAllowed
import com.eterocell.rhythhaus.library.ui.LibraryHomeScreen
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.theme.DarkHausPalette
import com.eterocell.rhythhaus.theme.LocalHausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.theme.ThemePreferenceStore
import com.eterocell.rhythhaus.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.theme.resolveHausPalette
import com.eterocell.rhythhaus.theme.systemPrefersDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineDispatcher
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
    val platformAccess = koinInject<PlatformSourceAccess>()
    val scanner = koinInject<LibraryScanner>()
    val themePreferenceStore = koinInject<ThemePreferenceStore>()
    val initialLibraryContent = remember { loadLibraryContent(repository, platformAccess) }
    var librarySources by remember { mutableStateOf(initialLibraryContent.sources) }
    var libraryTracks by remember { mutableStateOf(initialLibraryContent.tracks) }
    var importMessage by remember { mutableStateOf<String?>(null) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    var scanJob by remember { mutableStateOf<Job?>(null) }
    val scanCancellationRequested = remember { MutableStateFlow(false) }
    val scope = rememberCoroutineScope()
    val scanCompleteFormat = stringResource(Res.string.scan_complete_format)
    val selectedThemeMode by themePreferenceStore.selectedThemeMode.collectAsState(RhythHausThemeMode.System)

    fun updateLibraryContent(content: LibraryContentState) {
        librarySources = content.sources
        libraryTracks = content.tracks
    }

    fun launchSourceScan(source: LibrarySource) {
        if (!sourceMutationsAllowed(
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
            withContext(Dispatchers.Main) {
                scanProgress = ScanProgress(session = session)
                importMessage = scanCompleteFormat
                    .replaceFirst("%1\$d", session.tracksAdded.toString())
                    .replaceFirst("%2\$d", session.tracksUpdated.toString())
                updateLibraryContent(content)
            }
        }
    }

    val folderPickerLauncher = rememberPlatformFolderPickerLauncher { result ->
        when (result) {
            is PlatformFolderPickResult.Success -> launchSourceScan(result.source)

            is PlatformFolderPickResult.Unavailable -> importMessage = result.message

            is PlatformFolderPickResult.Failure -> importMessage = result.cause?.let { "${result.message}: $it" } ?: result.message
        }
    }
    DisposableEffect(controller) {
        onDispose { controller.release() }
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
                    if (sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        scope.launch {
                            clearLibraryInBackground(
                                repository = repository,
                                platformAccess = platformAccess,
                                ioDispatcher = Dispatchers.Default,
                                updateLibrary = ::updateLibraryContent,
                            )
                        }
                    }
                },
                onRescanSource = ::launchSourceScan,
                onRemoveSource = { source ->
                    if (sourceMutationsAllowed(
                            isProgressActive = scanProgress?.isActive == true,
                            isJobActive = scanJob?.isActive == true,
                        )
                    ) {
                        scope.launch {
                            removeSourceInBackground(
                                sourceId = source.id,
                                repository = repository,
                                platformAccess = platformAccess,
                                ioDispatcher = Dispatchers.Default,
                                updateLibrary = ::updateLibraryContent,
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
    platformAccess: PlatformSourceAccess,
    ioDispatcher: CoroutineDispatcher,
    updateLibrary: (LibraryContentState) -> Unit,
) {
    val content = withContext(ioDispatcher) {
        repository.removeSource(sourceId)
        loadLibraryContent(repository, platformAccess)
    }
    updateLibrary(content)
}

internal suspend fun clearLibraryInBackground(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    ioDispatcher: CoroutineDispatcher,
    updateLibrary: (LibraryContentState) -> Unit,
) {
    val content = withContext(ioDispatcher) {
        repository.clearAll()
        loadLibraryContent(repository, platformAccess)
    }
    updateLibrary(content)
}

internal fun ScanProgress?.requestScanCancellation(): ScanProgress? {
    val session = this?.session ?: return this
    if (session.status != ScanStatus.Scanning) return this
    return copy(session = session.copy(status = ScanStatus.Cancelling))
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
