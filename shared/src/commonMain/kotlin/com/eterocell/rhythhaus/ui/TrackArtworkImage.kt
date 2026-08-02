package com.eterocell.rhythhaus.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.eterocell.rhythhaus.library.TrackArtwork
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalTrackArtworkLoader =
    staticCompositionLocalOf<suspend (String) -> TrackArtwork?> { { null } }

internal sealed interface TrackArtworkLoadState {
    data object Loading : TrackArtworkLoadState

    data class Available(val bytes: ByteArray) : TrackArtworkLoadState

    data object Unavailable : TrackArtworkLoadState
}

internal fun initialTrackArtworkLoadState(
    trackId: String?,
    eagerArtworkBytes: ByteArray?,
): TrackArtworkLoadState =
    when {
        eagerArtworkBytes != null ->
            TrackArtworkLoadState.Available(eagerArtworkBytes)
        trackId != null -> TrackArtworkLoadState.Loading
        else -> TrackArtworkLoadState.Unavailable
    }

internal suspend fun loadTrackArtworkState(
    trackId: String,
    artworkLoader: suspend (String) -> TrackArtwork?,
): TrackArtworkLoadState =
    try {
        artworkLoader(trackId)?.bytes?.let(TrackArtworkLoadState::Available)
            ?: TrackArtworkLoadState.Unavailable
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (_: Exception) {
        TrackArtworkLoadState.Unavailable
    }

@Composable
internal fun rememberLazyTrackArtworkState(
    trackId: String?,
    eagerArtworkBytes: ByteArray?,
): State<TrackArtworkLoadState> {
    val artworkLoader = LocalTrackArtworkLoader.current
    val state =
        remember(trackId, eagerArtworkBytes, artworkLoader) {
            mutableStateOf(
                initialTrackArtworkLoadState(trackId, eagerArtworkBytes))
        }
    LaunchedEffect(trackId, eagerArtworkBytes, artworkLoader) {
        if (trackId != null && eagerArtworkBytes == null) {
            state.value =
                withContext(Dispatchers.Default) {
                    loadTrackArtworkState(trackId, artworkLoader)
                }
        }
    }
    return state
}

@Composable
internal fun LazyTrackArtworkImage(
    trackId: String?,
    eagerArtworkBytes: ByteArray?,
    contentDescription: String,
    role: ArtworkImageRole,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
) {
    val artworkState =
        rememberLazyTrackArtworkState(
                trackId = trackId,
                eagerArtworkBytes = eagerArtworkBytes,
            )
            .value
    val artworkBytes = (artworkState as? TrackArtworkLoadState.Available)?.bytes
    ArtworkImage(
        artworkBytes = artworkBytes,
        contentDescription = contentDescription,
        role = role,
        modifier = modifier,
        contentScale = contentScale,
        fallback = fallback,
    )
}
