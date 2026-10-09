package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.taglib.EmbeddedArtwork
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AudioMetadataUriTest {
    @Test
    fun uriMetadataReadsAndParsesReplayGainThroughItsOwnedDescriptor() {
        val descriptor = FixtureUriDescriptor()
        val reader =
            AudioMetadataReader(
                tagLibReader =
                    DescriptorTagLibReader {
                        descriptor.assertOpen()
                        TagReadResult.Found(completeTags())
                    },
                platformMetadataReader = {
                    error("Complete URI metadata must not reopen the media")
                },
                uriDescriptorOpener = descriptor::open,
            )

        val metadata = assertNotNull(reader.read(uri))

        assertEquals("Native Title", metadata.title)
        assertEquals("Native Artist", metadata.artist)
        assertEquals("Native Album", metadata.album)
        assertEquals(123_000L, metadata.durationMillis)
        assertEquals(3, metadata.trackNumber)
        assertEquals(2, metadata.discNumber)
        assertEquals("image/png", metadata.artworkMimeType)
        assertTrue(byteArrayOf(1, 2).contentEquals(metadata.artworkBytes))
        assertEquals(-6.0, metadata.replayGainTrackGainDb)
        assertEquals(1.2, metadata.replayGainTrackPeak)
        assertEquals(-4.0, metadata.replayGainAlbumGainDb)
        assertEquals(1.1, metadata.replayGainAlbumPeak)
        assertEquals(1, descriptor.closeCount)
    }

    @Test
    fun uriFallbackEnrichesDisplayMetadataAfterClosingTheDescriptor() {
        val descriptor = FixtureUriDescriptor()
        val reader =
            AudioMetadataReader(
                tagLibReader =
                    DescriptorTagLibReader {
                        descriptor.assertOpen()
                        TagReadResult.Found(
                            TagMetadata(
                                title = "Native Title",
                                replayGainTrackGain = "-6 dB",
                                replayGainTrackPeak = "1.2",
                                replayGainAlbumGain = "NaN dB",
                                replayGainAlbumPeak = "1.1",
                            ),
                        )
                    },
                platformMetadataReader = { source ->
                    assertEquals(uri, source)
                    assertEquals(1, descriptor.closeCount)
                    fallbackMetadata
                },
                uriDescriptorOpener = descriptor::open,
            )

        val metadata = assertNotNull(reader.read(uri))

        assertEquals("Native Title", metadata.title)
        assertEquals("Fallback Artist", metadata.artist)
        assertEquals("Fallback Album", metadata.album)
        assertEquals(456_000L, metadata.durationMillis)
        assertEquals(-6.0, metadata.replayGainTrackGainDb)
        assertEquals(1.2, metadata.replayGainTrackPeak)
        assertNull(metadata.replayGainAlbumGainDb)
        assertEquals(1.1, metadata.replayGainAlbumPeak)
        assertEquals(1, descriptor.closeCount)
    }

    @Test
    fun unsuccessfulUriTagReadsCloseTheirDescriptorBeforePlatformFallback() {
        listOf(
                TagReadResult.Unsupported("Unsupported content"),
                TagReadResult.Failed("Unreadable tags"),
            )
            .forEach { result ->
                val descriptor = FixtureUriDescriptor()
                val reader =
                    AudioMetadataReader(
                        tagLibReader =
                            DescriptorTagLibReader {
                                descriptor.assertOpen()
                                result
                            },
                        platformMetadataReader = { source ->
                            assertEquals(uri, source)
                            assertEquals(1, descriptor.closeCount)
                            fallbackMetadata
                        },
                        uriDescriptorOpener = descriptor::open,
                    )

                val metadata = assertNotNull(reader.read(uri))

                assertEquals("Fallback Title", metadata.title)
                assertEquals(456_000L, metadata.durationMillis)
                assertNull(metadata.replayGainTrackGainDb)
                assertEquals(1, descriptor.closeCount)
            }
    }

    @Test
    fun thrownUriTagReadClosesItsDescriptorAndRetainsPlatformFallback() {
        val descriptor = FixtureUriDescriptor()
        val reader =
            AudioMetadataReader(
                tagLibReader =
                    DescriptorTagLibReader {
                        descriptor.assertOpen()
                        throw IllegalStateException("Native read failed")
                    },
                platformMetadataReader = { source ->
                    assertEquals(uri, source)
                    assertEquals(1, descriptor.closeCount)
                    fallbackMetadata
                },
                uriDescriptorOpener = descriptor::open,
            )

        val metadata = assertNotNull(reader.read(uri))

        assertEquals("Fallback Title", metadata.title)
        assertEquals(456_000L, metadata.durationMillis)
        assertEquals(1, descriptor.closeCount)
    }

    @Test
    fun unavailableUriDescriptorRetainsPlatformFallback() {
        listOf<(AudioSource.Uri) -> AudioMetadataUriDescriptor?>(
                { source ->
                    assertEquals(uri, source)
                    null
                },
                { source ->
                    assertEquals(uri, source)
                    throw IllegalStateException("Content access revoked")
                },
            )
            .forEach { opener ->
                var openCount = 0
                val reader =
                    AudioMetadataReader(
                        tagLibReader =
                            DescriptorTagLibReader {
                                error("Unavailable content has no descriptor")
                            },
                        platformMetadataReader = { source ->
                            assertEquals(uri, source)
                            fallbackMetadata
                        },
                        uriDescriptorOpener = { source ->
                            openCount++
                            opener(source)
                        },
                    )

                val metadata = assertNotNull(reader.read(uri))

                assertEquals("Fallback Title", metadata.title)
                assertEquals(456_000L, metadata.durationMillis)
                assertEquals(1, openCount)
            }
    }

    private class FixtureUriDescriptor {
        var closeCount = 0
            private set

        fun open(source: AudioSource.Uri): AudioMetadataUriDescriptor {
            assertEquals(uri, source)
            return AudioMetadataUriDescriptor(
                source = AudioSource.FileDescriptor(42, "song.flac"),
                close = { closeCount++ },
            )
        }

        fun assertOpen() {
            assertEquals(0, closeCount)
        }
    }

    private class DescriptorTagLibReader(
        private val read: () -> TagReadResult,
    ) : TagLibReader {
        override fun readPath(path: String): TagReadResult =
            error("URI metadata must use its descriptor, not a path")

        override fun readFd(fd: Int, displayName: String): TagReadResult {
            assertEquals(42, fd)
            assertEquals("song.flac", displayName)
            return read()
        }

        override fun readProperties(path: String): Map<String, String> =
            error("ReplayGain must use the existing metadata result")
    }

    private fun completeTags(): TagMetadata =
        TagMetadata(
            title = "Native Title",
            artist = "Native Artist",
            album = "Native Album",
            durationMillis = 123_000L,
            trackNumber = 3,
            discNumber = 2,
            artwork = EmbeddedArtwork("image/png", byteArrayOf(1, 2)),
            replayGainTrackGain = " -6.00 dB ",
            replayGainTrackPeak = "1.2",
            replayGainAlbumGain = "-4 DB",
            replayGainAlbumPeak = "1.1",
        )

    private companion object {
        val uri = AudioSource.Uri("content://media/external/audio/media/9")
        val fallbackMetadata =
            AudioMetadata(
                title = "Fallback Title",
                artist = "Fallback Artist",
                album = "Fallback Album",
                durationMillis = 456_000L,
            )
    }
}
