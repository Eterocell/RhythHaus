package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?

expect fun ByteArray.decodeArtworkThumbnail(maxPixelSize: Int): ImageBitmap?

/**
 * In-memory cache for decoded artwork images. Keyed by ByteArray content hash plus size bucket.
 * A null size bucket is the original/full-size decode; non-null buckets are thumbnails.
 * All access is from Compose @Composable functions on the main thread — no synchronization needed.
 */
object ArtworkCache {
    private val cache = HashMap<ArtworkCacheKey, ImageBitmap>(64)

    fun get(bytes: ByteArray, maxPixelSize: Int? = null): ImageBitmap? = cache[artworkCacheKey(bytes, maxPixelSize)]

    fun put(bytes: ByteArray, image: ImageBitmap, maxPixelSize: Int? = null) {
        cache[artworkCacheKey(bytes, maxPixelSize)] = image
    }

    fun clear() = cache.clear()

    fun size(): Int = cache.size

    internal fun contains(bytes: ByteArray, maxPixelSize: Int? = null): Boolean = artworkCacheKey(bytes, maxPixelSize) in cache
}

internal data class ArtworkCacheKey(
    val contentHash: Int,
    val maxPixelSize: Int?,
)

internal fun artworkCacheKey(bytes: ByteArray, maxPixelSize: Int? = null): ArtworkCacheKey = ArtworkCacheKey(
    contentHash = bytes.contentHashCode(),
    maxPixelSize = maxPixelSize,
)

/**
 * Decodes full-size artwork with caching — first checks ArtworkCache, falls back to platform decode.
 */
fun ByteArray.decodeArtworkCached(): ImageBitmap? = ArtworkCache.get(this) ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }

/**
 * Decodes a thumbnail-sized artwork image with caching for compact list/bar surfaces.
 */
fun ByteArray.decodeArtworkThumbnailCached(maxPixelSize: Int = 128): ImageBitmap? =
    ArtworkCache.get(this, maxPixelSize) ?: decodeArtworkThumbnail(maxPixelSize)?.also { ArtworkCache.put(this, it, maxPixelSize) }

internal fun scaledThumbnailDimension(width: Int, height: Int, target: Int): Pair<Int, Int> {
    val safeTarget = target.coerceAtLeast(1)
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val largestDimension = maxOf(safeWidth, safeHeight)
    if (largestDimension <= safeTarget) return safeWidth to safeHeight
    val scale = safeTarget.toFloat() / largestDimension
    return (safeWidth * scale).toInt().coerceAtLeast(1) to (safeHeight * scale).toInt().coerceAtLeast(1)
}
