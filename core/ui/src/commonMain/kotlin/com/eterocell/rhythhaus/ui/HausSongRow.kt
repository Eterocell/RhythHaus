package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausMetrics
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import com.eterocell.rhythhaus.theme.HausTypography
import top.yukonga.miuix.kmp.basic.Checkbox
import top.yukonga.miuix.kmp.basic.Text

/**
 * Shell-provided density used by row adapters that do not own layout policy.
 */
public val LocalHausDesktopDensity: ProvidableCompositionLocal<Boolean> =
    compositionLocalOf {
        false
    }

/**
 * Shared, domain-neutral presentation for one song occurrence.
 *
 * Feature adapters own the artwork, labels, primary activation and trailing
 * actions. This primitive owns the continuous row surface, selection semantics,
 * and the primary/long-press gesture boundary. In selection mode, the checkbox
 * invokes the same primary action as the row; [onSelect] is reserved for
 * long-press selection behavior.
 *
 * [rowContentDescription] is the row's accessible name and defaults to the
 * visible title. Adapters whose rows carry more identifying metadata than the
 * title may pass the richer description, independently of [selectLabel], which
 * labels only the long-press Select action.
 */
@Composable
public fun HausSongRow(
    title: String,
    subtitle: String,
    selectionMode: Boolean,
    selected: Boolean,
    playing: Boolean,
    interactive: Boolean,
    onActivate: () -> Unit,
    onSelect: (() -> Unit)?,
    selectLabel: String?,
    rowContentDescription: String = title,
    playingLabel: String?,
    artwork: @Composable () -> Unit,
    indicator: @Composable () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val desktopDensity = LocalHausDesktopDensity.current
    val rowClickModifier =
        when {
            !interactive -> Modifier
            onSelect != null ->
                Modifier.hausCombinedClickable(
                    onClick = onActivate,
                    onLongClick = onSelect,
                    onLongClickLabel = selectLabel ?: title,
                )
            else -> Modifier.hausClickable(onActivate)
        }
    val activeBackground =
        when {
            selected -> HausColors.current.panelStrong
            playing -> HausColors.current.panel.copy(alpha = 0.58f)
            else -> Color.Transparent
        }
    val rowHeight =
        if (desktopDensity) HausMetrics.desktopSongRowMinHeight
        else HausMetrics.compactSongRowMinHeight
    val artworkSize =
        if (desktopDensity) HausMetrics.desktopArtworkMinSize
        else HausMetrics.compactArtworkMinSize

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier =
                Modifier.fillMaxWidth()
                    .heightIn(min = rowHeight)
                    .background(activeBackground),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(HausSpacing.md),
        ) {
            Row(
                modifier =
                    Modifier.weight(1f)
                        .heightIn(min = rowHeight)
                        .semantics {
                            contentDescription = rowContentDescription
                            if (selectionMode && onSelect != null) {
                                toggleableState =
                                    if (selected) ToggleableState.On
                                    else ToggleableState.Off
                            } else if (interactive) {
                                role = Role.Button
                            }
                            if (playing && playingLabel != null) {
                                stateDescription = playingLabel
                            }
                            if (!interactive) disabled()
                        }
                        .then(rowClickModifier)
                        .padding(
                            horizontal = HausSpacing.md,
                            vertical =
                                if (desktopDensity) 4.dp else HausSpacing.sm,
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HausSpacing.md),
            ) {
                if (selectionMode && onSelect != null) {
                    Box(
                        modifier =
                            Modifier.size(48.dp)
                                .then(
                                    if (interactive) {
                                        Modifier.hausFocusIndicator(
                                            shape =
                                                RoundedCornerShape(
                                                    HausShapes.control),
                                            includeDescendants = true,
                                        )
                                    } else {
                                        Modifier
                                    },
                                ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Checkbox(
                            state =
                                if (selected) ToggleableState.On
                                else ToggleableState.Off,
                            onClick = { if (interactive) onActivate() },
                            enabled = interactive,
                            modifier =
                                Modifier.size(48.dp)
                                    .then(
                                        if (interactive) Modifier
                                        else
                                            Modifier.clearAndSetSemantics {
                                                role = Role.Checkbox
                                                toggleableState =
                                                    if (selected)
                                                        ToggleableState.On
                                                    else ToggleableState.Off
                                                disabled()
                                            },
                                    ),
                        )
                    }
                }
                Box(
                    Modifier.size(artworkSize)
                        .clip(RoundedCornerShape(HausShapes.artwork))
                        .background(HausColors.current.panel),
                    contentAlignment = Alignment.Center,
                ) {
                    artwork()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(HausSpacing.xs),
                ) {
                    Text(
                        text = title,
                        color = HausColors.current.ink,
                        style = HausTypography.title,
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            color = HausColors.current.muted,
                            style = HausTypography.secondary,
                            maxLines = 1,
                        )
                    }
                }
                if (playing) indicator()
            }
            trailing()
        }
        androidx.compose.foundation.layout.Box(
            Modifier.fillMaxWidth()
                .height(1.dp)
                .background(HausColors.current.line.copy(alpha = 0.48f)),
        )
    }
}

/**
 * Formats the artist and album subtitle shared by song-row adapters. Blank
 * metadata is omitted instead of leaving a dangling separator.
 */
public fun songArtistAlbumSubtitle(artist: String?, album: String?): String {
    val visibleArtist = artist?.takeIf { it.isNotBlank() }
    val visibleAlbum = album?.takeIf { it.isNotBlank() }
    return when {
        visibleArtist == null -> visibleAlbum.orEmpty()
        visibleAlbum == null -> visibleArtist
        else -> "$visibleArtist · $visibleAlbum"
    }
}
