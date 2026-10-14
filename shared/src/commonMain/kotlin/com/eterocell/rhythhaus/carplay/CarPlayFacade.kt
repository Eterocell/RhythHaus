package com.eterocell.rhythhaus.carplay

import com.eterocell.rhythhaus.PlayableTrack
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.PlaybackStatus
import com.eterocell.rhythhaus.QueueOccurrence
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.TrackPlayHistory
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.projectSmartPlaylist
import com.eterocell.rhythhaus.library.selectDynamicOccurrenceForPlayback
import com.eterocell.rhythhaus.library.selectOccurrenceForPlayback
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.library.uuid4
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Identifies a localized CarPlay browsing destination. */
public enum class CarPlayCategoryId {
    /** All tracks in the current local library. */
    Songs,

    /** Unique albums and their ordered tracks. */
    Albums,

    /** Unique artists and their ordered tracks. */
    Artists,

    /** Saved and rule-based playlists. */
    Playlists,
}

/** Describes whether the authoritative local library has finished loading. */
public enum class CarPlayLibraryStatus {
    /** The first authoritative library publication has not completed. */
    Loading,

    /** The authoritative library contains no tracks. */
    Empty,

    /** The authoritative library contains tracks. */
    Ready,
}

/** Immutable category projection with a localized display title. */
public data class CarPlayLibraryCategory(
    /** Stable destination identifier. */
    public val id: CarPlayCategoryId,
    /** Localized category title. */
    public val title: String,
    /** Ordered top-level entries in this category. */
    public val entries: List<CarPlayLibraryEntry>,
)

/** Immutable list or detail entry in the Shared CarPlay catalog. */
public data class CarPlayLibraryEntry(
    /** Opaque stable entry identifier to return to [CarPlayFacade.selectEntry]. */
    public val id: String,
    /** User-visible entry title. */
    public val title: String,
    /** Optional secondary label. */
    public val subtitle: String? = null,
    /** Ordered child entries for album, artist, or playlist detail. */
    public val children: List<CarPlayLibraryEntry> = emptyList(),
    /** Whether this entry resolves to a currently playable track occurrence. */
    public val isPlayable: Boolean = false,
)

/** Current track metadata projected without exposing playback implementation types. */
public class CarPlayTrack
internal constructor(
    /** Stable opaque track identity used to bind transport commands. */
    public val id: String,
    /** Current track title. */
    public val title: String,
    /** Current track artist. */
    public val artist: String,
    /** Current track album. */
    public val album: String?,
    /** Track duration in milliseconds when known. */
    public val durationMillis: Long?,
    artworkBytes: ByteArray?,
) {
    private val artwork = artworkBytes?.copyOf()

    /** Returns a copy of the current embedded artwork bytes, when available. */
    public val artworkBytes: ByteArray?
        get() = artwork?.copyOf()

    public override fun equals(other: Any?): Boolean =
        this === other ||
            (other is CarPlayTrack &&
                id == other.id &&
                title == other.title &&
                artist == other.artist &&
                album == other.album &&
                durationMillis == other.durationMillis &&
                artwork.contentEqualsNullable(other.artwork))

    public override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + (album?.hashCode() ?: 0)
        result = 31 * result + (durationMillis?.hashCode() ?: 0)
        result = 31 * result + (artwork?.contentHashCode() ?: 0)
        return result
    }

    private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
        when {
            this == null -> other == null
            other == null -> false
            else -> contentEquals(other)
        }
}

/** Current playback status in the Shared playback session. */
public enum class CarPlayPlaybackStatus {
    /** No queue occurrence is selected. */
    Idle,

    /** The selected occurrence is loading. */
    Loading,

    /** The engine is waiting for playable media data. */
    Buffering,

    /** The current track is advancing. */
    Playing,

    /** The current track is loaded but paused. */
    Paused,

    /** The current track has stopped. */
    Stopped,

    /** The current track failed to load or play. */
    Error,
}

