package com.eterocell.rhythhaus

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ModelContractTest {
    @Test
    fun trackRetainsDataClassCopyAndComponentOrder() {
        val original = track(artworkBytes = byteArrayOf(1, 2, 3))
        val copied = original.copy(title = "Updated")
        val (
            id,
            title,
            artist,
            album,
            duration,
            accent,
            source,
            trackNumber,
            discNumber,
            artwork) =
            copied

        assertEquals("track-id", id)
        assertEquals("Updated", title)
        assertEquals("Artist", artist)
        assertEquals("Album", album)
        assertEquals(180, duration)
        assertEquals(TrackAccent(start = 1L, end = 2L), accent)
        assertEquals(AudioSource.FilePath("/music/track.mp3"), source)
        assertEquals(null, trackNumber)
        assertEquals(null, discNumber)
        assertEquals(original.artworkBytes, artwork)
    }

    @Test
    fun playableTrackRetainsDataClassCopyAndComponentOrder() {
        val original = playableTrack(artworkBytes = byteArrayOf(4, 5, 6))
        val copied = original.copy(title = "Updated")
        val (id, title, artist, album, duration, source, artwork) = copied

        assertEquals("track-id", id)
        assertEquals("Updated", title)
        assertEquals("Artist", artist)
        assertEquals("Album", album)
        assertEquals(180_000L, duration)
        assertEquals(AudioSource.FilePath("/music/track.mp3"), source)
        assertEquals(original.artworkBytes, artwork)
    }

    @Test
    fun trackEqualityUsesArtworkArrayContent() {
        val artwork = byteArrayOf(1, 2, 3)
        val equalArtwork = byteArrayOf(1, 2, 3)

        assertEquals(track(artwork), track(equalArtwork))
        assertEquals(track(artwork).hashCode(), track(equalArtwork).hashCode())
        assertNotEquals(track(artwork), track(byteArrayOf(1, 2, 4)))
    }

    @Test
    fun playableTrackEqualityUsesArtworkArrayContent() {
        val artwork = byteArrayOf(4, 5, 6)
        val equalArtwork = byteArrayOf(4, 5, 6)

        assertEquals(playableTrack(artwork), playableTrack(equalArtwork))
        assertEquals(
            playableTrack(artwork).hashCode(),
            playableTrack(equalArtwork).hashCode())
        assertNotEquals(
            playableTrack(artwork), playableTrack(byteArrayOf(4, 5, 7)))
    }

    @Test
    fun librarySnapshotDerivesNowPlayingAndClampedTotalDuration() {
        val firstMatch =
            track(byteArrayOf(1)).copy(title = "First", durationSeconds = -10)
        val secondMatch =
            track(byteArrayOf(2)).copy(title = "Second", durationSeconds = 20)
        val negativeDuration =
            track(byteArrayOf(3)).copy(id = "other-track", durationSeconds = -5)
        val snapshot =
            LibrarySnapshot(
                title = "Library",
                subtitle = "Subtitle",
                tracks = listOf(firstMatch, secondMatch, negativeDuration),
                nowPlayingTrackId = "track-id",
            )

        assertEquals(firstMatch, snapshot.nowPlaying)
        assertEquals(
            null, snapshot.copy(nowPlayingTrackId = "missing").nowPlaying)
        assertEquals(20, snapshot.totalDurationSeconds)
    }

    @Test
    fun audioSourcesRetainDataClassEqualityCopyAndStableKeys() {
        val filePath = AudioSource.FilePath("/music/track.mp3")
        val uri = AudioSource.Uri("content://music/track")
        val descriptor =
            AudioSource.FileDescriptor(fd = 7, displayName = "Track")

        assertEquals(filePath, AudioSource.FilePath("/music/track.mp3"))
        assertEquals(filePath, filePath.copy())
        assertEquals(uri, AudioSource.Uri("content://music/track"))
        assertEquals(uri, uri.copy())
        assertEquals(
            descriptor,
            AudioSource.FileDescriptor(fd = 7, displayName = "Track"))
        assertEquals(descriptor, descriptor.copy())
        assertEquals("/music/track.mp3", filePath.stableKey)
        assertEquals("content://music/track", uri.stableKey)
        assertEquals("Track", descriptor.stableKey)
        assertEquals(
            "persistent-track-key",
            AudioSource.FileDescriptor(7, "Track", "persistent-track-key")
                .stableKey,
        )
    }

    private fun track(artworkBytes: ByteArray): Track =
        Track(
            id = "track-id",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationSeconds = 180,
            accent = TrackAccent(start = 1L, end = 2L),
            source = AudioSource.FilePath("/music/track.mp3"),
            artworkBytes = artworkBytes,
        )

    private fun playableTrack(artworkBytes: ByteArray): PlayableTrack =
        PlayableTrack(
            id = "track-id",
            title = "Track",
            artist = "Artist",
            album = "Album",
            durationMillis = 180_000L,
            source = AudioSource.FilePath("/music/track.mp3"),
            artworkBytes = artworkBytes,
        )
}
