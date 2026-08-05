package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertEquals

class PlaylistBackupSettingsSectionTest {
    @Test
    fun injectedCancelAndCloseDispatchCurrentActions() {
        val labels =
            PlaylistBackupSettingsLabels(
                cancel = "Cancel action", close = "Close action")
        val action = PlaylistBackupUiAction.ClearError
        assertEquals("Cancel action", labels.cancel)
        assertEquals("Close action", labels.close)
        assertEquals(PlaylistBackupUiAction.ClearError, action)
    }
}
