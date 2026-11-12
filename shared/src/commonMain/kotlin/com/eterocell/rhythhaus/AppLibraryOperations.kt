package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.DesktopDropResult
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.PlatformFolderPickResult
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.RemoveMissingTracksRejectionReason
import com.eterocell.rhythhaus.library.RemoveMissingTracksResult
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.iosImportSummaryMessage
import com.eterocell.rhythhaus.library.normalizePickedSource
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import rhythhaus.shared.generated.resources.playlists

internal fun resolveMutationScanErrors(
    currentScanErrors: List<ScanError>,
    publication: LibraryMutationPublication,
): List<ScanError> = publication.scanErrors ?: currentScanErrors

internal suspend fun removeSourceInBackground(
    sourceId: String,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val content =
        withContext(ioDispatcher) {
            val source = repository.sources().firstOrNull { it.id == sourceId }
            repository.removeSource(sourceId)
            source?.let(platformAccess::releaseAccess)
            loadLibraryContent(repository, platformAccess)
        }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        playlistStateOwner = playlistStateOwner,
        publish = publish,
    )
}

internal suspend fun removeMissingTracksInBackground(
    sourceId: String,
    latestScanId: String,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val resultAndContent =
        withContext(ioDispatcher) {
            val result = repository.removeMissingTracks(sourceId, latestScanId)
            result to
                if (result is RemoveMissingTracksResult.Removed) {
                    loadLibraryContent(repository, platformAccess)
                } else {
                    null
                }
        }
    val (result, content) = resultAndContent
    val errorMessage =
        when (result) {
            is RemoveMissingTracksResult.Removed -> null
            is RemoveMissingTracksResult.Rejected ->
                removeMissingTracksRejectionMessage(result.reason)
        }
    if (result is RemoveMissingTracksResult.Rejected) {
        publish(
            LibraryMutationPublication(
                null, null, errorMessage, scanErrors = null))
    } else {
        publishLibraryContentAfterReconcileFailureSafe(
            reconciler = reconciler,
            content = requireNotNull(content),
            ownerIsActive = ownerIsActive,
            playlistStateOwner = playlistStateOwner,
            initialError = errorMessage,
            publish = publish,
        )
    }
}

internal suspend fun clearLibraryInBackground(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    reconciler: PlaybackSessionReconciler,
    ioDispatcher: CoroutineDispatcher,
    ownerIsActive: suspend () -> Boolean = { true },
    playlistStateOwner: PlaylistStateOwner,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    val content =
        withContext(ioDispatcher) {
            val sources = repository.sources()
            repository.clearAll()
            sources.forEach(platformAccess::releaseAccess)
            loadLibraryContent(repository, platformAccess)
        }
    publishLibraryContentAfterReconcileFailureSafe(
        reconciler = reconciler,
        content = content,
        ownerIsActive = ownerIsActive,
        playlistStateOwner = playlistStateOwner,
        publish = publish,
    )
}

private suspend fun publishLibraryContentAfterReconcileFailureSafe(
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    ownerIsActive: suspend () -> Boolean,
    playlistStateOwner: PlaylistStateOwner,
    initialError: String? = null,
    publish: suspend (LibraryMutationPublication) -> Unit,
) {
    suspend fun publishAuthoritativeContent(errorMessage: String? = null) {
        val playlists =
            playlistStateOwner.refresh().requireSuccessfulPublication()
        publish(LibraryMutationPublication(content, playlists, errorMessage))
    }
    val reconciliationError =
        try {
            reconciler.reconcile(content.tracks)
            null
        } catch (cancelled: CancellationException) {
            if (ownerIsActive()) {
                withContext(NonCancellable) {
                    runCatching {
                        publishAuthoritativeContent(
                            cancelled.appFailureMessage())
                    }
                }
            }
            throw cancelled
        } catch (failure: Throwable) {
            failure.appFailureMessage()
        }
    publishAuthoritativeContent(reconciliationError ?: initialError)
}

internal fun removeMissingTracksRejectionMessage(
    reason: RemoveMissingTracksRejectionReason,
): String = "Unable to remove missing tracks: ${reason.name}"

