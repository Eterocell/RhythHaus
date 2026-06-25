package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?

/**
 * Thread-safe in-memory cache for decoded artwork images.
 * Keyed by ByteArray content hash — collisions (~1 in 4B) are ignored.
 */
object ArtworkCache {
    private val cache = HashMap<Int, ImageBitmap>(64)

    @Synchronized
    fun get(bytes: ByteArray): ImageBitmap? = cache[bytes.contentHashCode()]

    @Synchronized
    fun put(bytes: ByteArray, image: ImageBitmap) {
        cache[bytes.contentHashCode()] = image
    }

    @Synchronized
    fun clear() = cache.clear()

    @Synchronized
    fun size(): Int = cache.size
}

/**
 * Decodes artwork with caching — first checks ArtworkCache, falls back to platform decode.
 */
fun ByteArray.decodeArtworkCached(): ImageBitmap? =
    ArtworkCache.get(this) ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }
