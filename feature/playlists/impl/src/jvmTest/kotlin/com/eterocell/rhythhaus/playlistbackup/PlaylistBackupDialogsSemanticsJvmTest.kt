package com.eterocell.rhythhaus.playlistbackup

import kotlin.test.Test
import kotlin.test.assertFalse

class PlaylistBackupDialogsSemanticsJvmTest {
    @Test
    fun previewExposesIssueRowsAndDisablesConfirmationWithoutRestorableEntries() {
        val preview =
            PlaylistImportPlan(
                    3,
                    emptyList(),
                    emptyList(),
                    PlaylistImportTotals(0, 1, PlaylistImportCounts(0, 1, 1)),
                    emptyList())
                .toPreview()
        assertFalse(preview.canConfirm)
    }
}
