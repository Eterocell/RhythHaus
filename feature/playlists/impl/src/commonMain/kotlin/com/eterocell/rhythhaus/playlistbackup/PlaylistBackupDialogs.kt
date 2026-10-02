package com.eterocell.rhythhaus.playlistbackup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.PlaylistSummary
import com.eterocell.rhythhaus.library.ui.PlaylistDismissalAppearance
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureAppearanceSource
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDestination
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissal
import com.eterocell.rhythhaus.library.ui.PlaylistFeatureDismissalPublisher
import com.eterocell.rhythhaus.library.ui.PublishFeatureDismissal
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.HausLazyDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.playlists.generated.resources.*
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

internal const val PlaylistBackupPreviewListTag = "playlist-backup-preview-list"
internal const val PlaylistBackupStaticExportTag =
    "playlist-backup-static-export"
internal const val PlaylistBackupFormatImportTag =
    "playlist-backup-format-import"

private val InteroperablePlaylistFormats =
    listOf(
        PlaylistDocumentFormat.M3u,
        PlaylistDocumentFormat.M3u8,
        PlaylistDocumentFormat.Pls,
    )

private fun formatLabel(format: PlaylistDocumentFormat): String =
    when (format) {
        PlaylistDocumentFormat.RhythHausJson -> "JSON"
        PlaylistDocumentFormat.M3u -> "M3U"
        PlaylistDocumentFormat.M3u8 -> "M3U8"
        PlaylistDocumentFormat.Pls -> "PLS"
    }

private fun playlistBackupFormatTag(format: PlaylistDocumentFormat): String =
    "playlist-backup-format-${formatLabel(format).lowercase()}"

/**
 * Shared-owned labels rendered unchanged by the feature settings surfaces.
 *
 * Value equality is defined by [cancel] and [close].
 *
 * @property cancel injected Shared cancel label.
 * @property close injected Shared close label.
 */
public data class PlaylistBackupSettingsLabels(
    /** Injected Shared cancel label. */
    public val cancel: String,
    /** Injected Shared close label. */
    public val close: String,
)

/**
 * Renders backup commands and the current operation/error state. Shared
 * controls navigation; this feature surface dispatches only the supplied
 * actions.
 */
@Composable
public fun PlaylistBackupSettingsSection(
    state: PlaylistBackupUiState,
    launcherAvailable: Boolean,
    labels: PlaylistBackupSettingsLabels,
    onExport: () -> Unit,
    onOpen: () -> Unit,
    onAction: (PlaylistBackupUiAction) -> Unit,
    modifier: Modifier = Modifier,
    staticPlaylists: List<PlaylistSummary> = emptyList(),
    onExportPlaylistFormat: ((String, PlaylistDocumentFormat) -> Unit)? = null,
    onOpenPlaylistFormat: ((PlaylistDocumentFormat) -> Unit)? = null,
) {
    var selectedFormat by remember {
        mutableStateOf(PlaylistDocumentFormat.M3u)
    }
    var selectedStaticPlaylistId by
        remember(staticPlaylists) {
            mutableStateOf(staticPlaylists.firstOrNull()?.id)
        }
    val selectedStaticPlaylist = staticPlaylists.firstOrNull {
        it.id == selectedStaticPlaylistId
    }
    val actionsEnabled = launcherAvailable && !state.isBusy
    Column(
        modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DialogTitle(stringResource(Res.string.playlist_backup_section))
            SettingsSectionButton(
                label =
                    stringResource(
                        if (state.operation ==
                            PlaylistBackupOperation.Exporting)
                            Res.string.playlist_backup_exporting
                        else Res.string.playlist_backup_export_json),
                onClick = onExport,
                enabled = actionsEnabled,
                primary = true,
                testTag = "playlist-backup-json-export",
            )
            SettingsSectionButton(
                label =
                    stringResource(
                        if (state.operation ==
                            PlaylistBackupOperation.Opening ||
                            state.operation == PlaylistBackupOperation.Planning)
                            Res.string.playlist_backup_importing
                        else Res.string.playlist_backup_import_json),
                onClick = onOpen,
                enabled = actionsEnabled,
                primary = false,
                testTag = "playlist-backup-json-import",
            )
            Text(
                stringResource(Res.string.smart_backup_note),
                color = HausColors.current.muted,
            )
            DialogTitle(
                stringResource(Res.string.playlist_backup_interoperability),
            )
            PlaylistDocumentFormatSelector(
                selectedFormat = selectedFormat,
                enabled = actionsEnabled,
                onSelected = { selectedFormat = it },
            )
            Text(
                stringResource(Res.string.playlist_backup_static_playlist),
                color = HausColors.current.muted,
            )
            if (staticPlaylists.isEmpty()) {
                CountLine(
                    stringResource(
                        Res.string.playlist_backup_no_static_playlists),
                )
            } else {
                staticPlaylists.forEach { playlist ->
                    PlaylistStaticSelectionButton(
                        playlist = playlist,
                        selected = playlist.id == selectedStaticPlaylistId,
                        enabled = actionsEnabled,
                        onClick = { selectedStaticPlaylistId = playlist.id },
                    )
                }
            }
            SettingsSectionButton(
                label =
                    stringResource(
                        if (state.operation ==
                            PlaylistBackupOperation.Exporting) {
                            Res.string.playlist_backup_exporting
                        } else {
                            Res.string.playlist_backup_export_selected
                        },
                    ),
                onClick = {
                    selectedStaticPlaylist?.let { playlist ->
                        onExportPlaylistFormat?.invoke(
                            playlist.id, selectedFormat)
                    }
                },
                enabled =
                    actionsEnabled &&
                        selectedStaticPlaylist != null &&
                        onExportPlaylistFormat != null,
                primary = true,
                testTag = PlaylistBackupStaticExportTag,
            )
            SettingsSectionButton(
                label =
                    stringResource(
                        if (state.operation ==
                            PlaylistBackupOperation.Opening ||
                            state.operation ==
                                PlaylistBackupOperation.Planning) {
                            Res.string.playlist_backup_importing
                        } else {
                            Res.string.playlist_backup_import_format
                        },
                    ),
                onClick = {
                    onOpenPlaylistFormat?.invoke(selectedFormat)
                },
                enabled = actionsEnabled && onOpenPlaylistFormat != null,
                primary = false,
                testTag = PlaylistBackupFormatImportTag,
            )
            state.error?.let { error ->
                CountLine(stringResource(error.resource))
                SettingsSectionButton(
                    labels.close,
                    { onAction(PlaylistBackupUiAction.ClearError) },
                    primary = false)
            }
        }
}

