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
                Res.string.playlist_backup_export_json,
                Res.string.playlist_backup_import_json,
                Res.string.playlist_backup_interoperability,
                Res.string.playlist_backup_static_playlist,
                Res.string.playlist_backup_no_static_playlists,
                Res.string.playlist_backup_export_selected,
                Res.string.playlist_backup_import_format,
                Res.string.playlist_backup_selected,
                Res.string.playlist_backup_not_selected,
                Res.string.playlist_backup_preview_title,
                Res.string.playlist_backup_result_title,
                Res.string.playlist_backup_repository_error,
            )
        assertEquals(15, keys.distinct().size)
    }
}