/**
 * App effects resolved from one terminal folder-pick/import result.
 *
 * @property message transient import message to publish, or null to leave the
 *   current message unchanged (plain folder-pick successes and silent
 *   cancellations publish nothing).
 * @property scanSource normalized source to scan, or null when no scan may
 *   start (cancellation, unavailable, and failure never scan).
 * @property holdsFollowUpScanGate true only for a successful iOS import
 *   terminal (one carrying an import summary): its scan is a follow-up scan
 *   whose admission must release the App source-mutation exclusion, so App
 *   holds the gate from the terminal until the coordinator settles it.
 */
internal data class LibraryPickerTerminalAction(
    val message: String?,
    val scanSource: LibrarySource?,
    val holdsFollowUpScanGate: Boolean = false,
)

/** App effects resolved from one validated desktop drop. */
internal suspend fun registerAndScanDesktopSources(
    sources: List<LibrarySource>,
    repository: LibraryRepository,
    publishRegistered: suspend () -> Unit,
    scan: suspend (LibrarySource) -> ScanSession,
    cancellationRequested: () -> Boolean,
): List<ScanSession> {
    withContext(NonCancellable) {
        sources.forEach(repository::upsertSource)
        publishRegistered()
    }
    val outcomes = mutableListOf<ScanSession>()
    for (source in sources) {
        if (cancellationRequested()) break
        outcomes += scan(source)
    }
    return outcomes
}

/** App effects resolved from one validated desktop drop. */
internal data class DesktopDropTerminalAction(
    val sourcesToScan: List<LibrarySource>,
    val addedSourceCount: Int,
    val reusedSourceCount: Int,
    val rejectedEntryCount: Int,
)

/**
 * Reuses existing source identities before the App-owned scan coordinator
 * admits a desktop drop. The resulting sources are unique, so scanner upserts
 * cannot create a duplicate source when a path is dropped again.
 */
internal fun resolveDesktopDropTerminal(
    result: DesktopDropResult,
    existingSources: List<LibrarySource>,
): DesktopDropTerminalAction {
    val existingSourceIds = existingSources.mapTo(mutableSetOf()) { it.id }
    val normalizedSources =
        result.sources
            .map { source -> normalizePickedSource(source, existingSources) }
            .distinctBy(LibrarySource::id)
    val collapsedSourceCount = result.sources.size - normalizedSources.size
    return DesktopDropTerminalAction(
        sourcesToScan = normalizedSources,
        addedSourceCount =
            normalizedSources.count { source ->
                source.id !in existingSourceIds
            },
        reusedSourceCount =
            result.duplicatePathCount +
                collapsedSourceCount +
                normalizedSources.count { source ->
                    source.id in existingSourceIds
                },
        rejectedEntryCount = result.rejectedEntryCount,
    )
}

/**
 * Resolves one terminal picker result into App effects.
 *
 * A successful iOS import publishes its aggregate count summary and requests
 * exactly one scan of the normalized managed app-local source. Folder-pick
 * successes without a summary (Android/JVM) keep the legacy behavior: scan
 * without touching the transient message. Cancellation is silent and never
 * scans; unavailable and failure publish their message and never scan.
 *
 * @param result the terminal picker result.
 * @param existingSources the currently configured library sources, used to
 *   normalize the picked source identity.
 */
internal fun resolveLibraryPickerTerminal(
    result: PlatformFolderPickResult,
    existingSources: List<LibrarySource>,
    importSummaryFormat: String =
        "Imported %1\$d, duplicates %2\$d, unsupported %3\$d, failed %4\$d",
): LibraryPickerTerminalAction =
    when (result) {
        is PlatformFolderPickResult.Success ->
            LibraryPickerTerminalAction(
                message =
                    result.importSummary?.let {
                        iosImportSummaryMessage(it, importSummaryFormat)
                    },
                scanSource =
                    normalizePickedSource(result.source, existingSources),
                holdsFollowUpScanGate = result.importSummary != null,
            )

        is PlatformFolderPickResult.Cancelled ->
            LibraryPickerTerminalAction(message = null, scanSource = null)

        is PlatformFolderPickResult.Unavailable ->
            LibraryPickerTerminalAction(
                message = result.message,
                scanSource = null,
            )

        is PlatformFolderPickResult.Failure ->
            LibraryPickerTerminalAction(
                message = result.message,
                scanSource = null,
            )
    }