/** Immutable projection of the authoritative Shared playback state. */
public data class CarPlayPlaybackProjection(
    /** Current track metadata, or null when no track is selected. */
    public val currentTrack: CarPlayTrack?,
    /** Current playback lifecycle state. */
    public val status: CarPlayPlaybackStatus,
    /** Current playback position in milliseconds. */
    public val positionMillis: Long,
    /** Current media duration in milliseconds, when known. */
    public val durationMillis: Long?,
    /** Whether the current state accepts a play command. */
    public val canPlay: Boolean,
    /** Whether the current state accepts a pause command. */
    public val canPause: Boolean,
    /** Whether the current queue has another occurrence. */
    public val canSkipNext: Boolean,
    /** Whether the current queue has another occurrence. */
    public val canSkipPrevious: Boolean,
    /** Whether the current media can be sought. */
    public val canSeek: Boolean,
)

/** Immutable library and playback view published for CarPlay clients. */
public data class CarPlaySnapshot(
    /** Monotonically increasing authoritative catalog revision. */
    public val revision: Long,
    /** Loading or content state of the current local library. */
    public val libraryStatus: CarPlayLibraryStatus,
    /** Localized library destinations and ordered entries. */
    public val categories: List<CarPlayLibraryCategory>,
    /** Current projection of the existing Shared playback session. */
    public val playback: CarPlayPlaybackProjection,
)

/** Result reported for a CarPlay selection or transport action. */
public enum class CarPlayCommandResult {
    /** The existing Shared playback controller accepted the command. */
    Applied,

    /** The command referred to a catalog revision that is no longer current. */
    StaleSnapshot,

    /** The selected entry or one of its visible queue tracks no longer exists. */
    StaleEntry,

    /** The expected current playback track is no longer current. */
    StalePlayback,

    /** The action is not supported by the current authoritative state. */
    Unavailable,
}

/** Localized titles supplied by the Shared Compose application resources. */
internal data class CarPlayCategoryLabels(
    val songs: String,
    val albums: String,
    val artists: String,
    val playlists: String,
)

/**
 * Callback-first, Swift-exportable facade over Shared's current library and
 * playback session. One Koin process singleton owns snapshots and commands.
 */
