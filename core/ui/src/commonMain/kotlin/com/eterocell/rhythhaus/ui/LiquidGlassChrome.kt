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

/**
 * Opaque handle for a supported Miuix backdrop; Miuix storage remains internal.
 */
public class RhythHausBackdrop
internal constructor(internal val layerBackdrop: LayerBackdrop)

/** Returns a backdrop handle, or null when render effects are unavailable. */
@Composable
public fun rememberRhythHausBackdrop(): RhythHausBackdrop? =
    if (isRenderEffectSupported()) RhythHausBackdrop(rememberLayerBackdrop())
    else null

/**
 * Records [backdrop] for later glass drawing and returns this modifier
 * unchanged without one.
 */
public fun Modifier.recordRhythHausBackdrop(
    backdrop: RhythHausBackdrop?
): Modifier =
    if (backdrop != null && isRenderEffectSupported()) {
        layerBackdrop(backdrop.layerBackdrop)
    } else {
        this
    }

/** Alpha applied to the public glass fallback surface. */
public const val RhythHausGlassSurfaceAlpha: Float = 0.72f
internal val RhythHausGlassBlurRadius = 10.dp
internal val RhythHausGlassRefractionHeight = 16.dp
internal val RhythHausGlassRefractionAmount = 24.dp

/**
 * Draws glass from [backdrop] or the fallback surface while preserving existing
 * visual values.
 */
public fun Modifier.rhythHausLiquidGlass(
    backdrop: RhythHausBackdrop?,
    shape: Shape,
    fallbackColor: Color,
    blurRadius: Dp = RhythHausGlassBlurRadius,
    refractionHeight: Dp = RhythHausGlassRefractionHeight,
    refractionAmount: Dp = RhythHausGlassRefractionAmount,
): Modifier =
    if (backdrop != null && isRuntimeShaderSupported()) {
        drawBackdrop(
            backdrop = backdrop.layerBackdrop,
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
