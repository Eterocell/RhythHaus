package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade

internal enum class ArtworkImageRole(val keySuffix: String) {
    Thumbnail("thumbnail"),
    Card("card"),
    Hero("hero"),
}

internal fun artworkMemoryCacheKey(bytes: ByteArray, role: ArtworkImageRole): String =
    "artwork:${role.keySuffix}:${bytes.contentHashCode()}:${bytes.size}"

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
