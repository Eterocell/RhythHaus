package com.eterocell.rhythhaus.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingPlatformTest {
    @Test
    fun platformMappingIsClosedAndMatchesGuidance() {
        assertEquals(OnboardingPlatformGuidance.MacOS, OnboardingPlatform.MacOS.toGuidance())
        assertEquals(OnboardingPlatformGuidance.IOS, OnboardingPlatform.IOS.toGuidance())
        assertEquals(OnboardingPlatformGuidance.Android, OnboardingPlatform.Android.toGuidance())
    }
}
