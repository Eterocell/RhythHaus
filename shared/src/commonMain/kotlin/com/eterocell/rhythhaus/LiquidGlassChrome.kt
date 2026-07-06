package com.eterocell.rhythhaus

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
internal fun rememberRhythHausBackdrop(): LayerBackdrop = rememberLayerBackdrop()

internal fun Modifier.recordRhythHausBackdrop(backdrop: LayerBackdrop): Modifier = layerBackdrop(backdrop)

internal const val RhythHausGlassSurfaceAlpha = 0.72f
internal val RhythHausGlassBlurRadius = 10.dp
internal val RhythHausGlassRefractionHeight = 16.dp
internal val RhythHausGlassRefractionAmount = 24.dp

internal fun Modifier.rhythHausLiquidGlass(
    backdrop: LayerBackdrop,
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = RhythHausGlassBlurRadius,
    refractionHeight: Dp = RhythHausGlassRefractionHeight,
    refractionAmount: Dp = RhythHausGlassRefractionAmount,
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
        vibrancy()
        blur(blurRadius.toPx())
        lens(refractionHeight.toPx(), refractionAmount.toPx())
    },
    onDrawSurface = {
        drawRect(fallbackColor)
    },
)
