package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.HausLazyDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.close
import rhythhaus.shared.generated.resources.playlist_backup_ambiguous
import rhythhaus.shared.generated.resources.playlist_backup_confirm_import
import rhythhaus.shared.generated.resources.playlist_backup_issue_accessibility
import rhythhaus.shared.generated.resources.playlist_backup_preview_playlist_counts
import rhythhaus.shared.generated.resources.playlist_backup_preview_title
import rhythhaus.shared.generated.resources.playlist_backup_result_ambiguous
import rhythhaus.shared.generated.resources.playlist_backup_result_created
import rhythhaus.shared.generated.resources.playlist_backup_result_created_one
import rhythhaus.shared.generated.resources.playlist_backup_result_restored
import rhythhaus.shared.generated.resources.playlist_backup_result_restored_one
import rhythhaus.shared.generated.resources.playlist_backup_result_skipped
import rhythhaus.shared.generated.resources.playlist_backup_result_skipped_one
import rhythhaus.shared.generated.resources.playlist_backup_result_title
import rhythhaus.shared.generated.resources.playlist_backup_result_unmatched
import rhythhaus.shared.generated.resources.playlist_backup_totals
import rhythhaus.shared.generated.resources.playlist_backup_unmatched
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

internal const val PlaylistBackupPreviewListTag = "playlist-backup-preview-list"

@Composable
fun PlaylistBackupPreviewDialog(
    preview: PlaylistBackupPreview,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(Res.string.playlist_backup_preview_title)
    val reportsByIndex = remember(preview.plan.reports) {
        preview.plan.reports.associateBy(PlaylistImportPlaylistReport::sourcePlaylistIndex)
    }
    HausLazyDialog(
        title = title,
        onDismiss = onDismiss,
        dismissLabel = stringResource(Res.string.cancel),
        bodyModifier = Modifier.testTag(PlaylistBackupPreviewListTag),
        body = {
            item("title") {
                DialogTitle(title)
                Spacer(Modifier.height(12.dp))
            }
            val totals = preview.plan.totals.entries
            item("totals") {
                CountLine(
                    stringResource(
                        Res.string.playlist_backup_totals,
                        totals.restorable,
                        totals.unmatched,
                        totals.ambiguous,
                    ),
                    emphasized = true,
                )
                Spacer(Modifier.height(12.dp))
            }
            items(
                count = preview.plan.reports.size,
                key = { index -> "report-${preview.plan.reports[index].sourcePlaylistIndex}" },
            ) { index ->
                val report = preview.plan.reports[index]
                CountLine(
                    stringResource(
                        Res.string.playlist_backup_preview_playlist_counts,
                        report.sourceName,
                        report.counts.restorable,
                        report.counts.unmatched,
                        report.counts.ambiguous,
                    ),
                )
                Spacer(Modifier.height(8.dp))
            }
            items(
                count = preview.plan.issues.size,
                key = { index -> "issue-${preview.plan.issues[index].playlistIndex}-${preview.plan.issues[index].entryIndex}" },
            ) { index ->
                val issue = preview.plan.issues[index]
                val playlistName = reportsByIndex[issue.playlistIndex]?.sourceName.orEmpty()
                val kind = when (issue.kind) {
                    PlaylistImportIssueKind.UNMATCHED -> stringResource(Res.string.playlist_backup_unmatched)
                    PlaylistImportIssueKind.AMBIGUOUS -> stringResource(Res.string.playlist_backup_ambiguous)
                }
                val accessibility = stringResource(
                    Res.string.playlist_backup_issue_accessibility,
                    playlistName,
                    issue.entry.title,
                    kind,
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HausColors.current.paper, RoundedCornerShape(12.dp))
                        .semantics(mergeDescendants = true) { contentDescription = accessibility }
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = issue.entry.title,
                        color = HausColors.current.ink,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "$playlistName · $kind",
                        color = HausColors.current.muted,
                        fontSize = 13.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        },
        actions = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DialogButton(
                    label = stringResource(Res.string.playlist_backup_confirm_import),
                    onClick = onConfirm,
                    enabled = preview.canConfirm && !isBusy,
                    primary = true,
                )
                DialogButton(
                    label = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                    primary = false,
                )
            }
        },
    )
}

@Composable
fun PlaylistBackupResultDialog(
    result: PlaylistBackupImportResult,
    onDismiss: () -> Unit,
) {
    val title = stringResource(Res.string.playlist_backup_result_title)
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        dismissLabel = stringResource(Res.string.close),
        body = {
            DialogTitle(title)
            Spacer(Modifier.height(12.dp))
            CountLine(
                stringResource(
                    if (result.totals.playlistsToCreate == 1) {
                        Res.string.playlist_backup_result_created_one
                    } else {
                        Res.string.playlist_backup_result_created
                    },
                    result.totals.playlistsToCreate,
                ),
            )
            CountLine(
                stringResource(
                    if (result.totals.playlistsSkipped == 1) {
                        Res.string.playlist_backup_result_skipped_one
                    } else {
                        Res.string.playlist_backup_result_skipped
                    },
                    result.totals.playlistsSkipped,
                ),
            )
            CountLine(
                stringResource(
                    if (result.totals.entries.restorable == 1) {
                        Res.string.playlist_backup_result_restored_one
                    } else {
                        Res.string.playlist_backup_result_restored
                    },
                    result.totals.entries.restorable,
                ),
            )
            CountLine(stringResource(Res.string.playlist_backup_result_unmatched, result.totals.entries.unmatched))
            CountLine(stringResource(Res.string.playlist_backup_result_ambiguous, result.totals.entries.ambiguous))
        },
        actions = {
            DialogButton(
                label = stringResource(Res.string.close),
                onClick = onDismiss,
                primary = true,
            )
        },
    )
}

@Composable
private fun DialogTitle(title: String) {
    Text(
        text = title,
        color = HausColors.current.ink,
        fontSize = 18.sp,
        fontWeight = FontWeight.Black,
    )
}

@Composable
private fun CountLine(text: String, emphasized: Boolean = false) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        color = if (emphasized) HausColors.current.ink else HausColors.current.muted,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Medium,
    )
}

@Composable
private fun DialogButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        colors = ButtonDefaults.buttonColors(
            color = if (primary) HausColors.current.ink else HausColors.current.muted.copy(alpha = 0.15f),
            contentColor = if (primary) HausColors.current.paper else HausColors.current.muted,
            disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
            disabledContentColor = HausColors.current.muted,
        ),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
