package com.eterocell.rhythhaus.library

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryModelsTest {
    @Test
    fun supportedAudioExtensionsAreCaseInsensitive() {
        assertTrue(isSupportedAudioName("Track.MP3"))
        assertTrue(isSupportedAudioName("mix.flac"))
        assertTrue(isSupportedAudioName("voice.m4a"))
        assertFalse(isSupportedAudioName("cover.jpg"))
        assertFalse(isSupportedAudioName("notes"))
    }
}
