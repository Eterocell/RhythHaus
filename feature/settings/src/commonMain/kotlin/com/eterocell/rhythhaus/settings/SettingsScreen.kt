package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.HausDialog
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import org.jetbrains.compose.resources.stringResource
import rhythhaus.feature.settings.generated.resources.Res
import rhythhaus.feature.settings.generated.resources.about
import rhythhaus.feature.settings.generated.resources.appearance
import rhythhaus.feature.settings.generated.resources.configured_folders
import rhythhaus.feature.settings.generated.resources.manage_music
import rhythhaus.feature.settings.generated.resources.remove_folder
import rhythhaus.feature.settings.generated.resources.remove_folder_message
import rhythhaus.feature.settings.generated.resources.remove_source_format
import rhythhaus.feature.settings.generated.resources.rescan_source_format
import rhythhaus.feature.settings.generated.resources.source_access_available
import rhythhaus.feature.settings.generated.resources.source_access_lost
import rhythhaus.feature.settings.generated.resources.source_last_scanned
import rhythhaus.feature.settings.generated.resources.source_never_scanned
import rhythhaus.feature.settings.generated.resources.source_status_format
import rhythhaus.feature.settings.generated.resources.theme_dark_description
import rhythhaus.feature.settings.generated.resources.theme_dark_label
import rhythhaus.feature.settings.generated.resources.theme_light_description
import rhythhaus.feature.settings.generated.resources.theme_light_label
import rhythhaus.feature.settings.generated.resources.theme_system_description
import rhythhaus.feature.settings.generated.resources.theme_system_label
import rhythhaus.feature.settings.generated.resources.unnamed_folder
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

/** Shared-owned wording and actions injected into [SettingsScreen]. */
public data class SettingsSharedLabels(
    /** Settings route title resolved by Shared. */
    public val title: String,
    /** Shared-owned add-folder action wording. */
    public val addMusicFolder: String,
    /** Shared-owned unavailable-picker wording. */
    public val folderPickerUnavailable: String,
    /** Shared-owned clear-library action wording. */
    public val clearLibrary: String,
    /** Shared-owned generic cancellation wording. */
    public val cancel: String,
    /** Shared-owned generic removal wording. */
    public val remove: String,
)

/**
 * Immutable, feature-safe rendering projection for one authoritative Library
 * source.
 */
public data class SettingsSourceItem(
    /** Stable source identifier returned to Shared callbacks. */
    public val id: String,
    /** User-visible source name already selected by Shared. */
    public val displayName: String,
    /** Whether the source remains accessible to the platform. */
    public val accessAvailable: Boolean,
    /** Whether the source has completed at least one scan. */
    public val hasBeenScanned: Boolean,
)

internal enum class SettingsSourceAccess {
    Available,
    Lost,
}

internal enum class SettingsSourceScan {
    NeverScanned,
    HasBeenScanned,
}

internal data class SettingsSourcePresentation(
    val displayNameFallbackRequired: Boolean,
    val access: SettingsSourceAccess,
    val scan: SettingsSourceScan,
)

internal fun SettingsSourceItem.presentation(): SettingsSourcePresentation =
    SettingsSourcePresentation(
        displayNameFallbackRequired = displayName.isBlank(),
        access =
            if (accessAvailable) SettingsSourceAccess.Available
            else SettingsSourceAccess.Lost,
        scan =
            if (hasBeenScanned) SettingsSourceScan.HasBeenScanned
            else SettingsSourceScan.NeverScanned)

internal data class SettingsLayoutPolicy(
    val horizontalPagePadding: Dp,
    val verticalPagePadding: Dp,
    val itemSpacing: Dp,
    val bottomContentPadding: Dp,
    val topBarTitlePadding: Dp,
    val topBarNavigationIconPadding: Dp,
    val appearanceHorizontalInsidePadding: Dp,
    val appearanceVerticalInsidePadding: Dp,
)

internal val CompactSettingsLayoutPolicy =
    SettingsLayoutPolicy(16.dp, 8.dp, 12.dp, 8.dp, 0.dp, 0.dp, 0.dp, 16.dp)
internal const val SettingsPickerTestTag = "settings-picker"
internal const val SettingsClearTestTag = "settings-clear"
internal const val SettingsAboutTestTag = "settings-about"
internal const val SettingsRescanPrefix = "settings-rescan-"
internal const val SettingsRemovePrefix = "settings-remove-"
internal const val SettingsRemoveConfirmTestTag = "settings-remove-confirm"
internal const val SettingsRemoveDismissTestTag = "settings-remove-dismiss"
internal const val SettingsRootTestTag = "settings-root"
internal const val SettingsPointerShieldTestTag = "settings-pointer-shield"
internal const val SettingsListTestTag = "settings-list"
internal const val SettingsThemeTestTag = "settings-theme"

