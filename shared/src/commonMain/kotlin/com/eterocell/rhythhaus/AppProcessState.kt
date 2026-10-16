package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.carplay.CarPlayCategoryLabels
import com.eterocell.rhythhaus.carplay.CarPlayFacade
import com.eterocell.rhythhaus.library.LibraryRepository
import com.eterocell.rhythhaus.library.PlatformSourceAccess
import com.eterocell.rhythhaus.library.currentTimeMillis
import com.eterocell.rhythhaus.library.toPlayableTrack
import com.eterocell.rhythhaus.library.ui.PlaylistSnapshot
import com.eterocell.rhythhaus.library.ui.PlaylistStateOwner
import com.eterocell.rhythhaus.session.PlaybackSessionReconciler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.carplay_albums
import rhythhaus.shared.generated.resources.carplay_artists
import rhythhaus.shared.generated.resources.carplay_songs
import rhythhaus.shared.generated.resources.playlists

/**
 * One initialization/history lifetime shared by phone composition and native
 * scenes.
 */
internal class AppProcessState(
    repository: LibraryRepository,
    platformAccess: PlatformSourceAccess,
    controller: PlaybackController,
    lifecycle: PlaybackProcessLifecycle,
    reconciler: PlaybackSessionReconciler,
    val publications: AuthoritativeLibraryPublicationOwner,
    playlistOwner: PlaylistStateOwner,
    facade: CarPlayFacade,
    scope: CoroutineScope,
    loadCategoryLabels: suspend () -> CarPlayCategoryLabels = {
        // iOS resource-environment resolution reads UIScreen traits.
        withContext(Dispatchers.Main.immediate) {
            CarPlayCategoryLabels(
                getString(Res.string.carplay_songs),
                getString(Res.string.carplay_albums),
                getString(Res.string.carplay_artists),
                getString(Res.string.playlists),
            )
        }
    },
) {
    val initial = MutableStateFlow(InitialLibraryPublicationState())
    val historyFailure = MutableStateFlow<String?>(null)

    init {
        scope.launch {
            publications.publications.filterNotNull().collect { publication ->
                val tracks =
                    publication.content.tracks.map { it.toPlayableTrack() }
                controller.refreshTrackMetadata(tracks)
                controller.refreshCurrentTrackMetadata(tracks)
            }
        }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            collectPlaybackHistoryAndPublish(
                playbackStarted = controller.playbackStarted,
                publicationOwner = publications,
                repository = repository,
                platformAccess = platformAccess,
                playedAtEpochMillis = ::currentTimeMillis,
                ioDispatcher = Dispatchers.Default,
                reportFailure = { historyFailure.value = it },
                publish = {},
            )
        }
        scope.launch {
            val labels = loadCategoryLabels()
            val unavailablePlaylists = PlaylistSnapshot()
            combine(
                    publications.publications.filterNotNull(),
                    playlistOwner.snapshots,
                    initial,
                ) { publication, playlists, state ->
                    Triple(publication, playlists, state)
                }
                .collect { (publication, playlists, state) ->
                    val content = publication.content
                    facade.publishCatalog(
                        publication.revision,
                        state.isReady,
                        content.tracks,
                        content.favoriteTrackIds,
                        content.playHistory,
                        playlists ?: unavailablePlaylists,
                        labels,
                        playlistsUnavailable = playlists == null,
                    )
                }
        }
        scope.launch {
            playlistOwner.refresh()
            val content = loadLibraryContent(repository, platformAccess)
            publishInitialLibraryContent(lifecycle, reconciler, content) { state
                ->
                state.content?.let { publications.publish(it) }
                initial.value = state
            }
        }
    }
}
