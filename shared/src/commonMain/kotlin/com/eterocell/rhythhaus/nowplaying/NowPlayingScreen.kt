package com.eterocell.rhythhaus.nowplaying

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.eterocell.rhythhaus.PlaybackController
import com.eterocell.rhythhaus.PlaybackState
import com.eterocell.rhythhaus.SleepTimerState
import com.eterocell.rhythhaus.Track
import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.ui.LocalTrackArtworkLoader
import com.eterocell.rhythhaus.taglib.TagLibReader
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.album_artwork
import rhythhaus.shared.generated.resources.pause
import rhythhaus.shared.generated.resources.play
import rhythhaus.shared.generated.resources.track_artist_album_format

internal fun nowPlayingFavoriteAvailable(
    authoritativeTrackId: String?,
    playbackTrackId: String?,
): Boolean =
    authoritativeTrackId != null && authoritativeTrackId == playbackTrackId

/**
 * Shared composition facade delegating Now Playing presentation to the feature
 * module.
 */
@Composable
public fun NowPlayingScreen(
    track: Track,
    playbackState: PlaybackState,
    playbackController: PlaybackController,
    tagLibReader: TagLibReader,
    currentLibraryTrack: LibraryTrack?,
    onBack: () -> Unit,
    sleepTimerState: SleepTimerState,
    onArmSleepTimer: (minutes: Int, fadeEnabled: Boolean) -> Unit,
    onArmSleepTimerAfterCompletions: (count: Int, fadeEnabled: Boolean) -> Unit,
    onCancelSleepTimer: () -> Unit,
    modifier: Modifier = Modifier,
    favoriteTrackIds: Set<String> = emptySet(),
    onSetTrackFavorite: (String, Boolean) -> Unit = { _, _ -> },
): Unit {
    val artworkLoader = LocalTrackArtworkLoader.current
    NowPlayingContent(
        track = track,
        playbackState = playbackState,
        playbackController = playbackController,
        labels =
            NowPlayingScreenLabels(
                play = stringResource(Res.string.play),
                pause = stringResource(Res.string.pause),
                albumArtwork = stringResource(Res.string.album_artwork),
                currentTrackArtistAlbum =
                    stringResource(
                        Res.string.track_artist_album_format,
                        track.artist,
                        track.album),
            ),
        artworkLoader = { trackId -> artworkLoader(trackId)?.bytes },
        onBack = onBack,
        sleepTimerState = sleepTimerState,
        onArmSleepTimer = onArmSleepTimer,
        onArmSleepTimerAfterCompletions = onArmSleepTimerAfterCompletions,
        onCancelSleepTimer = onCancelSleepTimer,
        modifier = modifier,
        favoriteTrackIds = favoriteTrackIds,
        onSetTrackFavorite = onSetTrackFavorite,
        isCurrentTrackAvailableInLibrary =
            nowPlayingFavoriteAvailable(
                currentLibraryTrack?.id,
                playbackState.currentTrack?.id,
            ),
    )
}
