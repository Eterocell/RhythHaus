package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausShapes
import com.eterocell.rhythhaus.theme.HausSpacing
import com.eterocell.rhythhaus.theme.HausTypography
import com.eterocell.rhythhaus.ui.hausFocusIndicator
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.library
import rhythhaus.shared.generated.resources.playlists
import rhythhaus.shared.generated.resources.search
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.sidebar_favorites
import rhythhaus.shared.generated.resources.sidebar_recently_added
import rhythhaus.shared.generated.resources.sidebar_recently_played
import top.yukonga.miuix.kmp.basic.Text

internal const val AppNavigationBottomTestTag = "AppNavigationBottom"
internal const val AppNavigationRailTestTag = "AppNavigationRail"
internal const val AppNavigationSidebarTestTag = "AppNavigationSidebar"
internal const val AppNavigationLibraryTestTag = "AppNavigationLibrary"
internal const val AppNavigationSearchTestTag = "AppNavigationSearch"
internal const val AppNavigationPlaylistsTestTag = "AppNavigationPlaylists"
internal const val AppNavigationSettingsTestTag = "AppNavigationSettings"
internal const val AppNavigationFavoritesTestTag = "AppNavigationFavorites"
internal const val AppNavigationRecentlyPlayedTestTag =
    "AppNavigationRecentlyPlayed"
internal const val AppNavigationRecentlyAddedTestTag =
    "AppNavigationRecentlyAdded"

private val BottomNavigationHeight = 64.dp

@Composable
internal fun AppNavigationChrome(
    layout: AppNavigationLayout,
    selectedDestination: LibraryPrimaryDestination?,
    activeBrowseMode: BrowseMode?,
    onNavigate: (LibraryPrimaryDestination) -> Unit,
    onBrowseModeChange: (BrowseMode) -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val libraryLabel = stringResource(Res.string.library)
    val searchLabel = stringResource(Res.string.search)
    val playlistsLabel = stringResource(Res.string.playlists)
    val settingsLabel = stringResource(Res.string.settings)
    val favoritesLabel = stringResource(Res.string.sidebar_favorites)
    val recentlyPlayedLabel = stringResource(Res.string.sidebar_recently_played)
    val recentlyAddedLabel = stringResource(Res.string.sidebar_recently_added)

    when (layout) {
        AppNavigationLayout.Bottom ->
            Row(
                modifier =
                    modifier
                        .fillMaxWidth()
                        .height(BottomNavigationHeight)
                        .background(HausColors.current.paper)
                        .testTag(AppNavigationBottomTestTag)
                        .padding(
                            horizontal = HausSpacing.sm,
                            vertical = HausSpacing.xs,
                        ),
                horizontalArrangement = Arrangement.spacedBy(HausSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PrimaryNavigationItem(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = libraryLabel,
                    icon = Icons.Filled.Home,
                    testTag = AppNavigationLibraryTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Library,
                    vertical = true,
                    onClick = { onNavigate(LibraryPrimaryDestination.Library) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = searchLabel,
                    icon = Icons.Filled.Search,
                    testTag = AppNavigationSearchTestTag,
                    selected =
                        selectedDestination == LibraryPrimaryDestination.Search,
                    vertical = true,
                    onClick = { onNavigate(LibraryPrimaryDestination.Search) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    label = playlistsLabel,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    testTag = AppNavigationPlaylistsTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Playlists,
                    vertical = true,
                    onClick = {
                        onNavigate(LibraryPrimaryDestination.Playlists)
                    },
                )
            }

        AppNavigationLayout.Rail ->
            Column(
                modifier =
                    modifier
                        .width(AppNavigationLayout.Rail.navigationWidthDp.dp)
                        .fillMaxHeight()
                        .background(HausColors.current.paper)
                        .testTag(AppNavigationRailTestTag)
                        .padding(horizontal = 6.dp, vertical = HausSpacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(HausSpacing.sm),
            ) {
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    label = libraryLabel,
                    icon = Icons.Filled.Home,
                    testTag = AppNavigationLibraryTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Library,
                    vertical = true,
                    onClick = { onNavigate(LibraryPrimaryDestination.Library) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    label = searchLabel,
                    icon = Icons.Filled.Search,
                    testTag = AppNavigationSearchTestTag,
                    selected =
                        selectedDestination == LibraryPrimaryDestination.Search,
                    vertical = true,
                    onClick = { onNavigate(LibraryPrimaryDestination.Search) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    label = playlistsLabel,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    testTag = AppNavigationPlaylistsTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Playlists,
                    vertical = true,
                    onClick = {
                        onNavigate(LibraryPrimaryDestination.Playlists)
                    },
                )
                Spacer(Modifier.weight(1f))
                ToolNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    label = settingsLabel,
                    icon = Icons.Filled.Settings,
                    testTag = AppNavigationSettingsTestTag,
                    vertical = true,
                    onClick = onSettings,
                )
            }

        AppNavigationLayout.Sidebar ->
            Column(
                modifier =
                    modifier
                        .width(AppNavigationLayout.Sidebar.navigationWidthDp.dp)
                        .fillMaxHeight()
                        .background(HausColors.current.paper)
                        .testTag(AppNavigationSidebarTestTag)
                        .padding(
                            horizontal = HausSpacing.lg,
                            vertical = 20.dp,
                        ),
                verticalArrangement = Arrangement.spacedBy(HausSpacing.sm),
            ) {
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = libraryLabel,
                    icon = Icons.Filled.Home,
                    testTag = AppNavigationLibraryTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Library,
                    vertical = false,
                    onClick = { onNavigate(LibraryPrimaryDestination.Library) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = searchLabel,
                    icon = Icons.Filled.Search,
                    testTag = AppNavigationSearchTestTag,
                    selected =
                        selectedDestination == LibraryPrimaryDestination.Search,
                    vertical = false,
                    onClick = { onNavigate(LibraryPrimaryDestination.Search) },
                )
                PrimaryNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = playlistsLabel,
                    icon = Icons.AutoMirrored.Filled.QueueMusic,
                    testTag = AppNavigationPlaylistsTestTag,
                    selected =
                        selectedDestination ==
                            LibraryPrimaryDestination.Playlists,
                    vertical = false,
                    onClick = {
                        onNavigate(LibraryPrimaryDestination.Playlists)
                    },
                )
                Box(
                    Modifier.fillMaxWidth()
                        .height(1.dp)
                        .background(HausColors.current.line))
                BrowseNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = favoritesLabel,
                    icon = Icons.Filled.Favorite,
                    testTag = AppNavigationFavoritesTestTag,
                    selected = activeBrowseMode == BrowseMode.Favorites,
                    onClick = { onBrowseModeChange(BrowseMode.Favorites) },
                )
                BrowseNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = recentlyPlayedLabel,
                    icon = Icons.Filled.History,
                    testTag = AppNavigationRecentlyPlayedTestTag,
                    selected = activeBrowseMode == BrowseMode.RecentlyPlayed,
                    onClick = {
                        onBrowseModeChange(BrowseMode.RecentlyPlayed)
                    },
                )
                BrowseNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = recentlyAddedLabel,
                    icon = Icons.Filled.NewReleases,
                    testTag = AppNavigationRecentlyAddedTestTag,
                    selected = activeBrowseMode == BrowseMode.RecentlyAdded,
                    onClick = {
                        onBrowseModeChange(BrowseMode.RecentlyAdded)
                    },
                )
                Spacer(Modifier.weight(1f))
                ToolNavigationItem(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    label = settingsLabel,
                    icon = Icons.Filled.Settings,
                    testTag = AppNavigationSettingsTestTag,
                    vertical = false,
                    onClick = onSettings,
                )
            }
    }
}

