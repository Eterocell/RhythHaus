package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PlaylistBackupIntegrationJvmTest {
    @Test
    fun orderedSelectionRoundTripsThroughAtomicBackupRestoreAndRepeatImport() {
        val payload =
            PlaylistBackupPayload(
                1,
                listOf(
                    PlaylistBackupPlaylist(
                        "Saved",
                        listOf(
                            PlaylistBackupEntry(
                                "First", "Artist", "Album", 120),
                            PlaylistBackupEntry(
                                "First", "Artist", "Album", 120)))))
        val decoded =
            assertIs<PlaylistBackupDecodeResult.Success>(
                PlaylistBackupCodec.decode(PlaylistBackupCodec.encode(payload)))
        assertEquals(payload.playlists, decoded.document.playlists)
    }
}
