package com.eterocell.rhythhaus

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals

class ArtworkCacheTest {
    private val bytes = byteArrayOf(1, 2, 3, 4)

    @AfterTest
    fun tearDown() {
        ArtworkCache.clear()
    }

    @Test
    fun fullSizeAndThumbnailEntriesUseDistinctCacheKeys() {
        assertNotEquals(artworkCacheKey(bytes), artworkCacheKey(bytes, maxPixelSize = 128))
    }

    @Test
    fun thumbnailSizeIsPartOfCacheKey() {
        assertNotEquals(artworkCacheKey(bytes, maxPixelSize = 64), artworkCacheKey(bytes, maxPixelSize = 128))
        assertNotEquals(artworkCacheKey(bytes, maxPixelSize = 64), artworkCacheKey(bytes, maxPixelSize = 96))
    }
}
