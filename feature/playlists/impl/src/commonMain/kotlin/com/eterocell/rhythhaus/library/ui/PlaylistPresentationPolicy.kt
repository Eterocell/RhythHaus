package com.eterocell.rhythhaus.library.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColorPalette

/**
 * Defines the spacing constants consumed by Shared's retained playlist screens.
 */
internal object PlaylistScreenLayoutPolicy {
    val horizontalPadding: Dp = 20.dp
    val additionalTopPadding: Dp = 0.dp
    val itemSpacing: Dp = 18.dp
}

/** Holds the value-equal visual values for a playlist tab. */
internal data class PlaylistTabPresentation(
    val selectedContainerColor: Color,
    val selectedContentColor: Color,
    val unselectedContainerColor: Color,
    val unselectedContentColor: Color,
    val compactControlHeight: Dp = 40.dp,
    val insideVerticalMargin: Dp = 6.dp,
    val lineHeight: TextUnit = 20.sp,
)

/** Returns the value-equal presentation for [tab] using [palette]. */
internal fun playlistTabPresentation(
    tab: PlaylistTab,
    palette: HausColorPalette,
): PlaylistTabPresentation =
    PlaylistTabPresentation(
        selectedContainerColor = palette.ink,
        selectedContentColor = palette.paper,
        unselectedContainerColor = palette.panel,
        unselectedContentColor = palette.ink,
    )
