package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformPlaylistBackupDocumentsTest {
    @Test
    fun backupExtensionIsAppendedOnlyWhenAbsent() {
        assertEquals("playlists.rhythhaus-playlists.json", playlistBackupFileName("playlists"))
        assertEquals("playlists.rhythhaus-playlists.json", playlistBackupFileName("playlists.rhythhaus-playlists.json"))
    }

    @Test
    fun backupExtensionComparisonIgnoresCase() {
        assertEquals("PLAYLISTS.RHYTHHAUS-PLAYLISTS.JSON", playlistBackupFileName("PLAYLISTS.RHYTHHAUS-PLAYLISTS.JSON"))
    }

    @Test
    fun backupFileNameIsAlwaysOneSafePathComponent() {
        assertEquals("backup.rhythhaus-playlists.json", playlistBackupFileName("../backup"))
        assertEquals("backup.rhythhaus-playlists.json", playlistBackupFileName("folder\\backup"))
        assertEquals("rhythhaus-playlists.rhythhaus-playlists.json", playlistBackupFileName(".."))
        assertEquals("rhythhaus-playlists.rhythhaus-playlists.json", playlistBackupFileName("  "))
    }

    @Test
    fun operationGateRejectsOverlapUntilCompletion() {
        val gate = PlaylistBackupDocumentOperationGate()

        assertEquals(true, gate.tryStart())
        assertEquals(false, gate.tryStart())
        gate.finish()
        assertEquals(true, gate.tryStart())
    }
}
