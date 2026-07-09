package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.rhythHausLiquidGlass
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.clear_library_message
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.blur.LayerBackdrop

@Composable
internal fun AnimatedClearLibraryDialogRoute(
    onDismiss: () -> Unit,
    onClearLibrary: () -> Unit,
    backdrop: LayerBackdrop?,
) {
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
                    text = stringResource(Res.string.clear_library),
                    color = HausColors.current.ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.clear_library_message),
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
                        modifier = Modifier.height(36.dp),
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
                        onClick = onClearLibrary,
                        modifier = Modifier.height(36.dp),
                        cornerRadius = 12.dp,
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                        colors = ButtonDefaults.buttonColors(
                            color = HausColors.current.pulse,
                            contentColor = HausColors.current.paper,
                        ),
                    ) {
                        Text(stringResource(Res.string.clear), fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
