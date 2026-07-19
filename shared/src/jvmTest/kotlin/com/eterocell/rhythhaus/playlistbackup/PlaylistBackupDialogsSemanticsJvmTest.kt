package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.test.Test
import java.util.Locale

class PlaylistBackupDialogsSemanticsJvmTest {
    @OptIn(ExperimentalTestApi::class)
    @Test
    fun previewExposesIssueRowsAndDisablesConfirmationWithoutRestorableEntries() = withEnglishLocale {
        runComposeUiTest {
            setContent {
            PlaylistBackupPreviewDialog(
                preview = PlaylistBackupPreview(
                    PlaylistImportPlan(
                        libraryRevision = 3,
                        playlists = emptyList(),
                        reports = listOf(
                            PlaylistImportPlaylistReport(
                                sourcePlaylistIndex = 0,
                                sourceName = "Road Mix",
                                plannedName = null,
                                counts = PlaylistImportCounts(0, 1, 1),
                            ),
                        ),
                        totals = PlaylistImportTotals(0, 1, PlaylistImportCounts(0, 1, 1)),
                        issues = listOf(
                            PlaylistImportIssue(
                                playlistIndex = 0,
                                entryIndex = 0,
                                entry = PlaylistBackupEntry("Missing Song", "Artist", "Album", 100),
                                kind = PlaylistImportIssueKind.UNMATCHED,
                                candidateTrackIds = emptyList(),
                            ),
                            PlaylistImportIssue(
                                playlistIndex = 0,
                                entryIndex = 1,
                                entry = PlaylistBackupEntry("Duplicate Song", "Artist", "Album", 100),
                                kind = PlaylistImportIssueKind.AMBIGUOUS,
                                candidateTrackIds = listOf("one", "two"),
                            ),
                        ),
                    ),
                ),
                isBusy = false,
                onDismiss = {},
                onConfirm = {},
            )
        }

            onNode(hasText("Import playlists")).assertIsNotEnabled()
            onNode(hasContentDescription("Road Mix, Missing Song, unmatched")).assertExists()
            onNode(hasContentDescription("Road Mix, Duplicate Song, ambiguous")).assertExists()
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun resultReportsSelectedCreatedSkippedAndEntryCounts() = withEnglishLocale {
        runComposeUiTest {
            setContent {
            PlaylistBackupResultDialog(
                result = PlaylistBackupImportResult(
                    PlaylistImportTotals(2, 1, PlaylistImportCounts(5, 3, 4)),
                ),
                onDismiss = {},
            )
        }

            onNode(hasText("2 playlists created")).assertExists()
            onNode(hasText("1 playlist skipped")).assertExists()
            onNode(hasText("5 tracks restored")).assertExists()
            onNode(hasText("3 unmatched")).assertExists()
            onNode(hasText("4 ambiguous")).assertExists()
        }
    }

    private fun withEnglishLocale(block: () -> Unit) {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.ENGLISH)
            block()
        } finally {
            Locale.setDefault(original)
        }
    }
}
