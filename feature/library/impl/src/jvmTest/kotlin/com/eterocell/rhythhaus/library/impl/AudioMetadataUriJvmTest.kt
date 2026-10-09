package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AudioMetadataUriJvmTest {
    @Test
    fun legacyConstructorRetainsPlatformFallbackForUrisWithoutDescriptorSupport() {
        val uri = AudioSource.Uri("file:///music/song.flac")
        var fallbackCount = 0
        val reader =
            AudioMetadataReader(
                tagLibReader =
                    object : TagLibReader {
                        override fun readPath(path: String): TagReadResult =
                            error(
                                "JVM URI support must not reinterpret a URI as a path")

                        override fun readFd(
                            fd: Int,
                            displayName: String
                        ): TagReadResult =
                            error("JVM URI support has no descriptor to read")

                        override fun readProperties(
                            path: String
                        ): Map<String, String> =
                            error(
                                "URI fallback must not reopen media for properties")
                    },
                platformMetadataReader = { source ->
                    assertEquals(uri, source)
                    fallbackCount++
                    AudioMetadata(
                        title = "Platform URI title", durationMillis = 123_000L)
                },
            )

        val metadata = assertNotNull(reader.read(uri))

        assertEquals("Platform URI title", metadata.title)
        assertEquals(123_000L, metadata.durationMillis)
        assertEquals(1, fallbackCount)
    }
}
