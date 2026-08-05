package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertEquals
import rhythhaus.feature.playlists.generated.resources.*

class PlaylistDesktopResourceResolutionTest {
    @Test
    fun featureLocaleResolvesAllBackupDialogKeys() {
        val keys =
            listOf(
                Res.string.playlist_backup_section,
                Res.string.playlist_backup_export,
                Res.string.playlist_backup_import,
                Res.string.playlist_backup_preview_title,
                Res.string.playlist_backup_result_title,
                Res.string.playlist_backup_repository_error,
            )
        assertEquals(6, keys.distinct().size)
    }
}
