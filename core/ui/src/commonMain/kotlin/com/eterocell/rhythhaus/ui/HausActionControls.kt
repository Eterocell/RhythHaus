package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.eterocell.rhythhaus.theme.HausColors
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonColors
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults

/** Miuix button with the Studio focus boundary and matching control shape. */
@Composable
public fun HausButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = ButtonDefaults.CornerRadius,
    minWidth: Dp = ButtonDefaults.MinWidth,
    minHeight: Dp = ButtonDefaults.MinHeight,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    insideMargin: PaddingValues = ButtonDefaults.InsideMargin,
    interactionSource: MutableInteractionSource? = null,
    indication: Indication? = LocalIndication.current,
    content: @Composable RowScope.() -> Unit,
) {
    MiuixButton(
        onClick = onClick,
        modifier =
            modifier.hausFocusIndicator(
                color =
                    if (enabled) colors.contentColor
                    else colors.disabledContentColor,
                shape = RoundedCornerShape(cornerRadius),
                outerColor = HausColors.current.outline,
            ),
        enabled = enabled,
        cornerRadius = cornerRadius,
        minWidth = minWidth,
        minHeight = minHeight,
        colors = colors,
        insideMargin = insideMargin,
        interactionSource = interactionSource,
        indication = indication,
        content = content,
    )
}

/** Miuix icon button with the Studio focus boundary. */
@Composable
public fun HausIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    holdDownState: Boolean = false,
    backgroundColor: Color = Color.Unspecified,
    cornerRadius: Dp = IconButtonDefaults.CornerRadius,
    minHeight: Dp = IconButtonDefaults.MinHeight,
    minWidth: Dp = IconButtonDefaults.MinWidth,
    focusColor: Color = HausColors.current.outline,
    content: @Composable () -> Unit,
) {
    val disabledSemantics =
        if (enabled) {
            Modifier
        } else {
            Modifier.semantics(mergeDescendants = true) {
                role = Role.Button
                disabled()
            }
        }
    MiuixIconButton(
        onClick = onClick,
        modifier =
            modifier
                .then(disabledSemantics)
                .hausFocusIndicator(
                    color = focusColor,
                    shape = RoundedCornerShape(cornerRadius),
                ),
        enabled = enabled,
        holdDownState = holdDownState,
        backgroundColor = backgroundColor,
        cornerRadius = cornerRadius,
        minHeight = minHeight,
        minWidth = minWidth,
        content = content,
    )
}
