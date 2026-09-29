package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import java.util.Locale
import kotlin.test.Test

class PlaylistBackupSettingsSectionTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun exportSurfaceExplainsSmartRuleExclusionInEnglishAndChinese() {
        val original = Locale.getDefault()
        try {
            for ((locale, export, notice) in
                listOf(
                    Triple(
                        Locale.ENGLISH,
                        "Export playlists",
                        "Smart playlists are not included in JSON backups"),
                    Triple(
                        Locale.SIMPLIFIED_CHINESE,
                        "导出播放列表",
                        "JSON 备份不包含智能播放列表"),
                )) {
                Locale.setDefault(locale)
                runComposeUiTest {
                    setContent {
                        PlaylistBackupSettingsSection(
                            state = PlaylistBackupUiState(),
                            launcherAvailable = true,
                            labels =
                                PlaylistBackupSettingsLabels("Cancel", "Close"),
                            onExport = {},
                            onOpen = {},
                            onAction = {},
                        )
                    }
                    onAllNodes(hasText(export))[0].assertExists()
                    onAllNodes(hasText(notice))[0].assertExists()
                }
            }
        } finally {
            Locale.setDefault(original)
        }
    }
}
