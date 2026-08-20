package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.ArtworkImage
import com.eterocell.rhythhaus.ui.ArtworkImageRole
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shape used for playlist track artwork thumbnails. */
internal val PlaylistArtworkThumbnailShape: RoundedCornerShape =
    RoundedCornerShape(14.dp)

/** Modifier clipping playlist track artwork thumbnails to rounded corners. */
@Composable
internal fun playlistArtworkThumbnailModifier(): Modifier =
    Modifier.size(48.dp)
        .clip(PlaylistArtworkThumbnailShape)
        .background(
            HausColors.current.panelStrong, PlaylistArtworkThumbnailShape)

/**
 * Renders a track's artwork, resolving bytes lazily by track ID when the track
 * does not carry embedded bytes.
 */
@Composable
internal fun PlaylistLazyArtworkImage(
    trackId: String?,
    eagerArtworkBytes: ByteArray?,
    contentDescription: String,
    role: ArtworkImageRole,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    artworkLoader: suspend (String) -> ByteArray?,
    fallback: @Composable () -> Unit,
) {
    var resolvedBytes by
        remember(trackId, eagerArtworkBytes, artworkLoader) {
            mutableStateOf(eagerArtworkBytes)
        }
    LaunchedEffect(trackId, eagerArtworkBytes, artworkLoader) {
        if (trackId != null && resolvedBytes == null) {
            resolvedBytes =
                withContext(Dispatchers.Default) {
                    try {
                        artworkLoader(trackId)
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Exception) {
                        null
                    }
                }
        }
    }
    ArtworkImage(
        artworkBytes = resolvedBytes,
        contentDescription = contentDescription,
        role = role,
        modifier = modifier,
        contentScale = contentScale,
        fallback = fallback,
    )
}
