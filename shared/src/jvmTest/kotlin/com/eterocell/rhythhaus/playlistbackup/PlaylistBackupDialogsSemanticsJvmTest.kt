package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.v2.runComposeUiTest
import java.util.Locale
import kotlin.test.Test

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

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun maximumValidPreviewComposesOnlyViewportRowsAndCanNavigateToFinalIssue() = withEnglishLocale {
        runComposeUiTest {
            val reports = List(1_000) { index ->
                PlaylistImportPlaylistReport(
                    sourcePlaylistIndex = index,
                    sourceName = "Playlist $index",
                    plannedName = null,
                    counts = PlaylistImportCounts(0, 100, 0),
                )
            }
            val issues = List(100_000) { index ->
                PlaylistImportIssue(
                    playlistIndex = index / 100,
                    entryIndex = index % 100,
                    entry = PlaylistBackupEntry("Issue $index", "Artist", "Album", 100),
                    kind = PlaylistImportIssueKind.UNMATCHED,
                    candidateTrackIds = emptyList(),
                )
            }
            setContent {
                PlaylistBackupPreviewDialog(
                    preview = PlaylistBackupPreview(
                        PlaylistImportPlan(
                            libraryRevision = 1,
                            playlists = emptyList(),
                            reports = reports,
                            totals = PlaylistImportTotals(0, 1_000, PlaylistImportCounts(0, 100_000, 0)),
                            issues = issues,
                        ),
                    ),
                    isBusy = false,
                    onDismiss = {},
                    onConfirm = {},
                )
            }

            waitForIdle()
            onNode(hasContentDescription("Playlist 999, Issue 99999, unmatched")).assertDoesNotExist()
            onNodeWithTag(PlaylistBackupPreviewListTag).performScrollToIndex(101_001)
            onNode(hasContentDescription("Playlist 999, Issue 99999, unmatched")).assertExists()
            onNode(hasText("Import playlists")).assertIsNotEnabled()
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
