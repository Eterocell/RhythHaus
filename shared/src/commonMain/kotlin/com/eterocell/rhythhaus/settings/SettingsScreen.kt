package com.eterocell.rhythhaus.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.LibrarySource
import com.eterocell.rhythhaus.library.ScanProgress
import com.eterocell.rhythhaus.library.ui.AnimatedClearLibraryDialogRoute
import com.eterocell.rhythhaus.library.ui.ScanningCard
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.RhythHausTopAppBar
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.appearance
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.manage_music
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.theme_dark_description
import rhythhaus.shared.generated.resources.theme_dark_label
import rhythhaus.shared.generated.resources.theme_light_description
import rhythhaus.shared.generated.resources.theme_light_label
import rhythhaus.shared.generated.resources.theme_system_description
import rhythhaus.shared.generated.resources.theme_system_label
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeContentPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    RhythHausTopAppBar(
                        title = stringResource(Res.string.settings),
                        onBack = onDismiss,
                    )

                    // Appearance preference
                    AppearanceDropdown(
                        currentThemeMode = currentThemeMode,
                        onThemeModeSelected = onThemeModeSelected,
                    )

                    // Manage Music section
                    Text(
                        text = stringResource(Res.string.manage_music),
                        color = HausColors.current.ink,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                    )

                    // Scanning card
                    if (scanProgress?.isActive == true) {
                        val sp = scanProgress
                        val ss = sp.session!!
                        ScanningCard(
                            foldersVisited = ss.foldersVisited,
                            filesVisited = ss.filesVisited,
                            tracksAdded = ss.tracksAdded,
                            latestItem = sp.latestItem,
                            onCancel = onCancelScan,
                        )
                    }

                    // Add music folder button
                    if (sourcePickerActionVisible) {
                        Button(
                            onClick = folderPickerLauncher::launch,
                            enabled = folderPickerLauncher.isAvailable && scanProgress?.isActive != true,
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

                    // Status message
                    importMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = HausColors.current.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    // Clear Library
                    if (hasImportedTracks) {
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