@Composable
private fun PrimaryNavigationItem(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    testTag: String,
    selected: Boolean,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    NavigationItem(
        modifier = modifier,
        label = label,
        icon = icon,
        testTag = testTag,
        selected = selected,
        vertical = vertical,
        onClick = onClick,
    )
}

@Composable
private fun BrowseNavigationItem(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    testTag: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    NavigationItem(
        modifier = modifier,
        label = label,
        icon = icon,
        testTag = testTag,
        selected = selected,
        vertical = false,
        onClick = onClick,
    )
}

@Composable
private fun NavigationItem(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    testTag: String,
    selected: Boolean,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (selected) HausColors.current.pulse else HausColors.current.muted
    val itemModifier =
        modifier
            .clip(RoundedCornerShape(HausShapes.control))
            .background(
                if (selected) HausColors.current.panelStrong
                else Color.Transparent)
            .hausFocusIndicator(shape = RoundedCornerShape(HausShapes.control))
            .selectable(
                selected = selected,
                role = Role.Tab,
                onClick = onClick,
            )
            .testTag(testTag)
            .padding(
                horizontal = if (vertical) 4.dp else 12.dp,
                vertical = if (vertical) 5.dp else 0.dp,
            )
    if (vertical) {
        Column(
            modifier = itemModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(
                text = label,
                color = contentColor,
                fontSize = HausTypography.label.fontSize,
                fontWeight = HausTypography.label.fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Row(
            modifier = itemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Text(
                text = label,
                color = contentColor,
                fontSize = HausTypography.secondary.fontSize,
                fontWeight =
                    if (selected) FontWeight.SemiBold
                    else HausTypography.secondary.fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ToolNavigationItem(
    modifier: Modifier,
    label: String,
    icon: ImageVector,
    testTag: String,
    vertical: Boolean,
    onClick: () -> Unit,
) {
    val itemModifier =
        modifier
            .clip(RoundedCornerShape(HausShapes.control))
            .hausFocusIndicator(shape = RoundedCornerShape(HausShapes.control))
            .clickable(role = Role.Button, onClick = onClick)
            .testTag(testTag)
            .padding(
                horizontal = if (vertical) 4.dp else 12.dp,
                vertical = if (vertical) 5.dp else 0.dp,
            )
    if (vertical) {
        Column(
            modifier = itemModifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = HausColors.current.muted)
            Text(
                text = label,
                color = HausColors.current.muted,
                fontSize = HausTypography.label.fontSize,
                fontWeight = HausTypography.label.fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    } else {
        Row(
            modifier = itemModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = HausColors.current.muted)
            Text(
                text = label,
                color = HausColors.current.muted,
                fontSize = HausTypography.secondary.fontSize,
                fontWeight = HausTypography.secondary.fontWeight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
