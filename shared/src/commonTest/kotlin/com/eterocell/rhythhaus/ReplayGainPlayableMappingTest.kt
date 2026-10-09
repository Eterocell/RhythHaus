package com.eterocell.rhythhaus

import com.eterocell.rhythhaus.library.LibraryTrack
import com.eterocell.rhythhaus.library.toPlayableTrack
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplayGainPlayableMappingTest {
    @Test
    fun directLibraryAndPresentationPlaybackRoutesCarryScannedReplayGainFacts() {
        val track =
            LibraryTrack(
                id = "track",
                sourceId = "source",
                sourceLocalKey = "song.flac",
                audioSource = AudioSource.FilePath("/Music/song.flac"),
                displayName = "song.flac",
                title = "Song",
                artist = "Artist",
                album = "Album",
                durationMillis = 123_000L,
                sizeBytes = 100L,
                modifiedAtEpochMillis = 1L,
                lastSeenScanId = "scan",
                createdAtEpochMillis = 1L,
                updatedAtEpochMillis = 2L,
                replayGainTrackGainDb = -6.0,
                replayGainTrackPeak = 1.2,
                replayGainAlbumGainDb = -4.0,
                replayGainAlbumPeak = 1.1,
            )
        val routes =
            listOf(
                track.toPlayableTrack(),
                librarySnapshot(listOf(track))
                    .tracks
                    .single()
                    .toPlayableTrack(),
            )
        for (playable in routes) {
            assertEquals("track", playable.id)
            assertEquals(
                AudioSource.FilePath("/Music/song.flac"), playable.source)
            assertEquals(
                listOf(-6.0, 1.2, -4.0, 1.1),
                listOf(
                    playable.replayGainTrackGainDb,
                    playable.replayGainTrackPeak,
                    playable.replayGainAlbumGainDb,
                    playable.replayGainAlbumPeak,
                ))
        }
    }
}
