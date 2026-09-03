package com.eterocell.rhythhaus.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import top.yukonga.miuix.kmp.blur.ProgressiveBlur

class LiquidGlassChromeTest {
    @Test
    fun uniformBlurStyleSelectsUniformEffectAndNoProgressiveComposite() {
        val plan = rhythHausGlassBlurRenderPlan(RhythHausGlassBlurStyle.Uniform)

        assertEquals(RhythHausGlassBlurStyle.Uniform, plan.style)
        assertEquals(RhythHausGlassBlurEffect.Uniform, plan.effect)
        assertNull(plan.progressiveGradient)
    }

    @Test
    fun topEdgeProgressiveBlurStyleSelectsProgressiveEffectAndMiuixTopComposite() {
        val plan =
            rhythHausGlassBlurRenderPlan(
                RhythHausGlassBlurStyle.TopEdgeProgressive)

        assertEquals(RhythHausGlassBlurStyle.TopEdgeProgressive, plan.style)
        assertEquals(RhythHausGlassBlurEffect.Progressive, plan.effect)
        assertSame(ProgressiveBlur.Top, plan.progressiveGradient)
    }

    @Test
    fun defaultBlurStylePlanUsesUniform() {
        assertEquals(
            RhythHausGlassBlurStyle.Uniform,
            rhythHausGlassBlurRenderPlan().style)
    }
}
