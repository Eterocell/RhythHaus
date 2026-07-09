package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.eterocell.rhythhaus.library.TrackArtwork
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class ArtworkImageRole(val keySuffix: String) {
    Thumbnail("thumbnail"),
    Card("card"),
    Hero("hero"),
}

internal fun artworkMemoryCacheKey(bytes: ByteArray, role: ArtworkImageRole): String =
    "artwork:${role.keySuffix}:${bytes.contentHashCode()}:${bytes.size}"

internal val LocalTrackArtworkLoader = staticCompositionLocalOf<suspend (String) -> TrackArtwork?> { { null } }

@Composable
internal fun rememberLazyTrackArtworkBytes(
    trackId: String?,
    eagerArtworkBytes: ByteArray?,
): State<ByteArray?> {
    val artworkLoader = LocalTrackArtworkLoader.current
    return produceState<ByteArray?>(initialValue = eagerArtworkBytes, trackId, eagerArtworkBytes, artworkLoader) {
        value = eagerArtworkBytes
        if (trackId != null && eagerArtworkBytes == null) {
            value = withContext(Dispatchers.Default) {
                artworkLoader(trackId)?.bytes
            }
        }
    }
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
    val artworkBytes = rememberLazyTrackArtworkBytes(
        trackId = trackId,
        eagerArtworkBytes = eagerArtworkBytes,
    ).value
    ArtworkImage(
        artworkBytes = artworkBytes,
        contentDescription = contentDescription,
        role = role,
        modifier = modifier,
        contentScale = contentScale,
        fallback = fallback,
    )
}

@Composable
internal fun ArtworkImage(
    artworkBytes: ByteArray?,
    contentDescription: String,
    role: ArtworkImageRole,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit,
) {
    if (artworkBytes == null) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }

    val cacheKey = artworkMemoryCacheKey(artworkBytes, role)
    val platformContext = LocalPlatformContext.current
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(platformContext)
            .data(artworkBytes)
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .crossfade(false)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        error = {
            Box(modifier = Modifier.matchParentSize(), contentAlignment = Alignment.Center) {
                fallback()
            }
        },
    )
}
