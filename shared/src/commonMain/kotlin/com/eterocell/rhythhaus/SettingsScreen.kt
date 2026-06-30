package com.eterocell.rhythhaus

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.library.PlatformFolderPickerLauncher
import com.eterocell.rhythhaus.library.ScanProgress
import kotlinx.coroutines.Job
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text

@Composable
fun SettingsScreen(
    folderPickerLauncher: PlatformFolderPickerLauncher,
    importMessage: String?,
    scanProgress: ScanProgress?,
    scanJob: Job?,
    hasImportedTracks: Boolean,
    onClearLibrary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HausPaper)
            .clickable(enabled = false, onClick = {}),
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = HausPaper) {
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
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(HausInk)
                            .hausClickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(
                            "< Back",
                            color = HausPaper,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = "Settings",
                        color = HausInk,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                    )
                }

                // Manage Music section
                Text(
                    text = "Manage Music",
                    color = HausInk,
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
                        color = HausInk,
                        contentColor = HausPaper,
                        disabledColor = HausMuted.copy(alpha = 0.28f),
                        disabledContentColor = HausMuted,
                    ),
                ) {
                    Text(
                        text = if (folderPickerLauncher.isAvailable) "Add music folder" else "Folder picker not available yet",
                        fontWeight = FontWeight.Black,
                    )
                }

                // Status message
                importMessage?.let { msg ->
                    Text(
                        text = msg,
                        color = HausMuted,
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
                        colors = ButtonDefaults.buttonColors(
                            color = HausPulse.copy(alpha = 0.15f),
                            contentColor = HausPulse,
                        ),
                    ) {
                        Text("Clear Library", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}
