package com.eterocell.rhythhaus

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.ScanError
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ScanSession
import com.eterocell.rhythhaus.library.ScanStatus
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.library.ui.PlaylistStateAction
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import rhythhaus.shared.generated.resources.playlists

internal data class LibraryContentState(
    val sources: List<LibrarySource>,
    val tracks: List<LibraryTrack>,
    val favoriteTrackIds: Set<String> = emptySet(),
    val playHistory: Map<String, TrackPlayHistory> = emptyMap(),
    val createdAtByTrackId: Map<String, Long> = emptyMap(),
    val sourceIdByTrackId: Map<String, String> = emptyMap(),
    val modifiedAtByTrackId: Map<String, Long?> = emptyMap(),
    val artworkTrackIds: Set<String> = emptySet(),
)

/** Compose-owned projection consumed by the App's downstream library routes. */
internal class AppLibraryContentState {
    var content by mutableStateOf(LibraryContentState(emptyList(), emptyList()))
        private set

    private var appliedRevision: Long = 0L

    /**
     * Keeps delayed Main-thread application from replacing a newer
     * authoritative snapshot that was published after this one left its owner
     * critical section.
     */
    fun apply(publication: AuthoritativeLibraryPublication): Boolean {
        if (publication.revision <= appliedRevision) return false
        content = publication.content
        appliedRevision = publication.revision
        return true
    }
}

internal data class AuthoritativeLibraryPublication(
    val content: LibraryContentState,
    val revision: Long,
)

internal class AuthoritativeLibraryPublicationOwner {
    private val mutex = Mutex()
    val publications =
        kotlinx.coroutines.flow.MutableStateFlow<
            AuthoritativeLibraryPublication?>(
            null)

    var revision: Long = 0L
        private set

    suspend fun publish(
        content: LibraryContentState
    ): AuthoritativeLibraryPublication = mutex.withLock {
        nextPublication(content)
    }

    suspend fun publishWithFavoriteReconciliation(
        content: LibraryContentState,
        favoriteTrackIds: suspend () -> Set<String>,
        playHistory: suspend () -> Map<String, TrackPlayHistory> = {
            content.playHistory
        },
        createdAtByTrackId: suspend () -> Map<String, Long> = {
            content.createdAtByTrackId
        },
        freshContent: suspend () -> LibraryContentState = { content },
    ): AuthoritativeLibraryPublication = mutex.withLock {
        nextPublication(
            freshContent()
                .copy(
                    favoriteTrackIds = favoriteTrackIds(),
                    playHistory = playHistory().toMap(),
                    createdAtByTrackId = createdAtByTrackId().toMap(),
                ),
        )
    }

