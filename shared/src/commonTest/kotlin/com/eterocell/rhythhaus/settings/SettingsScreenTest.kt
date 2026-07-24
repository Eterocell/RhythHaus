package com.eterocell.rhythhaus.settings

import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupOperation
import com.eterocell.rhythhaus.playlistbackup.PlaylistBackupUiState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsScreenTest {
    @Test
    fun backupActionsRequireAvailableLauncherAndIdleWorkflow() {
        assertTrue(playlistBackupActionsEnabled(true, PlaylistBackupUiState()))
        assertFalse(playlistBackupActionsEnabled(false, PlaylistBackupUiState()))
        assertFalse(
            playlistBackupActionsEnabled(
                true,
                PlaylistBackupUiState(operation = PlaylistBackupOperation.Opening),
            ),
        )
    }

    @Test
    fun generatedBuildVersionIsNonblank() {
        assertTrue(RhythHausBuildInfo.versionName.isNotBlank())
    }

    @Test
    fun compactSettingsLayoutPolicyUsesApprovedSpacing() {
        assertEquals(16.dp, CompactSettingsLayoutPolicy.horizontalPagePadding)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.verticalPagePadding)
        assertEquals(12.dp, CompactSettingsLayoutPolicy.itemSpacing)
        assertEquals(8.dp, CompactSettingsLayoutPolicy.bottomContentPadding)
    }

    @Test
    fun compactSettingsLayoutPolicyZerosOutComponentOwnedHorizontalPaddingInsidePageInset() {
        assertEquals(0.dp, CompactSettingsLayoutPolicy.topBarTitlePadding)
        assertEquals(0.dp, CompactSettingsLayoutPolicy.topBarNavigationIconPadding)
        assertEquals(0.dp, CompactSettingsLayoutPolicy.appearanceHorizontalInsidePadding)
        assertEquals(16.dp, CompactSettingsLayoutPolicy.appearanceVerticalInsidePadding)
    }

    @Test
    fun rhythHausSourceUrlIsExactRepositoryAddress() {
        assertEquals("https://github.com/Eterocell/RhythHaus", RhythHausSourceUrl)
    }

    @Test
    fun aboutScreenPresentsNonblankGeneratedVersion() {
        assertTrue(RhythHausBuildInfo.versionName.isNotBlank())
    }

    @Test
    fun aboutScreensUseReadableWideLayoutAndProminentLogo() {
        assertEquals(720.dp, AboutScreenLayoutPolicy.maxContentWidth)
        assertEquals(80.dp, AboutScreenLayoutPolicy.logoImageSize)
    }
}
