package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import com.eterocell.rhythhaus.theme.HausColors
import com.eterocell.rhythhaus.theme.HausPulse

/**
 * Draws a focus boundary. Place before the modifier that creates the focus
 * target.
 */
@Composable
public fun Modifier.hausFocusIndicator(
    color: Color = HausColors.current.outline,
    shape: Shape = RectangleShape,
    includeDescendants: Boolean = false,
    outerColor: Color? = null,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val focusBorder =
        if (focused) {
            Modifier.drawWithContent {
                drawContent()
                if (outerColor == null || outerColor == color) {
                    drawFocusStroke(shape, inset = 1.dp.toPx(), color = color)
                } else {
                    drawFocusStroke(
                        shape,
                        inset = 1.dp.toPx(),
                        color = outerColor,
                    )
                    drawFocusStroke(
                        shape,
                        inset = 3.dp.toPx(),
                        color = color,
                    )
                }
            }
        } else Modifier
    return onFocusChanged {
            focused = if (includeDescendants) it.hasFocus else it.isFocused
        }
        .then(focusBorder)
}

private fun DrawScope.drawFocusStroke(
    shape: Shape,
    inset: Float,
    color: Color,
) {
    val strokeWidth = 2.dp.toPx()
    val outlineSize =
        Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2,
        )
    if (outlineSize.width <= strokeWidth || outlineSize.height <= strokeWidth) {
        return
    }
    withTransform({ translate(inset, inset) }) {
        drawOutline(
            outline = shape.createOutline(outlineSize, layoutDirection, this),
            color = color,
            style = Stroke(width = strokeWidth),
        )
    }
}

/** Applies the standard bounded RhythHaus click and focus indication. */
@Composable
public fun Modifier.hausClickable(onClick: () -> Unit): Modifier =
    hausClickable(onClick = onClick, focusColor = HausColors.current.outline)

/**
 * Applies the standard bounded RhythHaus click indication with an explicit
 * focus color.
 */
@Composable
public fun Modifier.hausClickable(
    onClick: () -> Unit,
    focusColor: Color,
): Modifier =
    hausClickable(
        onClick = onClick,
        focusColor = focusColor,
        outerColor = null,
    )

/**
 * Applies the standard click indication with contrasting inner and outer focus
 * bands.
 */
@Composable
public fun Modifier.hausClickable(
    onClick: () -> Unit,
    focusColor: Color,
    outerColor: Color?,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return hausFocusIndicator(color = focusColor, outerColor = outerColor)
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = HausPulse),
            onClick = onClick)
}

/**
 * Applies the standard bounded RhythHaus click, long-click, and focus
 * indications.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
public fun Modifier.hausCombinedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String
): Modifier =
    hausCombinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
        focusColor = HausColors.current.outline,
    )

/**
 * Applies the standard bounded click and long-click indication with an explicit
 * focus color.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
public fun Modifier.hausCombinedClickable(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onLongClickLabel: String,
    focusColor: Color,
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return hausFocusIndicator(color = focusColor)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = true, color = HausPulse),
            onClick = onClick,
            onLongClick = onLongClick,
            onLongClickLabel = onLongClickLabel)
}