/**
 * Renders Settings from scalar state, source projections, callbacks, and
 * caller-owned slots. The picker obeys [sourcePickerActionVisible],
 * [sourcePickerAvailable], and [mutationsEnabled]; the clear action is rendered
 * only for [hasImportedTracks], requests Shared dialog state through
 * [onRequestClearLibrary] only when enabled, and renders [clearLibraryDialog]
 * only when supplied. Source callbacks emit IDs only; Shared resolves and
 * guards them at invocation.
 */
@Composable
public fun SettingsScreen(
    labels: SettingsSharedLabels,
    currentThemeMode: RhythHausThemeMode,
    sources: List<SettingsSourceItem>,
    sourcePickerActionVisible: Boolean,
    sourcePickerAvailable: Boolean,
    importMessage: String?,
    mutationsEnabled: Boolean,
    hasImportedTracks: Boolean,
    playlistBackupContent: @Composable () -> Unit,
    activeScanContent: (@Composable () -> Unit)?,
    clearLibraryDialog: (@Composable () -> Unit)?,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onAddMusicFolder: () -> Unit,
    onRescanSource: (String) -> Unit,
    onRemoveSource: (String) -> Unit,
    onRequestClearLibrary: () -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sourcePendingRemoval by remember {
        mutableStateOf<SettingsSourceItem?>(null)
    }
    val policy = CompactSettingsLayoutPolicy
    Box(
        modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .testTag(SettingsRootTestTag)) {
            Box(
                Modifier.matchParentSize()
                    .testTag(SettingsPointerShieldTestTag)
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                awaitPointerEvent(PointerEventPass.Initial)
                                    .changes
                                    .forEach {
                                        it.consume()
                                    }
                            }
                        }
                    })
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = HausColors.current.paper) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = HausColors.current.paper,
                        contentWindowInsets = WindowInsets(0.dp)) {
                            LazyColumn(
                                modifier =
                                    Modifier.fillMaxSize()
                                        .testTag(SettingsListTestTag)
                                        .safeContentPadding()
                                        .padding(
                                            horizontal =
                                                policy.horizontalPagePadding,
                                            vertical =
                                                policy.verticalPagePadding),
                                contentPadding =
                                    PaddingValues(
                                        bottom = policy.bottomContentPadding),
                                verticalArrangement =
                                    Arrangement.spacedBy(policy.itemSpacing),
                            ) {
                                item {
                                    RhythHausTopAppBar(
                                        labels.title,
                                        onDismiss,
                                        titlePadding =
                                            policy.topBarTitlePadding,
                                        navigationIconPadding =
                                            policy.topBarNavigationIconPadding)
                                }
                                item {
                                    AppearanceDropdown(
                                        currentThemeMode,
                                        onThemeModeSelected,
                                        policy
                                            .appearanceHorizontalInsidePadding,
                                        policy.appearanceVerticalInsidePadding)
                                }
                                item {
                                    Text(
                                        stringResource(Res.string.manage_music),
                                        color = HausColors.current.ink,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black)
                                }
                                item { playlistBackupContent() }
                                activeScanContent?.let { content ->
                                    item { content() }
                                }
                                if (sources.isNotEmpty()) {
                                    item {
                                        Text(
                                            stringResource(
                                                Res.string.configured_folders),
                                            color = HausColors.current.muted,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold)
                                    }
                                    items(
                                        sources,
                                        key = SettingsSourceItem::id) { source
                                            ->
                                            ConfiguredSourceRow(
                                                source,
                                                mutationsEnabled,
                                                { onRescanSource(source.id) }) {
                                                    sourcePendingRemoval =
                                                        source
                                                }
                                        }
                                }
                                if (sourcePickerActionVisible)
                                    item {
                                        val pickerEnabled =
                                            sourcePickerAvailable &&
                                                mutationsEnabled
                                        val pickerLabel =
                                            if (sourcePickerAvailable) {
                                                labels.addMusicFolder
                                            } else {
                                                labels.folderPickerUnavailable
                                            }
                                        val pickerModifier =
                                            Modifier.fillMaxWidth()
                                                .height(48.dp)
                                        if (!pickerEnabled) {
                                            Box(
                                                pickerModifier
                                                    .testTag(
                                                        SettingsPickerTestTag)
                                                    .clearAndSetSemantics {
                                                        contentDescription =
                                                            pickerLabel
                                                        disabled()
                                                    }) {
                                                    Button(
                                                        onClick =
                                                            onAddMusicFolder,
                                                        enabled = false,
                                                        modifier =
                                                            pickerModifier,
                                                        cornerRadius = 16.dp,
                                                        colors =
                                                            ButtonDefaults
                                                                .buttonColors(
                                                                    color =
                                                                        HausColors
                                                                            .current
                                                                            .ink,
                                                                    contentColor =
                                                                        HausColors
                                                                            .current
                                                                            .paper,
                                                                    disabledColor =
                                                                        HausColors
                                                                            .current
                                                                            .muted
                                                                            .copy(
                                                                                alpha =
                                                                                    .28f),
                                                                    disabledContentColor =
                                                                        HausColors
                                                                            .current
                                                                            .muted)) {
                                                            Text(
                                                                pickerLabel,
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Black)
                                                        }
                                                }
                                        } else
                                            Button(
                                                onClick = onAddMusicFolder,
                                                enabled = true,
                                                modifier =
                                                    pickerModifier.testTag(
                                                        SettingsPickerTestTag),
                                                cornerRadius = 16.dp,
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        color =
                                                            HausColors.current
                                                                .ink,
                                                        contentColor =
                                                            HausColors.current
                                                                .paper,
                                                        disabledColor =
                                                            HausColors.current
                                                                .muted
                                                                .copy(
                                                                    alpha =
                                                                        .28f),
                                                        disabledContentColor =
                                                            HausColors.current
                                                                .muted)) {
                                                    Text(
                                                        pickerLabel,
                                                        fontWeight =
                                                            FontWeight.Black)
                                                }
                                    }
                                importMessage?.let { message ->
                                    item {
                                        Text(
                                            message,
                                            color = HausColors.current.muted,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (hasImportedTracks)
                                    item {
                                        val clearModifier =
                                            Modifier.fillMaxWidth()
                                                .height(48.dp)
                                        if (!mutationsEnabled) {
                                            Box(
                                                clearModifier
                                                    .testTag(
                                                        SettingsClearTestTag)
                                                    .clearAndSetSemantics {
                                                        contentDescription =
                                                            labels.clearLibrary
                                                        disabled()
                                                    }) {
                                                    Button(
                                                        onClick =
                                                            onRequestClearLibrary,
                                                        enabled = false,
                                                        modifier =
                                                            clearModifier,
                                                        cornerRadius = 18.dp,
                                                        colors =
                                                            ButtonDefaults
                                                                .buttonColors(
                                                                    color =
                                                                        HausColors
                                                                            .current
                                                                            .pulse
                                                                            .copy(
                                                                                alpha =
                                                                                    .15f),
                                                                    contentColor =
                                                                        HausColors
                                                                            .current
                                                                            .pulse)) {
                                                            Text(
                                                                labels
                                                                    .clearLibrary,
                                                                fontWeight =
                                                                    FontWeight
                                                                        .Black)
                                                        }
                                                }
                                        } else
                                            Button(
                                                onClick = onRequestClearLibrary,
                                                enabled = true,
                                                modifier =
                                                    clearModifier.testTag(
                                                        SettingsClearTestTag),
                                                cornerRadius = 18.dp,
                                                colors =
                                                    ButtonDefaults.buttonColors(
                                                        color =
                                                            HausColors.current
                                                                .pulse
                                                                .copy(
                                                                    alpha =
                                                                        .15f),
                                                        contentColor =
                                                            HausColors.current
                                                                .pulse)) {
                                                    Text(
                                                        labels.clearLibrary,
                                                        fontWeight =
                                                            FontWeight.Black)
                                                }
                                    }
                                item { AboutRow(onAboutClick) }
                            }
                        }
                }
            clearLibraryDialog?.invoke()
            sourcePendingRemoval?.let { source ->
                RemoveSourceDialog(
                    source,
                    labels,
                    mutationsEnabled,
                    { sourcePendingRemoval = null }) {
                        onRemoveSource(source.id)
                        sourcePendingRemoval = null
                    }
            }
        }
}

