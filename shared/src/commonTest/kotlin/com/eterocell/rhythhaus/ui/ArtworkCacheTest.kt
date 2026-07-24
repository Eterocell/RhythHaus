package com.eterocell.rhythhaus.ui

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals

class ArtworkCacheTest {
    private val bytes = byteArrayOf(1, 2, 3, 4)

    @AfterTest
    fun tearDown() {
        ArtworkCache.clear()
    }

    @Test
    fun fullSizeAndThumbnailEntriesUseDistinctCacheKeys() {
        assertNotEquals(
            artworkCacheKey(bytes), artworkCacheKey(bytes, maxPixelSize = 128))
    }

    @Test
    fun thumbnailSizeIsPartOfCacheKey() {
        assertNotEquals(
            artworkCacheKey(bytes, maxPixelSize = 64),
            artworkCacheKey(bytes, maxPixelSize = 128))
        assertNotEquals(
            artworkCacheKey(bytes, maxPixelSize = 64),
            artworkCacheKey(bytes, maxPixelSize = 96))
    }

    @Test
    fun cacheMissDoesNotCreateEntries() {
        assertFalse(ArtworkCache.contains(bytes))
        assertFalse(ArtworkCache.contains(bytes, maxPixelSize = 128))
        assertEquals(0, ArtworkCache.size())
    }

    @Test
    fun scaledThumbnailDimensionBoundsLargestDimensionForWideArtwork() {
        val (width, height) =
            scaledThumbnailDimension(width = 4000, height = 500, target = 128)

        assertEquals(128, width)
        assertEquals(16, height)
    }

    @Test
    fun scaledThumbnailDimensionBoundsLargestDimensionForTallArtwork() {
        val (width, height) =
            scaledThumbnailDimension(width = 500, height = 4000, target = 128)

        assertEquals(16, width)
        assertEquals(128, height)
    }
}
