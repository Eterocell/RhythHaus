package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.library.TrackMetadataEditorData
import com.eterocell.rhythhaus.library.TrackMetadataOverride
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.library.generated.resources.Res
import rhythhaus.feature.library.generated.resources.metadata_album
import rhythhaus.feature.library.generated.resources.metadata_artist
import rhythhaus.feature.library.generated.resources.metadata_cancel
import rhythhaus.feature.library.generated.resources.metadata_disc_number
import rhythhaus.feature.library.generated.resources.metadata_edit_title
import rhythhaus.feature.library.generated.resources.metadata_failure
import rhythhaus.feature.library.generated.resources.metadata_invalid_number
import rhythhaus.feature.library.generated.resources.metadata_restore_all
import rhythhaus.feature.library.generated.resources.metadata_restore_field
import rhythhaus.feature.library.generated.resources.metadata_save
import rhythhaus.feature.library.generated.resources.metadata_scanned_value
import rhythhaus.feature.library.generated.resources.metadata_title
import rhythhaus.feature.library.generated.resources.metadata_track_number
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

/**
 * Blank fields inherit the latest scan; invalid numeric fields are never saved.
 */
public data class TrackMetadataDraft(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val trackNumber: String = "",
    val discNumber: String = "",
) {
    /** Converts valid non-empty draft values into a persistence projection. */
    public fun toOverrideOrNull(): TrackMetadataOverride? {
        fun positiveOrNull(value: String): Int? =
            value
                .trim()
                .takeIf { it.isNotEmpty() }
                ?.toIntOrNull()
                ?.takeIf { it > 0 }
        val track = positiveOrNull(trackNumber)
        val disc = positiveOrNull(discNumber)
        if (trackNumber.isNotBlank() && track == null ||
            discNumber.isNotBlank() && disc == null)
            return null
        return TrackMetadataOverride(
            title = title.trim().ifEmpty { null },
            artist = artist.trim().ifEmpty { null },
            album = album.trim().ifEmpty { null },
            trackNumber = track,
            discNumber = disc,
        )
    }

    /** Factories for drafts shown by the editor. */
    public companion object {
        /**
         * Creates a draft from persisted corrections, leaving nulls blank.
         *
         * @param data scanned tags and current persisted corrections.
         */
        public fun from(data: TrackMetadataEditorData): TrackMetadataDraft =
            TrackMetadataDraft(
                title = data.overrides.title.orEmpty(),
                artist = data.overrides.artist.orEmpty(),
                album = data.overrides.album.orEmpty(),
                trackNumber = data.overrides.trackNumber?.toString().orEmpty(),
                discNumber = data.overrides.discNumber?.toString().orEmpty(),
            )
    }
}

/**
 * Shows scanned tags separately from the editable app-local corrections.
 *
 * @param data scanned tags and persisted corrections, or null after a failed
 *   load.
 * @param draft current editable values.
 * @param saving whether persistence is in progress.
 * @param failed whether the latest load/save failed.
 * @param onDraft receives edited values.
 * @param onDismiss closes the editor.
 * @param onSave persists a validated correction.
 */
@Composable
public fun TrackMetadataEditDialog(
    data: TrackMetadataEditorData?,
    draft: TrackMetadataDraft,
    saving: Boolean,
    failed: Boolean,
    onDraft: (TrackMetadataDraft) -> Unit,
    onDismiss: () -> Unit,
    onSave: (TrackMetadataOverride) -> Unit,
) {
    val invalidNumberText = stringResource(Res.string.metadata_invalid_number)
    HausDialog(
        title = stringResource(Res.string.metadata_edit_title),
        onDismiss = onDismiss,
        body = {
            if (data != null) {
                MetadataField(
                    stringResource(Res.string.metadata_title),
                    data.scannedTrack.title,
                    draft.title,
                    saving,
                    { onDraft(draft.copy(title = it)) },
                    { onDraft(draft.copy(title = "")) },
                    "metadata-title")
                MetadataField(
                    stringResource(Res.string.metadata_artist),
                    data.scannedTrack.artist.orEmpty(),
                    draft.artist,
                    saving,
                    { onDraft(draft.copy(artist = it)) },
                    { onDraft(draft.copy(artist = "")) },
                    "metadata-artist")
                MetadataField(
                    stringResource(Res.string.metadata_album),
                    data.scannedTrack.album.orEmpty(),
                    draft.album,
                    saving,
                    { onDraft(draft.copy(album = it)) },
                    { onDraft(draft.copy(album = "")) },
                    "metadata-album")
                MetadataField(
                    stringResource(Res.string.metadata_track_number),
                    data.scannedTrack.trackNumber?.toString().orEmpty(),
                    draft.trackNumber,
                    saving,
                    { onDraft(draft.copy(trackNumber = it)) },
                    { onDraft(draft.copy(trackNumber = "")) },
                    "metadata-track-number")
                MetadataField(
                    stringResource(Res.string.metadata_disc_number),
                    data.scannedTrack.discNumber?.toString().orEmpty(),
                    draft.discNumber,
                    saving,
                    { onDraft(draft.copy(discNumber = it)) },
                    { onDraft(draft.copy(discNumber = "")) },
                    "metadata-disc-number")
                Button(
                    onClick = { onDraft(TrackMetadataDraft()) },
                    enabled = !saving && draft != TrackMetadataDraft(),
                    modifier = Modifier.testTag("metadata-restore-all"),
                ) {
                    Text(stringResource(Res.string.metadata_restore_all))
                }
            }
            if (data != null && draft.toOverrideOrNull() == null)
                Text(
                    invalidNumberText,
                    color = HausColors.current.pulse,
                    modifier =
                        Modifier.semantics {
                            error(invalidNumberText)
                        },
                )
            if (failed)
                Text(
                    stringResource(Res.string.metadata_failure),
                    color = HausColors.current.pulse)
        },
        actions = {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(44.dp)) {
                            Text(stringResource(Res.string.metadata_cancel))
                        }
                    if (data != null)
                        Button(
                            onClick = { draft.toOverrideOrNull()?.let(onSave) },
                            enabled =
                                !saving && draft.toOverrideOrNull() != null,
                            modifier =
                                Modifier.weight(1f)
                                    .height(44.dp)
                                    .testTag("metadata-save"),
                        ) {
                            Text(stringResource(Res.string.metadata_save))
                        }
                }
        },
    )
}

@Composable
private fun MetadataField(
    label: String,
    scanned: String,
    value: String,
    saving: Boolean,
    onValue: (String) -> Unit,
    onRestore: () -> Unit,
    tag: String,
) {
    val restoreDescription =
        "$label ${stringResource(Res.string.metadata_restore_field)}"
    Column(
        Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    Res.string.metadata_scanned_value, label, scanned))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(
                        value = value,
                        onValueChange = onValue,
                        modifier =
                            Modifier.weight(1f).testTag(tag).semantics {
                                contentDescription = label
                            },
                        label = label,
                        singleLine = true,
                        enabled = !saving,
                    )
                    Button(
                        onClick = onRestore,
                        enabled = !saving && value.isNotEmpty(),
                        modifier =
                            Modifier.height(44.dp).semantics {
                                contentDescription = restoreDescription
                            }) {
                            Text(
                                stringResource(
                                    Res.string.metadata_restore_field))
                        }
                }
        }
}
