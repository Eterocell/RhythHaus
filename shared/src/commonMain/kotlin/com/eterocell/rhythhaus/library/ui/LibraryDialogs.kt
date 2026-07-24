package com.eterocell.rhythhaus.library.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.ui.HausDialog
import org.jetbrains.compose.resources.stringResource
import rhythhaus.shared.generated.resources.Res
import rhythhaus.shared.generated.resources.cancel
import rhythhaus.shared.generated.resources.clear
import rhythhaus.shared.generated.resources.clear_library
import rhythhaus.shared.generated.resources.clear_library_message
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun AnimatedClearLibraryDialogRoute(
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
        },
        actions = {
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                modifier = Modifier.height(36.dp),
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
                modifier = Modifier.height(36.dp),
                cornerRadius = 12.dp,
                insideMargin =
                    PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                colors =
                    ButtonDefaults.buttonColors(
                        color = HausColors.current.pulse,
                        contentColor = HausColors.current.paper,
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
