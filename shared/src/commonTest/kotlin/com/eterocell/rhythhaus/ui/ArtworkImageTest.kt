package com.eterocell.rhythhaus.ui

import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArtworkImageTest {
    @Test
    fun artworkMemoryCacheKeyIncludesRoleAndSize() {
        val bytes = byteArrayOf(1, 2, 3, 4)

        val thumbnailKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Thumbnail)
        val heroKey = artworkMemoryCacheKey(bytes, ArtworkImageRole.Hero)

        assertNotEquals(thumbnailKey, heroKey)
        assertTrue(thumbnailKey.contains("thumbnail"))
        assertTrue(heroKey.contains("hero"))
        assertTrue(thumbnailKey.endsWith(":4"))
    }
}