public class CarPlayFacade
internal constructor(
    private val libraryRepository: LibraryRepository,
    private val playbackController: PlaybackController,
    private val processScope: CoroutineScope,
    private val canDispatchPlaybackCommands: () -> Boolean,
) {
    private val stateMutex = Mutex()
    private val observerJobs = MutableStateFlow<Map<String, Job>>(emptyMap())
    private var lastProjectedSourceTrack: PlayableTrack? = null
    private var lastProjectedTrack: CarPlayTrack? = null
    private val snapshotState =
        MutableStateFlow(
            CarPlaySnapshot(
                revision = 0L,
                libraryStatus = CarPlayLibraryStatus.Loading,
                categories = emptyList(),
                playback = projectPlayback(playbackController.state.value),
            ),
        )

    private var currentCatalog =
        CarPlayCatalogProjection(
            status = CarPlayLibraryStatus.Loading,
            categories = emptyList(),
            selectionContexts = emptyMap(),
        )
    private var currentLibraryRevision: Long? = null
    private var revision = 0L

    init {
        processScope.launch {
            playbackController.state.collect { playback ->
                publishPlayback(playback)
            }
        }
    }

    /** Delivers the latest immutable snapshot synchronously to [onSnapshot]. */
    public fun readSnapshot(onSnapshot: (CarPlaySnapshot) -> Unit) {
        onSnapshot(snapshotState.value)
    }

    /**
     * Observes current and future snapshots. The returned token must be passed
     * to [removeSnapshotObserver] when the client no longer needs updates.
     */
    public fun observeSnapshots(
        onSnapshot: (CarPlaySnapshot) -> Unit,
    ): String {
        val observerId = uuid4()
        val job =
            processScope.launch(start = CoroutineStart.LAZY) {
                snapshotState.collect(onSnapshot)
            }
        observerJobs.update { it + (observerId to job) }
        job.start()
        return observerId
    }

    /** Stops snapshot delivery for the observer identified by [observerId]. */
    public fun removeSnapshotObserver(observerId: String) {
        val job = observerJobs.value[observerId] ?: return
        observerJobs.update { it - observerId }
        job.cancel()
    }

    /**
     * Resolves [entryId] against the latest catalog and selects its exact
     * visible queue through the existing Shared playback helpers.
     */
    public fun selectEntry(
        entryId: String,
        expectedRevision: Long,
        onResult: (CarPlayCommandResult) -> Unit,
    ) {
        processScope.launch {
            val result = stateMutex.withLock {
                selectEntryLocked(entryId, expectedRevision)
            }
            onResult(result)
        }
    }

    /** Routes play through the existing Shared playback controller. */
    public fun play(
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
    ) = dispatchTransport(expectedRevision, expectedTrackId, onResult) {
        if (!it.canPlay) return@dispatchTransport CarPlayCommandResult.Unavailable
        playbackController.play()
        CarPlayCommandResult.Applied
    }

    /** Routes pause through the existing Shared playback controller. */
    public fun pause(
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
    ) = dispatchTransport(expectedRevision, expectedTrackId, onResult) {
        if (it.status != PlaybackStatus.Playing)
            return@dispatchTransport CarPlayCommandResult.Unavailable
        playbackController.pause()
        CarPlayCommandResult.Applied
    }

    /** Routes next-track navigation through the existing Shared queue. */
    public fun skipToNext(
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
    ) = dispatchTransport(expectedRevision, expectedTrackId, onResult) { state ->
        if (state.queue.size < 2) return@dispatchTransport CarPlayCommandResult.Unavailable
        playbackController.skipToNext()
        if (playbackController.state.value == state)
            CarPlayCommandResult.Unavailable
        else CarPlayCommandResult.Applied
    }

    /** Routes previous-track navigation through the existing Shared queue. */
    public fun skipToPrevious(
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
    ) = dispatchTransport(expectedRevision, expectedTrackId, onResult) { state ->
        if (state.queue.size < 2) return@dispatchTransport CarPlayCommandResult.Unavailable
        playbackController.skipToPrevious()
        if (playbackController.state.value == state)
            CarPlayCommandResult.Unavailable
        else CarPlayCommandResult.Applied
    }

    /** Routes seeking through the existing Shared playback controller. */
    public fun seekTo(
        positionMillis: Long,
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
    ) = dispatchTransport(expectedRevision, expectedTrackId, onResult) { state ->
        if (state.durationMillis == null ||
            (state.status != PlaybackStatus.Playing &&
                state.status != PlaybackStatus.Paused &&
                state.status != PlaybackStatus.Buffering)) {
            return@dispatchTransport CarPlayCommandResult.Unavailable
        }
        playbackController.seekTo(positionMillis)
        CarPlayCommandResult.Applied
    }

    /** Publishes one authoritative Shared catalog into the process owner. */
    internal suspend fun publishCatalog(
        libraryRevision: Long,
        isReady: Boolean,
        tracks: List<LibraryTrack>,
        favoriteTrackIds: Set<String>,
        playHistory: Map<String, TrackPlayHistory>,
        playlists: PlaylistSnapshot,
        labels: CarPlayCategoryLabels,
    ) {
        val projection =
            projectCatalog(
                isReady = isReady,
                tracks = tracks,
                favoriteTrackIds = favoriteTrackIds,
                playHistory = playHistory,
                playlists = playlists,
                labels = labels,
            )
        stateMutex.withLock {
            val appliedRevision = currentLibraryRevision
            if (appliedRevision != null && libraryRevision < appliedRevision)
                return@withLock
            if (libraryRevision != currentLibraryRevision ||
                projection != currentCatalog) {
                revision += 1L
            }
            currentLibraryRevision = libraryRevision
            currentCatalog = projection
            snapshotState.value =
                CarPlaySnapshot(
                    revision = revision,
                    libraryStatus = projection.status,
                    categories = projection.categories,
                    playback = projectPlayback(playbackController.state.value),
                )
        }
    }

    private suspend fun publishPlayback(playback: PlaybackState) {
        stateMutex.withLock {
            val previous = snapshotState.value
            val projected = projectPlayback(playback)
            if (previous.playback != projected) {
                snapshotState.value = previous.copy(playback = projected)
            }
        }
    }

    private suspend fun selectEntryLocked(
        entryId: String,
        expectedRevision: Long,
    ): CarPlayCommandResult {
        if (expectedRevision != snapshotState.value.revision)
            return CarPlayCommandResult.StaleSnapshot
        val currentEntry =
            currentCatalog.categories
                .asSequence()
                .flatMap { it.entries.asSequence() }
                .flatMap { sequenceOf(it) + it.children.asSequence() }
                .firstOrNull { it.id == entryId }
                ?: return CarPlayCommandResult.StaleEntry
        val selection = currentCatalog.selectionContexts[entryId]
            ?: return CarPlayCommandResult.Unavailable
        if (!currentEntry.isPlayable) return CarPlayCommandResult.Unavailable

        val tracksById = libraryRepository.tracks().associateBy(LibraryTrack::id)
        val availableSourceIds =
            libraryRepository
                .sources()
                .filter { it.accessStatus == LibrarySourceAccessStatus.Available }
                .mapTo(HashSet()) { it.id }
        val occurrences =
            selection.queue.map { item ->
                val track = tracksById[item.trackId]
                    ?: return CarPlayCommandResult.StaleEntry
                if (track.sourceId !in availableSourceIds)
                    return CarPlayCommandResult.Unavailable
                QueueOccurrence(item.occurrenceId, track.toPlayableTrack())
            }
        if (occurrences.none { it.id == selection.selectedOccurrenceId })
            return CarPlayCommandResult.StaleEntry

        val before = playbackController.state.value
        if (selection.dynamic) {
            selectDynamicOccurrenceForPlayback(
                playbackController,
                occurrences,
                selection.selectedOccurrenceId,
            )
        } else {
            selectOccurrenceForPlayback(
                playbackController,
                occurrences,
                selection.selectedOccurrenceId,
            )
        }
        return if (playbackController.state.value == before)
            CarPlayCommandResult.Unavailable
        else CarPlayCommandResult.Applied
    }

    private fun dispatchTransport(
        expectedRevision: Long,
        expectedTrackId: String,
        onResult: (CarPlayCommandResult) -> Unit,
        dispatch: (PlaybackState) -> CarPlayCommandResult,
    ) {
        processScope.launch {
            val result = stateMutex.withLock {
                if (expectedRevision != snapshotState.value.revision)
                    return@withLock CarPlayCommandResult.StaleSnapshot
                val state = playbackController.state.value
                if (state.currentTrack?.id != expectedTrackId)
                    return@withLock CarPlayCommandResult.StalePlayback
                if (libraryRepository.tracks().none { it.id == expectedTrackId })
                    return@withLock CarPlayCommandResult.StalePlayback
                if (!canDispatchPlaybackCommands())
                    return@withLock CarPlayCommandResult.Unavailable
                dispatch(state)
            }
            onResult(result)
        }
    }

    private fun projectCatalog(
        isReady: Boolean,
        tracks: List<LibraryTrack>,
        favoriteTrackIds: Set<String>,
        playHistory: Map<String, TrackPlayHistory>,
        playlists: PlaylistSnapshot,
        labels: CarPlayCategoryLabels,
    ): CarPlayCatalogProjection {
        val libraryStatus =
            when {
                !isReady -> CarPlayLibraryStatus.Loading
                tracks.isEmpty() -> CarPlayLibraryStatus.Empty
                else -> CarPlayLibraryStatus.Ready
            }
        val sourceAvailability =
            libraryRepository
                .sources()
                .associate { it.id to it.accessStatus }
        val tracksById = tracks.associateBy(LibraryTrack::id)
        val selectionContexts = linkedMapOf<String, CarPlaySelectionContext>()

        fun isAvailable(track: LibraryTrack): Boolean =
            sourceAvailability[track.sourceId] ==
                LibrarySourceAccessStatus.Available

        fun makeTrackEntry(
            entryId: String,
            track: LibraryTrack,
            queue: List<CarPlayQueueItem>,
            selectedOccurrenceId: String,
            dynamic: Boolean = false,
        ): CarPlayLibraryEntry {
            val available = isAvailable(track)
            if (available) {
                selectionContexts[entryId] =
                    CarPlaySelectionContext(queue, selectedOccurrenceId, dynamic)
            }
            return CarPlayLibraryEntry(
                id = entryId,
                title = track.title,
                subtitle = trackSubtitle(track),
                isPlayable = available,
            )
        }

        fun groupedEntries(
            category: CarPlayCategoryId,
            groups: List<Pair<String, List<LibraryTrack>>>,
        ): List<CarPlayLibraryEntry> =
            groups.map { (groupId, groupTracks) ->
                val queue =
                    groupTracks.filter(::isAvailable).map { track ->
                        CarPlayQueueItem(
                            occurrenceId = trackEntryId(category, groupId, track.id),
                            trackId = track.id,
                        )
                    }
                val children =
                    groupTracks.map { track ->
                        val occurrenceId =
                            trackEntryId(category, groupId, track.id)
                        makeTrackEntry(
                            occurrenceId,
                            track,
                            queue,
                            occurrenceId,
                        )
                    }
                CarPlayLibraryEntry(
                    id = groupId,
                    title =
                        when (category) {
                            CarPlayCategoryId.Albums -> groupTracks.first().album
                            CarPlayCategoryId.Artists -> groupTracks.first().artist
                            else -> error("Unexpected grouped CarPlay category: $category")
                        },
                    subtitle =
                        if (category == CarPlayCategoryId.Albums)
                            groupTracks.first().artist.ifBlank { null }
                        else null,
                    children = children,
                )
            }

        val songQueue = tracks.filter(::isAvailable).map { track ->
            CarPlayQueueItem(songEntryId(track.id), track.id)
        }
        val songs =
            tracks.map { track ->
                val occurrenceId = songEntryId(track.id)
                makeTrackEntry(
                    occurrenceId,
                    track,
                    songQueue,
                    occurrenceId,
                )
            }
        val albums =
            tracks
                .groupBy { it.artist to it.album }
                .entries
                .sortedWith(
                    compareBy<Map.Entry<Pair<String, String>, List<LibraryTrack>>> {
                        it.key.first.lowercase()
                    }
                        .thenBy { it.key.first }
                        .thenBy { it.key.second.lowercase() }
                        .thenBy { it.key.second },
                )
                .map { (key, groupedTracks) ->
                    "album:${key.first.length}:${key.first}:${key.second.length}:${key.second}" to
                        groupedTracks
                }
                .let { groupedEntries(CarPlayCategoryId.Albums, it) }
        val artists =
            tracks
                .groupBy(LibraryTrack::artist)
                .entries
                .sortedWith(
                    compareBy<Map.Entry<String, List<LibraryTrack>>> {
                        it.key.lowercase()
                    }.thenBy { it.key },
                )
                .map { (artist, groupedTracks) ->
                    "artist:${artist.length}:$artist" to groupedTracks
                }
                .let { groupedEntries(CarPlayCategoryId.Artists, it) }

        val playlistEntries =
            playlists.playlists.map { playlist ->
                val visibleEntries =
                    playlists.entries(playlist.id).mapNotNull { entry ->
                        tracksById[entry.trackId]?.let { entry to it }
                    }
                val queue =
                    visibleEntries
                        .filter { (_, track) -> isAvailable(track) }
                        .map { (entry, track) ->
                            CarPlayQueueItem(
                                occurrenceId =
                                    playlistEntryId(playlist.id, entry.id),
                                trackId = track.id,
                            )
                        }
                val children =
                    visibleEntries.map { (entry, track) ->
                        val occurrenceId = playlistEntryId(playlist.id, entry.id)
                        makeTrackEntry(
                            occurrenceId,
                            track,
                            queue,
                            occurrenceId,
                        )
                    }
                CarPlayLibraryEntry(
                    id = "playlist:${playlist.id.length}:${playlist.id}",
                    title = playlist.name,
                    children = children,
                )
            }
        val smartPlaylistEntries =
            playlists.smartPlaylists.map { smartPlaylist ->
                val projected =
                    projectSmartPlaylist(
                        smartPlaylist,
                        tracks,
                        favoriteTrackIds,
                        playHistory,
                        playlists,
                    )
                val visibleRows = projected.rows.mapNotNull { row ->
                    tracksById[row.trackId]?.let { row to it }
                }
                val queue =
                    visibleRows
                        .filter { (_, track) -> isAvailable(track) }
                        .map { (row, track) ->
                            CarPlayQueueItem(
                                occurrenceId =
                                    smartPlaylistEntryId(
                                        smartPlaylist.id,
                                        row.occurrenceId,
                                    ),
                                trackId = track.id,
                            )
                        }
                val children =
                    visibleRows.map { (row, track) ->
                        val occurrenceId =
                            smartPlaylistEntryId(smartPlaylist.id, row.occurrenceId)
                        makeTrackEntry(
                            occurrenceId,
                            track,
                            queue,
                            occurrenceId,
                            dynamic = true,
                        )
                    }
                CarPlayLibraryEntry(
                    id = "smart-playlist:${smartPlaylist.id.length}:${smartPlaylist.id}",
                    title = smartPlaylist.name,
                    children = children,
                )
            }

        return CarPlayCatalogProjection(
            status = libraryStatus,
            categories =
                listOf(
                    CarPlayLibraryCategory(
                        CarPlayCategoryId.Songs, labels.songs, songs),
                    CarPlayLibraryCategory(
                        CarPlayCategoryId.Albums, labels.albums, albums),
                    CarPlayLibraryCategory(
                        CarPlayCategoryId.Artists, labels.artists, artists),
                    CarPlayLibraryCategory(
                        CarPlayCategoryId.Playlists,
                        labels.playlists,
                        playlistEntries + smartPlaylistEntries,
                    ),
                ),
            selectionContexts = selectionContexts,
        )
    }

    private fun projectPlayback(state: PlaybackState): CarPlayPlaybackProjection {
        val sourceTrack = state.currentTrack
        val projectedTrack =
            if (sourceTrack === lastProjectedSourceTrack) {
                lastProjectedTrack
            } else {
                sourceTrack?.let {
                    CarPlayTrack(
                        id = it.id,
                        title = it.title,
                        artist = it.artist,
                        album = it.album,
                        durationMillis = it.durationMillis,
                        artworkBytes = it.artworkBytes,
                    )
                }
            }
        lastProjectedSourceTrack = sourceTrack
        lastProjectedTrack = projectedTrack
        val status =
            when (state.status) {
                PlaybackStatus.Idle -> CarPlayPlaybackStatus.Idle
                PlaybackStatus.Loading -> CarPlayPlaybackStatus.Loading
                PlaybackStatus.Buffering -> CarPlayPlaybackStatus.Buffering
                PlaybackStatus.Playing -> CarPlayPlaybackStatus.Playing
                PlaybackStatus.Paused -> CarPlayPlaybackStatus.Paused
                PlaybackStatus.Stopped -> CarPlayPlaybackStatus.Stopped
                PlaybackStatus.Error -> CarPlayPlaybackStatus.Error
            }
        val duration = state.durationMillis ?: projectedTrack?.durationMillis
        return CarPlayPlaybackProjection(
            currentTrack = projectedTrack,
            status = status,
            positionMillis = state.positionMillis,
            durationMillis = duration,
            canPlay = state.canPlay,
            canPause = state.status == PlaybackStatus.Playing,
            canSkipNext = state.currentOccurrence != null && state.queue.size > 1,
            canSkipPrevious = state.currentOccurrence != null && state.queue.size > 1,
            canSeek =
                projectedTrack != null && duration != null && duration > 0L &&
                    (state.status == PlaybackStatus.Playing ||
                        state.status == PlaybackStatus.Paused ||
                        state.status == PlaybackStatus.Buffering),
        )
    }

    private fun trackSubtitle(track: LibraryTrack): String? =
        listOf(track.artist, track.album)
            .filter(String::isNotBlank)
            .joinToString(" · ")
            .ifBlank { null }

    private fun songEntryId(trackId: String): String =
        "song:${trackId.length}:$trackId"

    private fun trackEntryId(
        category: CarPlayCategoryId,
        groupId: String,
        trackId: String,
    ): String =
        "${category.name.lowercase()}:${groupId.length}:$groupId:${trackId.length}:$trackId"

    private fun playlistEntryId(playlistId: String, entryId: String): String =
        "playlist-entry:${playlistId.length}:$playlistId:${entryId.length}:$entryId"

    private fun smartPlaylistEntryId(playlistId: String, occurrenceId: String): String =
        "smart-entry:${playlistId.length}:$playlistId:${occurrenceId.length}:$occurrenceId"

    private data class CarPlayCatalogProjection(
        val status: CarPlayLibraryStatus,
        val categories: List<CarPlayLibraryCategory>,
        val selectionContexts: Map<String, CarPlaySelectionContext>,
    )

    private data class CarPlaySelectionContext(
        val queue: List<CarPlayQueueItem>,
        val selectedOccurrenceId: String,
        val dynamic: Boolean,
    )

    private data class CarPlayQueueItem(
        val occurrenceId: String,
        val trackId: String,
    )
}
