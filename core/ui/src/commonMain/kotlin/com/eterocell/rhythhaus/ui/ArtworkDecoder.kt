package com.eterocell.rhythhaus.ui

import androidx.compose.ui.graphics.ImageBitmap

/** Decodes full-size artwork bytes for the current platform. */
public expect fun ByteArray.decodeArtwork(): ImageBitmap?

/** Decodes artwork bytes constrained to the requested maximum pixel size. */
public expect fun ByteArray.decodeArtworkThumbnail(
    maxPixelSize: Int
): ImageBitmap?

/** In-memory cache for decoded artwork images. */
public object ArtworkCache {
    private val cache = HashMap<ArtworkCacheKey, ImageBitmap>(64)

    /** Reads a cached image for these bytes and optional size bucket. */
    public fun get(bytes: ByteArray, maxPixelSize: Int? = null): ImageBitmap? =
        cache[artworkCacheKey(bytes, maxPixelSize)]

    /** Stores an image for these bytes and optional size bucket. */
    public fun put(
        bytes: ByteArray,
        image: ImageBitmap,
        maxPixelSize: Int? = null
    ): Unit {
        cache[artworkCacheKey(bytes, maxPixelSize)] = image
    }

    /** Clears all decoded artwork. */
    public fun clear(): Unit = cache.clear()

    /** Returns the number of cached image entries. */
    public fun size(): Int = cache.size

    internal fun contains(
        bytes: ByteArray,
        maxPixelSize: Int? = null
    ): Boolean = artworkCacheKey(bytes, maxPixelSize) in cache
}

internal data class ArtworkCacheKey(
    val contentHash: Int,
    val maxPixelSize: Int?
)

internal fun artworkCacheKey(
    bytes: ByteArray,
    maxPixelSize: Int? = null
): ArtworkCacheKey = ArtworkCacheKey(bytes.contentHashCode(), maxPixelSize)

/**
 * Decodes full-size artwork while preserving the existing memory-cache
 * behavior.
 */
public fun ByteArray.decodeArtworkCached(): ImageBitmap? =
    ArtworkCache.get(this)
        ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }

/**
 * Decodes thumbnail artwork while preserving the existing memory-cache
 * behavior.
 */
public fun ByteArray.decodeArtworkThumbnailCached(
    maxPixelSize: Int = 128
): ImageBitmap? =
    ArtworkCache.get(this, maxPixelSize)
        ?: decodeArtworkThumbnail(maxPixelSize)?.also {
            ArtworkCache.put(this, it, maxPixelSize)
        }

internal fun scaledThumbnailDimension(
    width: Int,
    height: Int,
    target: Int
): Pair<Int, Int> {
    val safeTarget = target.coerceAtLeast(1)
    val safeWidth = width.coerceAtLeast(1)
    val safeHeight = height.coerceAtLeast(1)
    val largestDimension = maxOf(safeWidth, safeHeight)
    if (largestDimension <= safeTarget) return safeWidth to safeHeight
    val scale = safeTarget.toFloat() / largestDimension
    return (safeWidth * scale).toInt().coerceAtLeast(1) to
        (safeHeight * scale).toInt().coerceAtLeast(1)
}
