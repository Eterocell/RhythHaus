@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.eterocell.rhythhaus.playlistbackup

import com.eterocell.rhythhaus.AudioSource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

class PlaylistTrackReadabilityIosTest {
    @Test
    fun managedRelativeTracksRemainExportableAndMissingFilesAreRejected() {
        val files = NSFileManager.defaultManager
        val directory = "${NSTemporaryDirectory()}/${NSUUID().UUIDString}"
        check(files.createDirectoryAtPath(directory, true, null, null))
        try {
            check(files.createFileAtPath("$directory/song.m4a", null, null))
            assertTrue(
                isIosPlaylistTrackReadable(
                    AudioSource.FilePath("song.m4a"), directory))
            assertTrue(
                isIosPlaylistTrackReadable(
                    AudioSource.FilePath("$directory/song.m4a"), directory))
            assertTrue(
                isIosPlaylistTrackReadable(
                    AudioSource.Uri("file://$directory/song.m4a"), directory))
            assertFalse(
                isIosPlaylistTrackReadable(
                    AudioSource.FilePath(directory), directory))
            assertFalse(
                isIosPlaylistTrackReadable(
                    AudioSource.FilePath("missing.m4a"), directory))
            check(files.removeItemAtPath("$directory/song.m4a", null))
            assertFalse(
                isIosPlaylistTrackReadable(
                    AudioSource.FilePath("song.m4a"), directory))
        } finally {
            files.removeItemAtPath(directory, null)
        }
    }
}
