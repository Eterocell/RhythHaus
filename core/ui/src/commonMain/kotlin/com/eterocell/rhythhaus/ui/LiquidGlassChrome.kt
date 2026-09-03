package com.eterocell.rhythhaus.ui

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.blur
import top.yukonga.miuix.kmp.blur.drawBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveBlur
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

/** Selects the blur treatment used by a RhythHaus glass surface. */
public enum class RhythHausGlassBlurStyle {
    /** Applies the existing uniform blur across the surface. */
    Uniform,

    /** Applies a top-edge progressive blur that fades toward the lower edge. */
    TopEdgeProgressive,
}

internal enum class RhythHausGlassBlurEffect {
    Uniform,
    Progressive,
}

@Immutable
internal data class RhythHausGlassBlurRenderPlan(
    val style: RhythHausGlassBlurStyle,
    val effect: RhythHausGlassBlurEffect,
    val progressiveGradient: ProgressiveBlur?,
)

internal fun rhythHausGlassBlurRenderPlan(
    blurStyle: RhythHausGlassBlurStyle = RhythHausGlassBlurStyle.Uniform
): RhythHausGlassBlurRenderPlan =
    when (blurStyle) {
        RhythHausGlassBlurStyle.Uniform ->
            RhythHausGlassBlurRenderPlan(
                style = blurStyle,
                effect = RhythHausGlassBlurEffect.Uniform,
                progressiveGradient = null)
        RhythHausGlassBlurStyle.TopEdgeProgressive ->
            RhythHausGlassBlurRenderPlan(
                style = blurStyle,
                effect = RhythHausGlassBlurEffect.Progressive,
                progressiveGradient = ProgressiveBlur.Top)
    }

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
    blurStyle: RhythHausGlassBlurStyle = RhythHausGlassBlurStyle.Uniform,
): Modifier =
    if (backdrop != null && isRuntimeShaderSupported()) {
        val blurPlan = rhythHausGlassBlurRenderPlan(blurStyle)
        drawBackdrop(
            backdrop = backdrop.layerBackdrop,
            shape = { shape },
            effects = {
                when (blurPlan.effect) {
                    RhythHausGlassBlurEffect.Uniform -> blur(blurRadius.toPx())
                    RhythHausGlassBlurEffect.Progressive ->
                        progressiveBlur(
                            blurRadius.toPx(),
                            gradient =
                                checkNotNull(blurPlan.progressiveGradient))
                }
            },
            onDrawSurface = {
                drawRect(fallbackColor)
            },
            progressiveGradient = blurPlan.progressiveGradient,
        )
    } else {
        clip(shape).background(fallbackColor)
    }
