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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.ui.BackChip
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.RhythHausThemeMode
import com.eterocell.rhythhaus.ui.hausClickable
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import kotlinx.coroutines.Job
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.add_music_folder
import rhythhaus.shared.generated.resources.appearance
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.folder_picker_unavailable
import rhythhaus.shared.generated.resources.manage_music
import rhythhaus.shared.generated.resources.selected
import rhythhaus.shared.generated.resources.settings
import rhythhaus.shared.generated.resources.theme_dark_description
import rhythhaus.shared.generated.resources.theme_dark_label
import rhythhaus.shared.generated.resources.theme_light_description
import rhythhaus.shared.generated.resources.theme_light_label
import rhythhaus.shared.generated.resources.theme_system_description
import rhythhaus.shared.generated.resources.theme_system_label
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import com.eterocell.rhythhaus.library.ui.ScanningCard

@Composable
fun SettingsScreen(
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    hasImportedTracks: Boolean,
    currentThemeMode: RhythHausThemeMode,
    onThemeModeSelected: (RhythHausThemeMode) -> Unit,
    onClearLibrary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HausColors.current.paper)
            .clickable(enabled = false, onClick = {}),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausColors.current.paper) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeContentPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Title bar with back button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackChip(onClick = onDismiss)
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = stringResource(Res.string.settings),
                        color = HausColors.current.ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Appearance section
                Text(
                    text = stringResource(Res.string.appearance),
                    color = HausColors.current.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )

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
                        onCancel = { scanJob?.cancel() },
                    )
                }

                // Add music folder button
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
                        text = if (folderPickerLauncher.isAvailable) stringResource(Res.string.add_music_folder) else stringResource(Res.string.folder_picker_unavailable),
                        fontWeight = FontWeight.Black,
                    )
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
                        onClick = onClearLibrary,
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.pulse.copy(alpha = 0.15f),
                            contentColor = HausColors.current.pulse,
                        ),
                    ) {
                        Text(stringResource(Res.string.clear_library), fontSize = 13.sp, fontWeight = FontWeight.Black)
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
    var expanded by remember { mutableStateOf(false) }
    val colors = HausColors.current
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.panel)
            .border(width = 1.dp, color = if (expanded) colors.pulse else colors.line, shape = shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .hausClickable(onClick = { expanded = !expanded })
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentThemeMode.displayLabelResource(),
                    color = colors.ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = currentThemeMode.displayDescriptionResource(),
                    color = colors.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = colors.pulse,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
            )
        }

        if (expanded) {
            RhythHausThemeMode.settingsOptions.forEach { mode ->
                AppearanceDropdownOption(
                    mode = mode,
                    selected = mode == currentThemeMode,
                    onSelected = {
                        onThemeModeSelected(mode)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AppearanceDropdownOption(
    mode: RhythHausThemeMode,
    selected: Boolean,
    onSelected: () -> Unit,
) {
    val colors = HausColors.current
    val fillColor = if (selected) colors.pulse.copy(alpha = 0.14f) else colors.panel
    val labelColor = if (selected) colors.pulse else colors.ink

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(fillColor)
            .hausClickable(onClick = onSelected)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mode.displayLabelResource(),
                color = labelColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            )
        }
        Text(
            text = if (selected) stringResource(Res.string.selected) else "",
            color = labelColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
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
