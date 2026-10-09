package com.eterocell.rhythhaus.library.impl

import com.eterocell.rhythhaus.AudioSource
import com.eterocell.rhythhaus.taglib.EmbeddedArtwork
import com.eterocell.rhythhaus.taglib.TagLibReader
import com.eterocell.rhythhaus.taglib.TagMetadata
import com.eterocell.rhythhaus.taglib.TagReadResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AudioMetadataReplayGainTest {
    @Test
    fun pathMetadataParsesAllFourReplayGainFacts() {
        val metadata =
            readMetadata(
                completeTags(
                    replayGainTrackGain = " -6.00 dB ",
                    replayGainTrackPeak = "2",
                    replayGainAlbumGain = "+3 DB",
                    replayGainAlbumPeak = "1.1",
                ),
            )

        assertEquals(-6.0, metadata.replayGainTrackGainDb)
        assertEquals(2.0, metadata.replayGainTrackPeak)
        assertEquals(3.0, metadata.replayGainAlbumGainDb)
        assertEquals(1.1, metadata.replayGainAlbumPeak)
        assertEquals("Tagged Title", metadata.title)
        assertEquals(123_000L, metadata.durationMillis)
    }

    @Test
    fun descriptorMetadataIgnoresMalformedAlbumFieldsIndependently() {
        val metadata =
            readMetadata(
                completeTags(
                    replayGainTrackGain = "-6 dB",
                    replayGainTrackPeak = "1.2",
                    replayGainAlbumGain = "NaN dB",
                    replayGainAlbumPeak = "-1",
                ),
                AudioSource.FileDescriptor(fd = 42, displayName = "song.flac"),
            )

        assertEquals(-6.0, metadata.replayGainTrackGainDb)
        assertEquals(1.2, metadata.replayGainTrackPeak)
        assertNull(metadata.replayGainAlbumGainDb)
        assertNull(metadata.replayGainAlbumPeak)
        assertEquals("Tagged Title", metadata.title)
        assertEquals("Tagged Artist", metadata.artist)
        assertEquals("Tagged Album", metadata.album)
    }

    @Test
    fun invalidTrackGainAndAlbumPeakDoNotEraseOtherValidFields() {
        val metadata =
            readMetadata(
                completeTags(
                    replayGainTrackGain = "junk",
                    replayGainTrackPeak = "2",
                    replayGainAlbumGain = "-4 dB",
                    replayGainAlbumPeak = "0",
                ),
            )

        assertNull(metadata.replayGainTrackGainDb)
        assertEquals(2.0, metadata.replayGainTrackPeak)
        assertEquals(-4.0, metadata.replayGainAlbumGainDb)
        assertNull(metadata.replayGainAlbumPeak)
    }

    @Test
    fun missingReplayGainDoesNotRequestPlatformFallbackOrPropertiesRead() {
        val metadata = readMetadata(completeTags())

        assertNull(metadata.replayGainTrackGainDb)
        assertNull(metadata.replayGainTrackPeak)
        assertNull(metadata.replayGainAlbumGainDb)
        assertNull(metadata.replayGainAlbumPeak)
        assertEquals("Tagged Title", metadata.title)
    }

    @Test
    fun platformFallbackEnrichesMissingDisplayMetadataWithoutErasingFacts() {
        val tags =
            TagMetadata(
                title = "Native Title",
                replayGainTrackGain = "-6 dB",
                replayGainTrackPeak = "1.2",
                replayGainAlbumGain = "-4 dB",
                replayGainAlbumPeak = "1.1",
            )
        val reader =
            AudioMetadataReader(
                tagLibReader = FixtureTagLibReader(tags),
                platformMetadataReader = {
                    AudioMetadata(
                        title = "Fallback Title",
                        artist = "Fallback Artist",
                        album = "Fallback Album",
                        durationMillis = 123_000L,
                    )
                },
            )

        val metadata =
            assertNotNull(reader.read(AudioSource.FilePath("/music/song.flac")))

        assertEquals("Native Title", metadata.title)
        assertEquals("Fallback Artist", metadata.artist)
        assertEquals("Fallback Album", metadata.album)
        assertEquals(123_000L, metadata.durationMillis)
        assertEquals(-6.0, metadata.replayGainTrackGainDb)
        assertEquals(1.2, metadata.replayGainTrackPeak)
        assertEquals(-4.0, metadata.replayGainAlbumGainDb)
        assertEquals(1.1, metadata.replayGainAlbumPeak)
    }

    @Test
    fun replayGainFactChangesParticipateInMetadataEqualityAndHashing() {
        val original = AudioMetadata(title = "Same Title")
        val changed =
            listOf(
                original.copy(replayGainTrackGainDb = -6.0),
                original.copy(replayGainTrackPeak = 1.2),
                original.copy(replayGainAlbumGainDb = -4.0),
                original.copy(replayGainAlbumPeak = 1.1),
            )

        changed.forEach { metadata ->
            assertNotEquals(original, metadata)
            assertNotEquals(original.hashCode(), metadata.hashCode())
            assertEquals(metadata, metadata.copy())
            assertEquals(metadata.hashCode(), metadata.copy().hashCode())
        }
    }

    @Test
    fun signedZeroGainsFollowDataClassEqualityAndHashing() {
        val cases =
            listOf(
                AudioMetadata(replayGainTrackGainDb = 0.0) to
                    AudioMetadata(replayGainTrackGainDb = -0.0),
                AudioMetadata(replayGainAlbumGainDb = 0.0) to
                    AudioMetadata(replayGainAlbumGainDb = -0.0),
            )

        cases.forEach { (positiveZero, negativeZero) ->
            assertNotEquals(positiveZero, negativeZero)
            assertEquals(2, setOf(positiveZero, negativeZero).size)
            assertEquals(negativeZero, negativeZero.copy())
            assertEquals(
                negativeZero.hashCode(), negativeZero.copy().hashCode())
        }
    }

    private fun readMetadata(
        tags: TagMetadata,
        source: AudioSource = AudioSource.FilePath("/music/song.flac"),
    ): AudioMetadata =
        assertNotNull(
            AudioMetadataReader(
                    tagLibReader = FixtureTagLibReader(tags),
                    platformMetadataReader = {
                        error(
                            "Complete metadata must not reopen the audio source")
                    },
                )
                .read(source),
        )

    private fun completeTags(
        replayGainTrackGain: String? = null,
        replayGainTrackPeak: String? = null,
        replayGainAlbumGain: String? = null,
        replayGainAlbumPeak: String? = null,
    ): TagMetadata =
        TagMetadata(
            title = "Tagged Title",
            artist = "Tagged Artist",
            album = "Tagged Album",
            durationMillis = 123_000L,
            artwork = EmbeddedArtwork("image/png", byteArrayOf(1)),
            replayGainTrackGain = replayGainTrackGain,
            replayGainTrackPeak = replayGainTrackPeak,
            replayGainAlbumGain = replayGainAlbumGain,
            replayGainAlbumPeak = replayGainAlbumPeak,
        )

    private class FixtureTagLibReader(private val tags: TagMetadata) :
        TagLibReader {
        override fun readPath(path: String): TagReadResult {
            assertEquals("/music/song.flac", path)
            return TagReadResult.Found(tags)
        }

        override fun readFd(fd: Int, displayName: String): TagReadResult {
            assertEquals(42, fd)
            assertEquals("song.flac", displayName)
            return TagReadResult.Found(tags)
        }

        override fun readProperties(path: String): Map<String, String> =
            error("ReplayGain must use the existing metadata result")
    }
}