@Composable
private fun AboutRow(onClick: () -> Unit) =
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .testTag(SettingsAboutTestTag)
            .semantics(mergeDescendants = true) { role = Role.Button }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Info,
            null,
            tint = HausColors.current.ink,
            modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            stringResource(Res.string.about),
            Modifier.weight(1f),
            HausColors.current.ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold)
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            null,
            tint = HausColors.current.muted,
            modifier = Modifier.size(22.dp))
    }

@Composable
private fun ConfiguredSourceRow(
    source: SettingsSourceItem,
    mutationsEnabled: Boolean,
    onRescan: () -> Unit,
    onRemove: () -> Unit
) {
    val presentation = source.presentation()
    val displayName =
        if (presentation.displayNameFallbackRequired) {
            stringResource(Res.string.unnamed_folder)
        } else source.displayName
    val access =
        stringResource(
            if (presentation.access == SettingsSourceAccess.Available) {
                Res.string.source_access_available
            } else Res.string.source_access_lost)
    val scan =
        stringResource(
            if (presentation.scan == SettingsSourceScan.HasBeenScanned) {
                Res.string.source_last_scanned
            } else Res.string.source_never_scanned)
    val alpha = if (mutationsEnabled) 1f else .42f
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(
                HausColors.current.panel.copy(alpha = .54f),
                RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        displayName,
                        color = HausColors.current.ink,
                        fontSize = 15.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                    Text(
                        stringResource(
                            Res.string.source_status_format, access, scan),
                        color = HausColors.current.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2)
                }
            IconButton(
                onRescan,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.testTag(SettingsRescanPrefix + source.id)
                        .semantics {
                            if (!mutationsEnabled) disabled()
                        },
                backgroundColor = Color.Transparent,
                minWidth = 44.dp,
                minHeight = 44.dp) {
                    Icon(
                        Icons.Default.Refresh,
                        stringResource(
                            Res.string.rescan_source_format, displayName),
                        tint = HausColors.current.ink.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp))
                }
            IconButton(
                onRemove,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.testTag(SettingsRemovePrefix + source.id)
                        .semantics {
                            if (!mutationsEnabled) disabled()
                        },
                backgroundColor = Color.Transparent,
                minWidth = 44.dp,
                minHeight = 44.dp) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        stringResource(
                            Res.string.remove_source_format, displayName),
                        tint = HausColors.current.pulse.copy(alpha = alpha),
                        modifier = Modifier.size(20.dp))
                }
        }
}

