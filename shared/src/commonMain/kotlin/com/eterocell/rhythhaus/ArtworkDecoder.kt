package com.eterocell.rhythhaus

import androidx.compose.ui.graphics.ImageBitmap

expect fun ByteArray.decodeArtwork(): ImageBitmap?

/**
 * In-memory cache for decoded artwork images. Keyed by ByteArray content hash.
 * All access is from Compose @Composable functions on the main thread — no synchronization needed.
 */
object ArtworkCache {
    private val cache = HashMap<Int, ImageBitmap>(64)

    fun get(bytes: ByteArray): ImageBitmap? = cache[bytes.contentHashCode()]

    fun put(bytes: ByteArray, image: ImageBitmap) {
        cache[bytes.contentHashCode()] = image
    }

    fun clear() = cache.clear()

    fun size(): Int = cache.size
}

/**
 * Decodes artwork with caching — first checks ArtworkCache, falls back to platform decode.
 */
fun ByteArray.decodeArtworkCached(): ImageBitmap? =
    ArtworkCache.get(this) ?: decodeArtwork()?.also { ArtworkCache.put(this, it) }