/**
 * Settings-page action button: 48dp tall, 16dp corner radius, 14sp bold label.
 * Primary uses ink/paper; secondary uses the shared muted tint.
 */
@Composable
private fun PlaylistDocumentFormatSelector(
    selectedFormat: PlaylistDocumentFormat,
    enabled: Boolean,
    onSelected: (PlaylistDocumentFormat) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InteroperablePlaylistFormats.forEach { format ->
            val selected = format == selectedFormat
            val selectionDescription =
                stringResource(
                    if (selected) Res.string.playlist_backup_selected
                    else Res.string.playlist_backup_not_selected)
            Button(
                onClick = { onSelected(format) },
                enabled = enabled,
                modifier =
                    Modifier.weight(1f)
                        .height(44.dp)
                        .testTag(playlistBackupFormatTag(format))
                        .semantics {
                            role = Role.RadioButton
                            this.selected = selected
                            stateDescription = selectionDescription
                        },
                cornerRadius = 12.dp,
                colors =
                    ButtonDefaults.buttonColors(
                        color =
                            if (selected) {
                                HausColors.current.ink
                            } else {
                                HausColors.current.muted.copy(alpha = 0.15f)
                            },
                        contentColor =
                            if (selected) {
                                HausColors.current.paper
                            } else {
                                HausColors.current.muted
                            },
                        disabledColor =
                            HausColors.current.muted.copy(alpha = 0.28f),
                        disabledContentColor = HausColors.current.muted,
                    ),
            ) {
                Text(
                    formatLabel(format),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlaylistStaticSelectionButton(
    playlist: PlaylistSummary,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val selectionDescription =
        stringResource(
            if (selected) Res.string.playlist_backup_selected
            else Res.string.playlist_backup_not_selected)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth()
                .height(44.dp)
                .testTag("playlist-backup-static-${playlist.id}")
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    stateDescription = selectionDescription
                },
        cornerRadius = 12.dp,
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (selected) {
                        HausColors.current.ink
                    } else {
                        HausColors.current.muted.copy(alpha = 0.15f)
                    },
                contentColor =
                    if (selected) {
                        HausColors.current.paper
                    } else {
                        HausColors.current.muted
                    },
                disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                disabledContentColor = HausColors.current.muted,
            ),
    ) {
        Text(playlist.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SettingsSectionButton(
    label: String,
    onClick: () -> Unit,
    primary: Boolean,
    enabled: Boolean = true,
    testTag: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth()
                .height(48.dp)
                .then(testTag?.let(Modifier::testTag) ?: Modifier),
        cornerRadius = 16.dp,
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (primary) HausColors.current.ink
                    else HausColors.current.muted.copy(alpha = 0.15f),
                contentColor =
                    if (primary) HausColors.current.paper
                    else HausColors.current.muted,
                disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                disabledContentColor = HausColors.current.muted),
    ) {
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Hosts settings dialogs while Shared remains the dismissal-policy authority.
 * Shared controls navigation and receives all visible-dialog dismissals through
 * its publisher.
 */
@Composable
public fun PlaylistBackupSettingsHost(
    state: PlaylistBackupUiState,
    launcherAvailable: Boolean,
    destination: PlaylistFeatureDestination,
    appearanceSource: PlaylistFeatureAppearanceSource,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    labels: PlaylistBackupSettingsLabels,
    onExport: () -> Unit,
    onOpen: () -> Unit,
    onAction: (PlaylistBackupUiAction) -> Unit,
    onDismissPreview: () -> Unit,
    onConfirmPreview: () -> Unit,
    onDismissResult: () -> Unit,
    modifier: Modifier = Modifier,
    staticPlaylists: List<PlaylistSummary> = emptyList(),
    onExportPlaylistFormat: ((String, PlaylistDocumentFormat) -> Unit)? = null,
    onOpenPlaylistFormat: ((PlaylistDocumentFormat) -> Unit)? = null,
) {
    var previewAppearance by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    var resultAppearance by remember {
        mutableStateOf<PlaylistDismissalAppearance?>(null)
    }
    if (state.preview != null && previewAppearance == null) {
        previewAppearance = appearanceSource.next("settings-preview")
    }
    if (state.preview == null) previewAppearance = null
    if (state.result != null && resultAppearance == null) {
        resultAppearance = appearanceSource.next("settings-result")
    }
    if (state.result == null) resultAppearance = null
    PlaylistBackupSettingsSection(
        state = state,
        launcherAvailable = launcherAvailable,
        labels = labels,
        onExport = onExport,
        onOpen = onOpen,
        onAction = onAction,
        modifier = modifier,
        staticPlaylists = staticPlaylists,
        onExportPlaylistFormat = onExportPlaylistFormat,
        onOpenPlaylistFormat = onOpenPlaylistFormat,
    )
    state.preview?.let { preview ->
        PlaylistBackupPreviewDialog(
            preview,
            state.isBusy,
            destination,
            requireNotNull(previewAppearance),
            dismissalPublisher,
            labels,
            onDismissPreview,
            onConfirmPreview)
    }
    state.result?.let { result ->
        PlaylistBackupResultDialog(
            result,
            destination,
            requireNotNull(resultAppearance),
            dismissalPublisher,
            labels,
            onDismissResult)
    }
}

/**
 * Renders the import preview and publishes its stable settings-preview
 * dismissal identity.
 */
@Composable
public fun PlaylistBackupPreviewDialog(
    preview: PlaylistBackupPreview,
    isBusy: Boolean,
    destination: PlaylistFeatureDestination,
    appearance: PlaylistDismissalAppearance,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    labels: PlaylistBackupSettingsLabels,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val title = stringResource(Res.string.playlist_backup_preview_title)
    val reportsByIndex =
        remember(preview.reports) {
            preview.reports.associateBy(
                PlaylistBackupPlaylistReport::sourcePlaylistIndex)
        }
    HausLazyDialog(
        title = title,
        onDismiss = onDismiss,
        dismissLabel = labels.cancel,
        bodyModifier = Modifier.testTag(PlaylistBackupPreviewListTag),
        body = {
            item("title") {
                DialogTitle(title)
                Spacer(Modifier.height(12.dp))
            }
            val totals = preview.totals
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
                count = preview.reports.size,
                key = { index ->
                    "report-${preview.reports[index].sourcePlaylistIndex}"
                },
            ) { index ->
                val report = preview.reports[index]
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
                count = preview.issues.size,
                key = { index ->
                    "issue-${preview.issues[index].playlistIndex}-${preview.issues[index].entryIndex}"
                },
            ) { index ->
                val issue = preview.issues[index]
                val playlistName =
                    reportsByIndex[issue.playlistIndex]?.sourceName.orEmpty()
                val kind =
                    when (issue.kind) {
                        PlaylistBackupIssueKind.Unmatched ->
                            stringResource(Res.string.playlist_backup_unmatched)
                        PlaylistBackupIssueKind.Ambiguous ->
                            stringResource(Res.string.playlist_backup_ambiguous)
                    }
                val accessibility =
                    stringResource(
                        Res.string.playlist_backup_issue_accessibility,
                        playlistName,
                        issue.entry.title,
                        kind,
                    )
                Column(
                    modifier =
                        Modifier.fillMaxWidth()
                            .background(
                                HausColors.current.paper,
                                RoundedCornerShape(12.dp))
                            .semantics(mergeDescendants = true) {
                                contentDescription = accessibility
                            }
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DialogButton(
                        label =
                            stringResource(
                                Res.string.playlist_backup_confirm_import),
                        onClick = onConfirm,
                        enabled = preview.canConfirm && !isBusy,
                        primary = true,
                        testTag = "playlist-backup-preview-confirm",
                    )
                    DialogButton(
                        label = labels.cancel,
                        onClick = onDismiss,
                        primary = false,
                        testTag = "playlist-backup-preview-dismiss",
                    )
                }
        },
    )
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance)) {
            onDismiss()
        }
}

/**
 * Renders the completed import result and publishes its stable settings-result
 * dismissal identity.
 */
@Composable
public fun PlaylistBackupResultDialog(
    result: PlaylistBackupImportResult,
    destination: PlaylistFeatureDestination,
    appearance: PlaylistDismissalAppearance,
    dismissalPublisher: PlaylistFeatureDismissalPublisher,
    labels: PlaylistBackupSettingsLabels,
    onDismiss: () -> Unit,
) {
    val title = stringResource(Res.string.playlist_backup_result_title)
    HausDialog(
        title = title,
        onDismiss = onDismiss,
        dismissLabel = labels.close,
        body = {
            DialogTitle(title)
            Spacer(Modifier.height(12.dp))
            CountLine(
                stringResource(
                    if (result.playlistsToCreate == 1) {
                        Res.string.playlist_backup_result_created_one
                    } else {
                        Res.string.playlist_backup_result_created
                    },
                    result.playlistsToCreate,
                ),
            )
            CountLine(
                stringResource(
                    if (result.playlistsSkipped == 1) {
                        Res.string.playlist_backup_result_skipped_one
                    } else {
                        Res.string.playlist_backup_result_skipped
                    },
                    result.playlistsSkipped,
                ),
            )
            CountLine(
                stringResource(
                    if (result.entries.restorable == 1) {
                        Res.string.playlist_backup_result_restored_one
                    } else {
                        Res.string.playlist_backup_result_restored
                    },
                    result.entries.restorable,
                ),
            )
            CountLine(
                stringResource(
                    Res.string.playlist_backup_result_unmatched,
                    result.entries.unmatched))
            CountLine(
                stringResource(
                    Res.string.playlist_backup_result_ambiguous,
                    result.entries.ambiguous))
        },
        actions = {
            DialogButton(
                label = labels.close,
                onClick = onDismiss,
                primary = true,
                testTag = "playlist-backup-result-dismiss",
            )
        },
    )
    PublishFeatureDismissal(
        destination,
        dismissalPublisher,
        PlaylistFeatureDismissal.Modal(destination, appearance)) {
            onDismiss()
        }
}

private val PlaylistBackupUiError.resource
    get() =
        when (this) {
            PlaylistBackupUiError.Unavailable ->
                Res.string.playlist_backup_unavailable_error
            PlaylistBackupUiError.ReadFailed ->
                Res.string.playlist_backup_read_error
            PlaylistBackupUiError.WriteFailed ->
                Res.string.playlist_backup_write_error
            PlaylistBackupUiError.Oversized ->
                Res.string.playlist_backup_oversized_error
            PlaylistBackupUiError.Malformed ->
                Res.string.playlist_backup_malformed_error
            PlaylistBackupUiError.InvalidData ->
                Res.string.playlist_backup_import_invalid_data_error
            PlaylistBackupUiError.Checksum ->
                Res.string.playlist_backup_checksum_error
            PlaylistBackupUiError.UnsupportedVersion ->
                Res.string.playlist_backup_version_error
            PlaylistBackupUiError.StalePreview ->
                Res.string.playlist_backup_stale_error
            PlaylistBackupUiError.ExportMissingTrack ->
                Res.string.playlist_backup_missing_track_error
            PlaylistBackupUiError.ExportMissingDuration ->
                Res.string.playlist_backup_missing_duration_error
            PlaylistBackupUiError.ExportInvalidDuration ->
                Res.string.playlist_backup_invalid_duration_error
            PlaylistBackupUiError.ExportInvalidData ->
                Res.string.playlist_backup_invalid_data_error
            PlaylistBackupUiError.RepositoryFailed ->
                Res.string.playlist_backup_repository_error
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
        color =
            if (emphasized) HausColors.current.ink
            else HausColors.current.muted,
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
    testTag: String? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier =
            Modifier.fillMaxWidth()
                .height(44.dp)
                .then(
                    testTag?.let(Modifier::testTag) ?: Modifier,
                ),
        cornerRadius = 12.dp,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
        colors =
            ButtonDefaults.buttonColors(
                color =
                    if (primary) HausColors.current.ink
                    else HausColors.current.muted.copy(alpha = 0.15f),
                contentColor =
                    if (primary) HausColors.current.paper
                    else HausColors.current.muted,
                disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                disabledContentColor = HausColors.current.muted,
            ),
    ) {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