@Composable
private fun RemoveSourceDialog(
    source: SettingsSourceItem,
    labels: SettingsSharedLabels,
    mutationsEnabled: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val fullName =
        source.displayName.ifBlank { stringResource(Res.string.unnamed_folder) }
    val visibleName =
        if (fullName.length <= 64) fullName else fullName.take(63) + "…"
    HausDialog(
        title = stringResource(Res.string.remove_folder),
        onDismiss = onDismiss,
        dismissLabel = labels.cancel,
        body = {
            Text(
                stringResource(Res.string.remove_folder),
                color = HausColors.current.ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black)
            Spacer(Modifier.height(12.dp))
            Text(
                visibleName,
                Modifier.clearAndSetSemantics { contentDescription = fullName },
                color = HausColors.current.ink,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(Res.string.remove_folder_message),
                color = HausColors.current.muted,
                fontSize = 14.sp,
                lineHeight = 20.sp)
        },
        actions = {
            Spacer(Modifier.weight(1f))
            Button(
                onDismiss,
                modifier =
                    Modifier.height(44.dp)
                        .testTag(SettingsRemoveDismissTestTag),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.muted.copy(alpha = .15f),
                        contentColor = HausColors.current.muted)) {
                    Text(
                        labels.cancel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
            Spacer(Modifier.width(12.dp))
            Button(
                onConfirm,
                enabled = mutationsEnabled,
                modifier =
                    Modifier.height(44.dp)
                        .testTag(SettingsRemoveConfirmTestTag),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.pulse,
                        contentColor = HausColors.current.paper,
                        disabledColor =
                            HausColors.current.muted.copy(alpha = .28f),
                        disabledContentColor = HausColors.current.muted)) {
                    Text(
                        labels.remove,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium)
                }
        })
}

@Composable
private fun AppearanceDropdown(
    current: RhythHausThemeMode,
    onSelected: (RhythHausThemeMode) -> Unit,
    horizontal: Dp,
    vertical: Dp
) {
    val options = RhythHausThemeMode.settingsOptions
    OverlayDropdownPreference(
        items = options.map { it.label() },
        selectedIndex = options.indexOf(current).coerceAtLeast(0),
        title = stringResource(Res.string.appearance),
        summary = current.description(),
        modifier = Modifier.fillMaxWidth().testTag(SettingsThemeTestTag),
        insideMargin =
            PaddingValues(horizontal = horizontal, vertical = vertical),
        renderInRootScaffold = false,
        onSelectedIndexChange = { options.getOrNull(it)?.let(onSelected) })
}

@Composable
private fun RhythHausThemeMode.label(): String =
    stringResource(
        when (this) {
            RhythHausThemeMode.System -> Res.string.theme_system_label
            RhythHausThemeMode.Light -> Res.string.theme_light_label
            RhythHausThemeMode.Dark -> Res.string.theme_dark_label
        })

@Composable
private fun RhythHausThemeMode.description(): String =
    stringResource(
        when (this) {
            RhythHausThemeMode.System -> Res.string.theme_system_description
            RhythHausThemeMode.Light -> Res.string.theme_light_description
            RhythHausThemeMode.Dark -> Res.string.theme_dark_description
        })
