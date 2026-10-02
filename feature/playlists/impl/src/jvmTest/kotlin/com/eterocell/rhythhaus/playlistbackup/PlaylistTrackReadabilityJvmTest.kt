package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PlaylistTrackReadabilityJvmTest {
    @Test
    fun deletedMediaAndDirectoriesCannotBeExportedAsReadableTracks() {
        val directory = Files.createTempDirectory("playlist-readability")
        val file = Files.createTempFile(directory, "track", ".mp3")
        try {
            assertTrue(
                isJvmPlaylistTrackReadable(
                    AudioSource.FilePath(file.toString())))
            assertTrue(
                isJvmPlaylistTrackReadable(
                    AudioSource.Uri(file.toUri().toString())))
            Files.delete(file)
            assertFalse(
                isJvmPlaylistTrackReadable(
                    AudioSource.FilePath(file.toString())))
            assertFalse(
                isJvmPlaylistTrackReadable(
                    AudioSource.FilePath(directory.toString())))
            assertFalse(
                isJvmPlaylistTrackReadable(
                    AudioSource.Uri("https://example.invalid/audio.mp3")))
        } finally {
            Files.deleteIfExists(file)
            Files.deleteIfExists(directory)
        }
    }
}
