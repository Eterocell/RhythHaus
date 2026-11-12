package com.eterocell.rhythhaus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.SourceRebindResult
import com.eterocell.rhythhaus.library.defaultPlatformLibrarySource
import com.eterocell.rhythhaus.library.registerMissingDefaultLibrarySource
import com.eterocell.rhythhaus.library.ui.PlaylistState
import com.eterocell.rhythhaus.library.ui.reducePlaylistState
import com.eterocell.rhythhaus.library.validateDesktopDrop
import com.eterocell.rhythhaus.mediastore.MediaStoreAudioPermissionController
import com.eterocell.rhythhaus.mediastore.registerMediaStoreAudioSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rhythhaus.shared.generated.resources.playlists

internal data class AppLibraryOperationLabels(
    val scanCompleteFormat: String,
    val desktopDropAddedFormat: String,
    val desktopDropBusy: String,
    val desktopDropFailedFormat: String,
    val desktopDropDuplicate: String,
    val desktopDropRejected: String,
    val desktopDropSummaryFormat: String,
    val iosImportSummaryFormat: String,
)

/**
 * App-lifetime workflow adapter; admission and revisions stay in existing
 * owners.
 */
internal class AppLibraryOperationsHost(
    private val library: AppLibraryServices,
    private val scope: kotlinx.coroutines.CoroutineScope,
    private val currentContent: () -> LibraryContentState,
    private val currentInitial: () -> InitialLibraryPublicationState,
    private val currentPlaylist: () -> PlaylistState,
    private val publishPlaylist: (PlaylistState) -> Unit,
    private val updateLibraryContent: suspend (LibraryContentState) -> Unit,
    private val labels: () -> AppLibraryOperationLabels,
    private val isDesktopModalFree: () -> Boolean,
    private val mediaStorePermissionController:
        MediaStoreAudioPermissionController,
) {
    private val repository
        get() = library.repository

    private val platformAccess
        get() = library.platformAccess

    private val scanner
        get() = library.scanner

    private val playbackReconciler
        get() = library.playbackReconciler

    private val playlistStateOwner
        get() = library.playlistStateOwner

    private val libraryContent
        get() = currentContent()

    private val initialPublication
        get() = currentInitial()

    private var playlistState: PlaylistState
        get() = currentPlaylist()
        set(value) = publishPlaylist(value)

    private val desktopModalFree
        get() = isDesktopModalFree()

    private val scanCompleteFormat
        get() = labels().scanCompleteFormat

    private val desktopDropAddedFormat
        get() = labels().desktopDropAddedFormat

    private val desktopDropBusy
        get() = labels().desktopDropBusy

    private val desktopDropFailedFormat
        get() = labels().desktopDropFailedFormat

    private val desktopDropDuplicate
        get() = labels().desktopDropDuplicate

    private val desktopDropRejected
        get() = labels().desktopDropRejected

    private val desktopDropSummaryFormat
        get() = labels().desktopDropSummaryFormat

    private val iosImportSummaryFormat
        get() = labels().iosImportSummaryFormat

    var importMessage by mutableStateOf<String?>(null)
        private set

    var followUpScanPending by mutableStateOf(false)
        private set

    var scanProgress by mutableStateOf<ScanProgress?>(null)
        private set

    var latestCompletedScanSession by mutableStateOf<ScanSession?>(null)
        private set

    var scanErrors by mutableStateOf(emptyList<ScanError>())
        private set

    var scanJob by mutableStateOf<Job?>(null)
        private set

    private val scanCancellationRequested = MutableStateFlow(false)
    lateinit var folderPickerLauncher:
        com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
    val operationCoordinator = AppLibraryOperationCoordinator {
        scanCancellationRequested.value = true
        scanProgress = scanProgress.requestScanCancellation()
        scanJob?.join()
    }
    val libraryOrchestrator =
        AppLibraryOrchestrator(
            coordinator = operationCoordinator,
            publishError = { message ->
                withContext(Dispatchers.Main) { importMessage = message }
            },
        )
    val mutationsEnabled: Boolean
        get() =
            appLibraryMutationsEnabled(
                publicationMutationsAllowed =
                    initialPublication.mutationsAllowed,
                coordinatorIdle =
                    operationCoordinator.state.value
                        is LibraryOperationState.Idle,
                importActive = folderPickerLauncher.isImportActive,
                followUpScanPending = followUpScanPending,
            )

    suspend fun performSourceScan(
        source: LibrarySource,
        token: LibraryOperationToken,
        resetCancellation: Boolean = true,
    ): ScanSession {
        scanJob = currentCoroutineContext()[Job]
        if (resetCancellation) {
            scanCancellationRequested.value = false
        }
        var progressCallbacks: OrderedScanProgressCallbacks? = null
        try {
            val progress =
                ScanProgress(
                    session =
                        ScanSession(
                            id = "",
                            sourceId = source.id,
                            status = ScanStatus.Scanning,
                            startedAtEpochMillis = 0L),
                )
            libraryOrchestrator.publishIfCurrent(token) {
                withContext(Dispatchers.Main) {
                    scanProgress = progress
                }
            }

            progressCallbacks =
                OrderedScanProgressCallbacks(scope) { latestProgress ->
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            scanProgress = latestProgress
                            if (latestProgress.session?.status ==
                                ScanStatus.Completed) {
                                latestCompletedScanSession =
                                    latestProgress.session
                            }
                        }
                    }
                }
            val session =
                scanner.scan(
                    source = source,
                    isCancelled = { scanCancellationRequested.value },
                    onProgress = { latestProgress ->
                        progressCallbacks.offer(latestProgress)
                    },
                )

            progressCallbacks.awaitPublished()
            val content = loadLibraryContent(repository, platformAccess)
            publishScanContentAfterReconcile(
                reconciler = playbackReconciler,
                playlistStateOwner = playlistStateOwner,
                content = content,
                session = session,
                loadScanErrors = repository::scanErrors,
                ownerIsActive = { currentCoroutineContext().isActive },
                publish = { publication ->
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            scanProgress = publication.progress
                            scanErrors = publication.scanErrors
                            importMessage =
                                publication.errorMessage
                                    ?: scanCompleteFormat
                                        .replaceFirst(
                                            "%1\$d",
                                            session.tracksAdded.toString())
                                        .replaceFirst(
                                            "%2\$d",
                                            session.tracksUpdated.toString())
                            updateLibraryContent(publication.content)
                            publication.playlists?.let { action ->
                                playlistState =
                                    reducePlaylistState(
                                        playlistState,
                                        action.requireSuccessfulPublication(),
                                    )
                            }
                        }
                    }
                },
            )
            return session
        } finally {
            withContext(NonCancellable) {
                progressCallbacks?.awaitPublished()
            }
        }
    }

    suspend fun performDesktopDropScans(
        sources: List<LibrarySource>,
        token: LibraryOperationToken,
    ) {
        scanJob = currentCoroutineContext()[Job]
        scanCancellationRequested.value = false
        val outcomes =
            registerAndScanDesktopSources(
                sources = sources,
                repository = repository,
                publishRegistered = {
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            updateLibraryContent(
                                loadLibraryContent(repository, platformAccess))
                        }
                    }
                },
                scan = { source ->
                    performSourceScan(
                        source = source,
                        token = token,
                        resetCancellation = false,
                    )
                },
                cancellationRequested = { scanCancellationRequested.value },
            )
        val failures = outcomes.filter { it.status == ScanStatus.Failed }
        if (failures.isNotEmpty()) {
            val details =
                failures.joinToString("; ") { session ->
                    val name =
                        sources
                            .firstOrNull { it.id == session.sourceId }
                            ?.displayName ?: session.sourceId
                    session.terminalMessage?.let { "$name: $it" } ?: name
                }
            libraryOrchestrator.publishIfCurrent(token) {
                withContext(Dispatchers.Main) {
                    importMessage =
                        desktopDropFailedFormat
                            .replace("%1\$d", failures.size.toString())
                            .replace("%2\$s", details)
                }
            }
        }
    }

    fun launchSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed) return
        scope.launch(Dispatchers.Default) {
            libraryOrchestrator.launchScan { token ->
                performSourceScan(source, token)
            }
        }
    }

    fun launchFollowUpSourceScan(source: LibrarySource) {
        if (!initialPublication.mutationsAllowed) {
            followUpScanPending = false
            return
        }
        scope.launch(Dispatchers.Default) {
            settleFollowUpScanPending(
                scan = {
                    libraryOrchestrator.launchScan { token ->
                        performSourceScan(source, token)
                    }
                },
                releasePending = { followUpScanPending = false },
            )
        }
    }

    fun mutationError(message: String) {
        importMessage = message
    }

    suspend fun handleDesktopDrop(paths: List<String>) {
        if (!desktopModalFree || !mutationsEnabled) {
            importMessage = desktopDropBusy
            return
        }
        val action =
            resolveDesktopDropTerminal(
                result =
                    withContext(Dispatchers.Default) {
                        validateDesktopDrop(
                            paths = paths,
                            createdAtEpochMillis =
                                com.eterocell.rhythhaus.library
                                    .currentTimeMillis(),
                        )
                    },
                existingSources = libraryContent.sources,
            )
        if (!desktopModalFree ||
            !initialPublication.mutationsAllowed ||
            folderPickerLauncher.isImportActive ||
            followUpScanPending ||
            operationCoordinator.state.value !is LibraryOperationState.Idle) {
            importMessage = desktopDropBusy
            return
        }
        if (action.sourcesToScan.isEmpty()) {
            importMessage = desktopDropRejected
            return
        }
        val acceptedMessage =
            when {
                action.rejectedEntryCount > 0 || action.reusedSourceCount > 0 ->
                    desktopDropSummaryFormat
                        .replaceFirst(
                            "%1\$d", action.addedSourceCount.toString())
                        .replaceFirst(
                            "%2\$d", action.reusedSourceCount.toString())
                        .replaceFirst(
                            "%3\$d", action.rejectedEntryCount.toString())

                action.addedSourceCount == 0 -> desktopDropDuplicate

                else ->
                    desktopDropAddedFormat.replaceFirst(
                        "%1\$d", action.addedSourceCount.toString())
            }
        val admitted =
            withContext(Dispatchers.Default) {
                libraryOrchestrator.launchScan { token ->
                    if (!withContext(Dispatchers.Main) { desktopModalFree }) {
                        withContext(Dispatchers.Main) {
                            importMessage = desktopDropBusy
                        }
                        return@launchScan
                    }
                    libraryOrchestrator.publishIfCurrent(token) {
                        withContext(Dispatchers.Main) {
                            importMessage = acceptedMessage
                        }
                    }
                    performDesktopDropScans(
                        sources = action.sourcesToScan,
                        token = token,
                    )
                }
            }
        if (!admitted) importMessage = desktopDropBusy
    }

    suspend fun restoreInitialLibrary() {
        if (!initialPublication.isReady) return
        val initialLibraryContent = initialPublication.content ?: return
        initialPublication.errorMessage?.let { importMessage = it }
        val restoredSession = repository.latestTerminalScanSession()
        val restoredCompletedSession = repository.latestCompletedScanSession()
        val restoredState =
            restoredTerminalScanState(
                restoredSession,
                initialLibraryContent.sources,
                restoredSession?.let { repository.scanErrors(it.id) }.orEmpty(),
            )
        scanProgress = restoredState.progress
        latestCompletedScanSession =
            restoredCompletedScanSession(
                restoredCompletedSession,
                initialLibraryContent.sources,
            )
        scanErrors = restoredState.errors

        defaultPlatformLibrarySource()?.let { defaultSource ->
            val sourceToScan =
                registerMissingDefaultLibrarySource(repository, defaultSource)
            if (sourceToScan != null) {
                updateLibraryContent(
                    loadLibraryContent(repository, platformAccess))
                launchSourceScan(sourceToScan)
            }
        }
    }

    fun receivePickerResult(result: PlatformFolderPickResult) {
        val action =
            resolveLibraryPickerTerminal(
                result = result,
                existingSources = libraryContent.sources,
                importSummaryFormat = iosImportSummaryFormat,
            )
        action.message?.let { importMessage = it }
        action.scanSource?.let { source ->
            if (action.holdsFollowUpScanGate) {
                followUpScanPending = true
                launchFollowUpSourceScan(source)
            } else {
                launchSourceScan(source)
            }
        }
    }

    fun recoverSource(sourceId: String, replacement: LibrarySource) {
        scope.launch {
            libraryOrchestrator.launch(LibraryOperationKind.RebindSource) {
                token ->
                val replacementAlreadyOwned =
                    libraryContent.sources.any { source ->
                        source.id != sourceId &&
                            source.platformKind == replacement.platformKind &&
                            source.handle == replacement.handle
                    }
                val releaseRejectedReplacement = {
                    if (!replacementAlreadyOwned) {
                        platformAccess.releaseAccess(replacement)
                    }
                }
                val result =
                    try {
                        withContext(Dispatchers.Default) {
                            repository.rebindSource(sourceId, replacement)
                        }
                    } catch (failure: Throwable) {
                        releaseRejectedReplacement()
                        throw failure
                    }
                if (result !is SourceRebindResult.Rebound) {
                    releaseRejectedReplacement()
                }
                libraryOrchestrator.publishIfCurrent(token) {
                    withContext(Dispatchers.Main) {
                        when (result) {
                            is SourceRebindResult.Rebound -> {
                                updateLibraryContent(
                                    loadLibraryContent(
                                        repository, platformAccess))
                                importMessage =
                                    "Folder access restored. Scan when ready."
                            }
                            SourceRebindResult.HandleConflict ->
                                importMessage =
                                    "That folder is already configured."
                            SourceRebindResult.PlatformMismatch ->
                                importMessage =
                                    "That folder cannot replace this source."
                            SourceRebindResult.UnknownSource ->
                                importMessage =
                                    "The source is no longer configured."
                        }
                    }
                }
            }
        }
    }

    fun clearLibrary() {
        if (mutationsEnabled) {
            scope.launch {
                libraryOrchestrator.launch(LibraryOperationKind.Clear) { token
                    ->
                    clearLibraryInBackground(
                        repository = repository,
                        platformAccess = platformAccess,
                        reconciler = playbackReconciler,
                        ioDispatcher = Dispatchers.Default,
                        ownerIsActive = {
                            currentCoroutineContext().isActive
                        },
                        playlistStateOwner = playlistStateOwner,
                        publish = { publication ->
                            libraryOrchestrator.publishIfCurrent(token) {
                                withContext(Dispatchers.Main) {
                                    publication.content?.let {
                                        updateLibraryContent(it)
                                    }
                                    scanErrors = emptyList()
                                    publication.playlists?.let { action ->
                                        playlistState =
                                            reducePlaylistState(
                                                playlistState,
                                                action
                                                    .requireSuccessfulPublication(),
                                            )
                                    }
                                    publication.errorMessage?.let(
                                        ::mutationError)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    fun removeSource(source: LibrarySource) {
        if (mutationsEnabled) {
            scope.launch {
                libraryOrchestrator.launch(LibraryOperationKind.RemoveSource) {
                    token ->
                    removeSourceInBackground(
                        sourceId = source.id,
                        repository = repository,
                        platformAccess = platformAccess,
                        reconciler = playbackReconciler,
                        ioDispatcher = Dispatchers.Default,
                        ownerIsActive = {
                            currentCoroutineContext().isActive
                        },
                        playlistStateOwner = playlistStateOwner,
                        publish = { publication ->
                            libraryOrchestrator.publishIfCurrent(token) {
                                withContext(Dispatchers.Main) {
                                    publication.content?.let {
                                        updateLibraryContent(it)
                                    }
                                    scanErrors = emptyList()
                                    publication.playlists?.let { action ->
                                        playlistState =
                                            reducePlaylistState(
                                                playlistState,
                                                action
                                                    .requireSuccessfulPublication(),
                                            )
                                    }
                                    publication.errorMessage?.let(
                                        ::mutationError)
                                }
                            }
                        },
                    )
                }
            }
        }
    }

    fun removeMissingTracks(source: LibrarySource, session: ScanSession) {
        if (mutationsEnabled) {
            scope.launch {
                libraryOrchestrator.launch(
                    LibraryOperationKind.RemoveMissingTracks) { token ->
                        removeMissingTracksInBackground(
                            sourceId = source.id,
                            latestScanId = session.id,
                            repository = repository,
                            platformAccess = platformAccess,
                            reconciler = playbackReconciler,
                            ioDispatcher = Dispatchers.Default,
                            ownerIsActive = {
                                currentCoroutineContext().isActive
                            },
                            playlistStateOwner = playlistStateOwner,
                            publish = { publication ->
                                libraryOrchestrator.publishIfCurrent(token) {
                                    withContext(Dispatchers.Main) {
                                        publication.content?.let {
                                            updateLibraryContent(it)
                                        }
                                        scanErrors =
                                            resolveMutationScanErrors(
                                                scanErrors, publication)
                                        publication.playlists?.let { action ->
                                            playlistState =
                                                reducePlaylistState(
                                                    playlistState,
                                                    action
                                                        .requireSuccessfulPublication(),
                                                )
                                        }
                                        publication.errorMessage?.let(
                                            ::mutationError)
                                    }
                                }
                            },
                        )
                    }
            }
        }
    }

    fun addMediaStoreAudio() {
        if (mutationsEnabled) {
            scope.launch {
                libraryOrchestrator.launch(LibraryOperationKind.AddSource) {
                    token ->
                    withContext(NonCancellable) {
                        withContext(Dispatchers.Default) {
                                registerMediaStoreAudioSource(
                                    repository,
                                    mediaStorePermissionController.state.value,
                                    com.eterocell.rhythhaus.library
                                        .currentTimeMillis(),
                                )
                            }
                            ?.let {
                                libraryOrchestrator.publishIfCurrent(token) {
                                    updateLibraryContent(
                                        loadLibraryContent(
                                            repository, platformAccess))
                                }
                            }
                    }
                }
            }
        }
    }

    fun cancelScan() {
        scanCancellationRequested.value = true
        scanProgress = scanProgress.requestScanCancellation()
    }
}
