package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.LibrarySourceAccessStatus
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ui.AnimatedClearLibraryDialogRoute
import com.eterocell.rhythhaus.library.ui.ScanningCard
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.appearance
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.configured_folders
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.manage_music
import rhythhaus.shared.generated.resources.remove
import rhythhaus.shared.generated.resources.remove_folder
import rhythhaus.shared.generated.resources.remove_folder_message
import rhythhaus.shared.generated.resources.remove_source_format
import rhythhaus.shared.generated.resources.rescan_source_format
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.source_access_available
import rhythhaus.shared.generated.resources.source_access_lost
import rhythhaus.shared.generated.resources.source_last_scanned
import rhythhaus.shared.generated.resources.source_never_scanned
import rhythhaus.shared.generated.resources.source_status_format
import rhythhaus.shared.generated.resources.theme_dark_description
import rhythhaus.shared.generated.resources.theme_dark_label
import rhythhaus.shared.generated.resources.theme_light_description
import rhythhaus.shared.generated.resources.theme_light_label
import rhythhaus.shared.generated.resources.theme_system_description
import rhythhaus.shared.generated.resources.theme_system_label
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.preference.OverlayDropdownPreference

@Composable
fun SettingsScreen(
    sources: List<LibrarySource>,
    folderPickerLauncher: PlatformFolderPickerLauncher,
    sourcePickerActionVisible: Boolean,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    hasImportedTracks: Boolean,
    currentThemeMode: RhythHausThemeMode,
    clearLibraryDialogBackdrop: LayerBackdrop?,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onRescanSource: (LibrarySource) -> Unit,
    onRemoveSource: (LibrarySource) -> Unit,
    onCancelScan: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showClearLibraryDialog by remember { mutableStateOf(false) }
    var sourcePendingRemoval by remember { mutableStateOf<LibrarySource?>(null) }
    val mutationsEnabled = sourceMutationsEnabled(scanProgress)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .clickable(enabled = false, onClick = {}),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = HausColors.current.paper,
                contentWindowInsets = WindowInsets(0.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    item {
                        RhythHausTopAppBar(
                            title = stringResource(Res.string.settings),
                            onBack = onDismiss,
                        )
                    }

                    item {
                        AppearanceDropdown(
                            currentThemeMode = currentThemeMode,
                            onThemeModeSelected = onThemeModeSelected,
                        )
                    }

                    item {
                        Text(
                            text = stringResource(Res.string.manage_music),
                            color = HausColors.current.ink,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }

                    if (scanProgress?.isActive == true) {
                        item {
                            val ss = scanProgress.session!!
                            ScanningCard(
                                foldersVisited = ss.foldersVisited,
                                filesVisited = ss.filesVisited,
                                tracksAdded = ss.tracksAdded,
                                latestItem = scanProgress.latestItem,
                                onCancel = onCancelScan,
                            )
                        }
                    }

                    if (sources.isNotEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.configured_folders),
                                color = HausColors.current.muted,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        items(sources, key = LibrarySource::id) { source ->
                            ConfiguredSourceRow(
                                source = source,
                                mutationsEnabled = mutationsEnabled,
                                onRescan = { onRescanSource(source) },
                                onRemove = { sourcePendingRemoval = source },
                            )
                        }
                    }

                    if (sourcePickerActionVisible) {
                        item {
                            Button(
                                onClick = folderPickerLauncher::launch,
                                enabled = folderPickerLauncher.isAvailable && mutationsEnabled,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                cornerRadius = 16.dp,
                                colors = ButtonDefaults.buttonColors(
                                    color = HausColors.current.ink,
                                    contentColor = HausColors.current.paper,
                                    disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                                    disabledContentColor = HausColors.current.muted,
                                ),
                            ) {
                                Text(
                                    text = if (folderPickerLauncher.isAvailable) stringResource(Res.string.add_music_folder) else stringResource(
                                        Res.string.folder_picker_unavailable
                                    ),
                                    fontWeight = FontWeight.Black,
                                )
                            }
                        }
                    }

                    importMessage?.let { msg ->
                        item {
                            Text(
                                text = msg,
                                color = HausColors.current.muted,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }

                    if (hasImportedTracks) {
                        item {
                            Button(
                                onClick = { showClearLibraryDialog = true },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                cornerRadius = 18.dp,
                                colors = ButtonDefaults.buttonColors(
                                    color = HausColors.current.pulse.copy(alpha = 0.15f),
                                    contentColor = HausColors.current.pulse,
                                ),
                            ) {
                                Text(
                                    text = stringResource(Res.string.clear_library),
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
        if (showClearLibraryDialog) {
            AnimatedClearLibraryDialogRoute(
                onDismiss = { showClearLibraryDialog = false },
                onClearLibrary = {
                    onClearLibrary()
                    showClearLibraryDialog = false
                },
                backdrop = clearLibraryDialogBackdrop,
            )
        }
        sourcePendingRemoval?.let { source ->
            RemoveSourceDialog(
                source = source,
                mutationsEnabled = mutationsEnabled,
                backdrop = clearLibraryDialogBackdrop,
                onDismiss = { sourcePendingRemoval = null },
                onConfirm = {
                    onRemoveSource(source)
                    sourcePendingRemoval = null
                },
            )
        }
    }
}

internal enum class SourceAccessLabel { Available, LostAccess }

internal enum class SourceScanLabel { NeverScanned, LastScanned }

internal fun sourceManagementLabels(source: LibrarySource): Pair<SourceAccessLabel, SourceScanLabel> =
    (if (source.accessStatus == LibrarySourceAccessStatus.Available) SourceAccessLabel.Available else SourceAccessLabel.LostAccess) to
        (if (source.lastScanAtEpochMillis == null) SourceScanLabel.NeverScanned else SourceScanLabel.LastScanned)

internal fun sourceMutationsEnabled(scanProgress: ScanProgress?): Boolean = scanProgress?.isActive != true

@Composable
private fun ConfiguredSourceRow(
    source: LibrarySource,
    mutationsEnabled: Boolean,
    onRescan: () -> Unit,
    onRemove: () -> Unit,
) {
    val displayName = source.displayName.ifBlank { source.handle }
    val labels = sourceManagementLabels(source)
    val accessLabel = when (labels.first) {
        SourceAccessLabel.Available -> stringResource(Res.string.source_access_available)
        SourceAccessLabel.LostAccess -> stringResource(Res.string.source_access_lost)
    }
    val scanLabel = when (labels.second) {
        SourceScanLabel.NeverScanned -> stringResource(Res.string.source_never_scanned)
        SourceScanLabel.LastScanned -> stringResource(Res.string.source_last_scanned)
    }
    val rescanDescription = stringResource(Res.string.rescan_source_format, displayName)
    val removeDescription = stringResource(Res.string.remove_source_format, displayName)
    val contentAlpha = if (mutationsEnabled) 1f else 0.42f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .background(HausColors.current.panel.copy(alpha = 0.54f), RoundedCornerShape(16.dp))
            .border(1.dp, HausColors.current.line, RoundedCornerShape(16.dp))
            .padding(start = 14.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = displayName,
                color = HausColors.current.ink,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(Res.string.source_status_format, accessLabel, scanLabel),
                color = HausColors.current.muted,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }
        IconButton(
            onClick = onRescan,
            enabled = mutationsEnabled,
            backgroundColor = Color.Transparent,
            minWidth = 44.dp,
            minHeight = 44.dp,
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = rescanDescription,
                tint = HausColors.current.ink.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
        }
        IconButton(
            onClick = onRemove,
            enabled = mutationsEnabled,
            backgroundColor = Color.Transparent,
            minWidth = 44.dp,
            minHeight = 44.dp,
        ) {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = removeDescription,
                tint = HausColors.current.pulse.copy(alpha = contentAlpha),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun RemoveSourceDialog(
    source: LibrarySource,
    mutationsEnabled: Boolean,
    backdrop: LayerBackdrop?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val displayName = source.displayName.ifBlank { source.handle }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HausColors.current.ink.copy(alpha = 0.36f))
            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        val dialogShape = RoundedCornerShape(24.dp)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .rhythHausLiquidGlass(
                    backdrop = backdrop,
                    shape = dialogShape,
                    fallbackColor = HausColors.current.panel.copy(alpha = 0.92f),
                )
                .pointerInput(Unit) { detectTapGestures(onTap = { }) },
            cornerRadius = 24.dp,
            colors = CardDefaults.defaultColors(color = HausColors.current.panel.copy(alpha = 0f)),
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = stringResource(Res.string.remove_folder),
                    color = HausColors.current.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.remove_folder_message, displayName),
                    color = HausColors.current.muted,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.height(44.dp),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.muted.copy(alpha = 0.15f),
                            contentColor = HausColors.current.muted,
                        ),
                    ) {
                        Text(stringResource(Res.string.cancel), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        onClick = onConfirm,
                        enabled = mutationsEnabled,
                        modifier = Modifier.height(44.dp),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.pulse,
                            contentColor = HausColors.current.paper,
                            disabledColor = HausColors.current.muted.copy(alpha = 0.28f),
                            disabledContentColor = HausColors.current.muted,
                        ),
                    ) {
                        Text(stringResource(Res.string.remove), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppearanceDropdown(
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
) {
    val options = RhythHausThemeMode.settingsOptions
    val selectedIndex = options.indexOf(currentThemeMode).coerceAtLeast(0)
    OverlayDropdownPreference(
        items = options.map { it.displayLabelResource() },
        selectedIndex = selectedIndex,
        title = stringResource(Res.string.appearance),
        summary = currentThemeMode.displayDescriptionResource(),
        modifier = Modifier.fillMaxWidth(),
        renderInRootScaffold = false,
        onSelectedIndexChange = { index ->
            options.getOrNull(index)?.let(onThemeModeSelected)
        },
    )
}

@Composable
private fun RhythHausThemeMode.displayLabelResource(): String = when (this) {
    RhythHausThemeMode.System -> stringResource(Res.string.theme_system_label)
    RhythHausThemeMode.Light -> stringResource(Res.string.theme_light_label)
    RhythHausThemeMode.Dark -> stringResource(Res.string.theme_dark_label)
}

@Composable
private fun RhythHausThemeMode.displayDescriptionResource(): String = when (this) {
    RhythHausThemeMode.System -> stringResource(Res.string.theme_system_description)
    RhythHausThemeMode.Light -> stringResource(Res.string.theme_light_description)
    RhythHausThemeMode.Dark -> stringResource(Res.string.theme_dark_description)
}
