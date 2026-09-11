package com.eterocell.rhythhaus.onboarding

import kotlin.test.Test
import kotlin.test.assertTrue

class OnboardingResourceOwnershipJvmTest {
    @Test
    fun englishAndChineseResourcesAreOwnedByOnboarding() {
        val root = java.io.File("src/commonMain/composeResources")
        assertTrue(java.io.File(root, "values/strings.xml").isFile)
        assertTrue(java.io.File(root, "values-zh/strings.xml").isFile)
    }
}
