package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.HausButton as Button
import com.eterocell.rhythhaus.ui.HausDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.clear_library_counts_format
import rhythhaus.shared.generated.resources.clear_library_message
import rhythhaus.shared.generated.resources.remove
import rhythhaus.shared.generated.resources.remove_missing_confirmation_count
import rhythhaus.shared.generated.resources.remove_missing_confirmation_message
import rhythhaus.shared.generated.resources.remove_missing_confirmation_title
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun AnimatedClearLibraryDialogRoute(
    sourceCount: Int,
    trackCount: Int,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onClearLibrary: () -> Unit,
) {
    val dialogTitle = stringResource(Res.string.clear_library)
    HausDialog(
        title = dialogTitle,
        onDismiss = onDismiss,
        body = {
            Text(
                text = dialogTitle,
                color = HausColors.current.ink,
                style = HausTypography.section,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text =
                    stringResource(
                        Res.string.clear_library_counts_format,
                        sourceCount,
                        trackCount),
                modifier = Modifier.testTag("settings-clear-counts"),
                color = HausColors.current.danger,
                style = HausTypography.secondary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.clear_library_message),
                color = HausColors.current.muted,
                style = HausTypography.secondary,
            )
        },
        actions = {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .testTag("settings-clear-dismiss"),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.muted.copy(alpha = 0.15f),
                        contentColor = HausColors.current.muted,
                    ),
            ) {
                Text(
                    stringResource(Res.string.cancel),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onClearLibrary,
                enabled = confirmEnabled,
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .testTag("settings-clear-confirm"),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.danger,
                        contentColor = HausColors.current.onDanger,
                        disabledColor = HausColors.current.dangerPanel,
                        disabledContentColor = HausColors.current.danger,
                    ),
            ) {
                Text(
                    stringResource(Res.string.clear),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium)
            }
        },
    )
}

@Composable
internal fun AnimatedRemoveMissingTracksDialogRoute(
    sourceName: String,
    trackCount: Int,
    confirmEnabled: Boolean,
    onDismiss: () -> Unit,
    onRemoveMissingTracks: () -> Unit,
) {
    val dialogTitle =
        stringResource(Res.string.remove_missing_confirmation_title)
    HausDialog(
        title = dialogTitle,
        onDismiss = onDismiss,
        body = {
            Text(
                text = dialogTitle,
                color = HausColors.current.ink,
                style = HausTypography.section,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = sourceName,
                modifier = Modifier.testTag("settings-remove-missing-source"),
                color = HausColors.current.ink,
                style = HausTypography.secondary,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    stringResource(
                        Res.string.remove_missing_confirmation_count,
                        trackCount),
                modifier = Modifier.testTag("settings-remove-missing-count"),
                color = HausColors.current.danger,
                style = HausTypography.secondary,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text =
                    stringResource(
                        Res.string.remove_missing_confirmation_message),
                color = HausColors.current.muted,
                style = HausTypography.secondary,
            )
        },
        actions = {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .testTag("settings-remove-missing-dismiss"),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.muted.copy(alpha = 0.15f),
                        contentColor = HausColors.current.muted,
                    ),
            ) {
                Text(
                    stringResource(Res.string.cancel),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.width(12.dp))
            Button(
                onClick = onRemoveMissingTracks,
                enabled = confirmEnabled,
                modifier =
                    Modifier.heightIn(min = 48.dp)
                        .testTag("settings-remove-missing-confirm"),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.danger,
                        contentColor = HausColors.current.onDanger,
                        disabledColor =
                            HausColors.current.muted.copy(alpha = .28f),
                        disabledContentColor = HausColors.current.muted,
                    ),
            ) {
                Text(
                    stringResource(Res.string.remove),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        },
    )
}
