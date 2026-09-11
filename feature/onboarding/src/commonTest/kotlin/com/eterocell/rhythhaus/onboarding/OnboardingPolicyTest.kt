package com.eterocell.rhythhaus.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingPolicyTest {
    @Test
    fun schemaOneHasFourOrderedPages() {
        assertEquals(
            listOf(
                OnboardingPage.LocalFirst,
                OnboardingPage.AddMusic,
                OnboardingPage.ScanAndFormats,
                OnboardingPage.Recovery),
            onboardingPages,
        )
    }

    @Test
    fun nextAndBackStayWithinTheFourPageBoundary() {
        assertEquals(1, nextOnboardingPage(0))
        assertEquals(3, nextOnboardingPage(3))
        assertEquals(0, previousOnboardingPage(0))
        assertEquals(2, previousOnboardingPage(3))
    }
}