    suspend fun publishIfCurrentRevision(
        expectedRevision: Long,
        content: LibraryContentState,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication> =
        mutex.withLock {
            if (revision != expectedRevision) {
                AuthoritativeRevisionResult.Stale
            } else {
                AuthoritativeRevisionResult.Current(nextPublication(content))
            }
        }

    /**
     * Keeps an accepted persistence mutation and its visible authoritative
     * publication in one revision-guarded critical section. Returning null from
     * [mutation] leaves the current publication unchanged.
     */
    suspend fun mutateAndPublishIfCurrentRevision(
        expectedRevision: Long,
        mutation: suspend () -> LibraryContentState?,
        publish: suspend (AuthoritativeLibraryPublication) -> Unit,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication?> =
        mutex.withLock {
            if (revision != expectedRevision) {
                AuthoritativeRevisionResult.Stale
            } else {
                val content =
                    mutation()
                        ?: return@withLock AuthoritativeRevisionResult.Current(
                            null)
                val publication = nextPublication(content)
                publish(publication)
                AuthoritativeRevisionResult.Current(publication)
            }
        }

    suspend fun mutateAndPublish(
        mutation: suspend () -> LibraryContentState?,
        publish: suspend (AuthoritativeLibraryPublication) -> Unit,
    ): AuthoritativeRevisionResult<AuthoritativeLibraryPublication?> =
        mutex.withLock {
            val content =
                mutation()
                    ?: return@withLock AuthoritativeRevisionResult.Current(null)
            val publication = nextPublication(content)
            publish(publication)
            AuthoritativeRevisionResult.Current(publication)
        }

    private fun nextPublication(
        content: LibraryContentState
    ): AuthoritativeLibraryPublication =
        AuthoritativeLibraryPublication(content, ++revision).also {
            publications.value = it
        }

    suspend fun <T> withCurrentRevision(
        expectedRevision: Long,
        block: suspend () -> T,
    ): AuthoritativeRevisionResult<T> = mutex.withLock {
        if (revision != expectedRevision) {
            AuthoritativeRevisionResult.Stale
        } else {
            AuthoritativeRevisionResult.Current(block())
        }
    }
}

internal sealed interface AuthoritativeRevisionResult<out T> {
    data class Current<T>(val value: T) : AuthoritativeRevisionResult<T>

    data object Stale : AuthoritativeRevisionResult<Nothing>
}

internal data class InitialLibraryPublicationState(
    val content: LibraryContentState? = null,
    val errorMessage: String? = null,
    val isReady: Boolean = false,
) {
    val mutationsAllowed: Boolean
        get() = isReady

    fun complete(content: LibraryContentState): InitialLibraryPublicationState =
        copy(
            content = content,
            errorMessage = null,
            isReady = true,
        )

    fun failSafe(
        content: LibraryContentState,
        failure: Throwable
    ): InitialLibraryPublicationState =
        copy(
            content = content,
            errorMessage = failure.appFailureMessage(),
            isReady = true,
        )
}

internal suspend fun publishInitialLibraryContent(
    lifecycle: PlaybackSessionRestorer,
    reconciler: PlaybackSessionReconciler,
    content: LibraryContentState,
    updateState: suspend (InitialLibraryPublicationState) -> Unit,
) {
    val pending = InitialLibraryPublicationState()
    try {
        lifecycle.restoreOnce(content.tracks.map { it.toPlayableTrack() })
        reconciler.reconcile(content.tracks)
        updateState(pending.complete(content))
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Throwable) {
        updateState(pending.failSafe(content, failure))
    }
}

internal data class ScanPublicationState(
    val content: LibraryContentState,
    val progress: ScanProgress,
    val playlists: PlaylistStateAction? = null,
    val errorMessage: String? = null,
    val scanErrors: List<ScanError> = emptyList(),
)

internal data class LibraryMutationPublication(
    val content: LibraryContentState?,
    val playlists: PlaylistStateAction?,
    val errorMessage: String?,
    val scanErrors: List<ScanError>? = emptyList(),
)

internal suspend fun publishScanContentAfterReconcile(
    reconciler: PlaybackSessionReconciler,
    playlistStateOwner: PlaylistStateOwner,
    content: LibraryContentState,
    session: ScanSession,
    loadScanErrors: ((String) -> List<ScanError>)? = null,
    ownerIsActive: suspend () -> Boolean,
    publish: suspend (ScanPublicationState) -> Unit,
) {
    try {
        reconciler.reconcile(content.tracks)
    } catch (cancelled: CancellationException) {
        if (ownerIsActive()) {
            withContext(NonCancellable) {
                val playlists = runCatching {
                    playlistStateOwner.refresh().requireSuccessfulPublication()
                }
                    .getOrNull()
                val scanErrors = runCatching {
                    loadScanErrors?.invoke(session.id).orEmpty()
                }
                    .getOrDefault(emptyList())
                runCatching {
                    publish(
                        ScanPublicationState(
                            content = content,
                            progress =
                                ScanProgress(
                                    session.terminalAfterCancellation()),
                            playlists = playlists,
                            scanErrors = scanErrors,
                        ),
                    )
                }
            }
        }
        throw cancelled
    }
    val publication =
        ScanPublicationState(
            content = content, progress = ScanProgress(session))
    val playlists = playlistStateOwner.refresh().requireSuccessfulPublication()
    publish(
        publication.copy(
            playlists = playlists,
            scanErrors = loadScanErrors?.invoke(session.id).orEmpty(),
        ),
    )
}

internal fun loadLibraryContent(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
): LibraryContentState {
    val tracks = repository.tracks()
    return LibraryContentState(
        sources =
            repository.sources().map { source ->
                source.copy(accessStatus = platformAccess.accessStatus(source))
            },
        tracks = tracks,
        favoriteTrackIds = repository.favoriteTrackIds().toSet(),
        artworkTrackIds = repository.artworkTrackIds().toSet(),
        playHistory = repository.playHistory().toMap(),
        createdAtByTrackId =
            tracks.associate { track ->
                track.id to track.createdAtEpochMillis
            },
        sourceIdByTrackId =
            tracks.associate { track -> track.id to track.sourceId },
        modifiedAtByTrackId =
            tracks.associate { track ->
                track.id to track.modifiedAtEpochMillis
            },
    )
}

/**
 * Records an admitted playback event and publishes its fresh library snapshot
 * under publication ownership. A rejected write means the track was removed, so
 * it leaves the current publication unchanged without entering scan admission.
 */
internal suspend fun recordPlaybackHistoryAndPublish(
    event: PlaybackStarted,
    publicationOwner: AuthoritativeLibraryPublicationOwner,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    playedAtEpochMillis: Long,
    ioDispatcher: CoroutineDispatcher,
    publish: suspend (AuthoritativeLibraryPublication) -> Unit,
) {
    publicationOwner.mutateAndPublish(
        mutation = {
            withContext(ioDispatcher) {
                if (!repository.recordTrackPlayed(
                    event.trackId,
                    playedAtEpochMillis,
                )) {
                    null
                } else {
                    loadLibraryContent(repository, platformAccess)
                }
            }
        },
        publish = publish,
    )
}

/**
 * Keeps a controller's history stream alive after an individual persistence
 * failure, while preserving cancellation as lifecycle control flow.
 */
internal suspend fun collectPlaybackHistoryAndPublish(
    playbackStarted: Flow<PlaybackStarted>,
    publicationOwner: AuthoritativeLibraryPublicationOwner,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    playedAtEpochMillis: () -> Long,
    ioDispatcher: CoroutineDispatcher,
    reportFailure: suspend (String) -> Unit,
    publish: suspend (AuthoritativeLibraryPublication) -> Unit,
) {
    playbackStarted.collect { event ->
        try {
            recordPlaybackHistoryAndPublish(
                event = event,
                publicationOwner = publicationOwner,
                repository = repository,
                platformAccess = platformAccess,
                playedAtEpochMillis = playedAtEpochMillis(),
                ioDispatcher = ioDispatcher,
                publish = publish,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            reportFailure(failure.appFailureMessage())
        }
    }
}

/**
 * Applies a desired favorite state through the App-owned mutation coordinator
 * and republishes the combined library projection only while its revision is
 * still current.
 */
internal suspend fun setTrackFavoriteAndPublish(
    orchestrator: AppLibraryOrchestrator,
    publicationOwner: AuthoritativeLibraryPublicationOwner,
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    trackId: String,
    favorite: Boolean,
    expectedRevision: Long? = null,
    ioDispatcher: CoroutineDispatcher,
    publish: suspend (AuthoritativeLibraryPublication) -> Unit,
): Boolean {
    var published = false
    orchestrator.launch(LibraryOperationKind.SetTrackFavorite) { token ->
        orchestrator.publishIfCurrent(token) {
            currentCoroutineContext().ensureActive()
        } ?: return@launch
        val result =
            withContext(NonCancellable) {
                publicationOwner.mutateAndPublish(
                    mutation = {
                        withContext(ioDispatcher) {
                            if (!repository.setTrackFavorite(
                                trackId, favorite)) {
                                null
                            } else {
                                loadLibraryContent(repository, platformAccess)
                            }
                        }
                    },
                    publish = publish,
                )
            }
        published =
            result is AuthoritativeRevisionResult.Current &&
                result.value != null
    }
    return published
}

private fun ScanSession.terminalAfterCancellation(): ScanSession =
    if (status == ScanStatus.Scanning || status == ScanStatus.Cancelling) {
        copy(status = ScanStatus.Cancelled, terminalMessage = "Scan cancelled")
    } else {
        this
    }