/**
 * App-owned source-mutation gating.
 *
 * Mutations require the initial publication to allow them, the operation
 * coordinator to be idle, and no platform import to be active. Folding the iOS
 * import-active window into this existing gate blocks competing source
 * mutations while the picker is shown or files are being copied, before the
 * follow-up scan is even admitted; the follow-up pending flag keeps the
 * exclusion in force after the import terminal clears the import-active flag
 * and until exactly that follow-up scan is admitted or rejected by the
 * coordinator. Android and JVM launchers report their import as never active
 * and never hold a follow-up gate, so their gating is unchanged.
 *
 * @param publicationMutationsAllowed whether the initial library publication
 *   permits mutations.
 * @param coordinatorIdle whether the operation coordinator is idle.
 * @param importActive whether a platform import operation is active.
 * @param followUpScanPending whether a successful import's follow-up scan has
 *   not yet been conclusively admitted or rejected by the coordinator.
 */
internal fun appLibraryMutationsEnabled(
    publicationMutationsAllowed: Boolean,
    coordinatorIdle: Boolean,
    importActive: Boolean,
    followUpScanPending: Boolean = false,
): Boolean =
    publicationMutationsAllowed &&
        coordinatorIdle &&
        !importActive &&
        !followUpScanPending

/**
 * Runs one follow-up scan admission and guarantees the pending exclusion is
 * released once that admission has conclusively settled.
 *
 * [AppLibraryOrchestrator.launchScan] completes its coordinator token and then
 * rethrows a CancellationException when the scan is cancelled, so a plain
 * statement after the call would leak the pending gate forever on that path.
 * The release therefore runs in a non-cancellable finally; the original
 * cancellation keeps propagating, so a cancelled follow-up stays silent and is
 * never retried.
 *
 * @param scan the actual follow-up scan launch through the App-owned
 *   coordinator admission flow.
 * @param releasePending clears the App follow-up pending state.
 */
internal suspend fun settleFollowUpScanPending(
    scan: suspend () -> Unit,
    releasePending: () -> Unit,
) {
    try {
        scan()
    } finally {
        withContext(NonCancellable) {
            releasePending()
        }
    }
}

internal fun ScanProgress?.requestScanCancellation(): ScanProgress? {
    val session = this?.session ?: return this
    if (session.status != ScanStatus.Scanning) return this
    return copy(session = session.copy(status = ScanStatus.Cancelling))
}

internal fun restoredTerminalScanProgress(
    session: ScanSession?,
    sources: List<LibrarySource>,
): ScanProgress? =
    session
        ?.takeIf {
            it.status in
                setOf(
                    ScanStatus.Completed,
                    ScanStatus.Cancelled,
                    ScanStatus.Failed)
        }
        ?.takeIf { terminal -> sources.any { it.id == terminal.sourceId } }
        ?.let(::ScanProgress)

internal fun restoredCompletedScanSession(
    session: ScanSession?,
    sources: List<LibrarySource>,
): ScanSession? =
    session
        ?.takeIf { it.status == ScanStatus.Completed }
        ?.takeIf { completed -> sources.any { it.id == completed.sourceId } }

internal data class RestoredTerminalScanState(
    val progress: ScanProgress?,
    val errors: List<ScanError>,
)

internal fun restoredTerminalScanState(
    session: ScanSession?,
    sources: List<LibrarySource>,
    errors: List<ScanError>,
): RestoredTerminalScanState {
    val progress = restoredTerminalScanProgress(session, sources)
    return RestoredTerminalScanState(
        progress = progress,
        errors =
            if (progress?.session?.id == session?.id) errors else emptyList(),
    )
}

internal fun removeMissingTracksPublication(
    result: RemoveMissingTracksResult,
    content: LibraryContentState,
): LibraryMutationPublication =
    when (result) {
        is RemoveMissingTracksResult.Removed ->
            LibraryMutationPublication(content, null, null)
        is RemoveMissingTracksResult.Rejected ->
            LibraryMutationPublication(
                content = null,
                playlists = null,
                errorMessage =
                    removeMissingTracksRejectionMessage(result.reason),
                scanErrors = null,
            )
    }

internal fun Throwable.appFailureMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "Playback session unavailable"

private fun ScanSession.terminalAfterFailure(failure: Throwable): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(
            status = ScanStatus.Failed,
            terminalMessage = failure.appFailureMessage())
    } else {
        this
    }
