package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.shader.isRenderEffectSupported
import top.yukonga.miuix.kmp.shader.isRuntimeShaderSupported

@Composable
internal fun rememberRhythHausBackdrop(): LayerBackdrop? =
    if (isRenderEffectSupported()) rememberLayerBackdrop() else null

internal fun Modifier.recordRhythHausBackdrop(
    backdrop: LayerBackdrop?
): Modifier =
    if (backdrop != null && isRenderEffectSupported()) {
        layerBackdrop(backdrop)
    } else {
        this
    }

internal const val RhythHausGlassSurfaceAlpha = 0.72f
internal val RhythHausGlassBlurRadius = 10.dp
internal val RhythHausGlassRefractionHeight = 16.dp
internal val RhythHausGlassRefractionAmount = 24.dp

internal fun Modifier.rhythHausLiquidGlass(
    backdrop: LayerBackdrop?,
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = RhythHausGlassBlurRadius,
    refractionHeight: Dp = RhythHausGlassRefractionHeight,
    refractionAmount: Dp = RhythHausGlassRefractionAmount,
): Modifier =
    if (backdrop != null && isRuntimeShaderSupported()) {
        drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                blur(blurRadius.toPx())
            },
            onDrawSurface = {
                drawRect(fallbackColor)
            },
        )
    } else {
        clip(shape).background(fallbackColor)
    }
