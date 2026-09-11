package com.eterocell.rhythhaus.onboarding

internal enum class OnboardingPlatform {
    Android,
    IOS,
    MacOS
}

internal expect fun currentOnboardingPlatform(): OnboardingPlatform

internal fun OnboardingPlatform.toGuidance(): OnboardingPlatformGuidance =
    when (this) {
        OnboardingPlatform.Android -> OnboardingPlatformGuidance.Android
        OnboardingPlatform.IOS -> OnboardingPlatformGuidance.IOS
        OnboardingPlatform.MacOS -> OnboardingPlatformGuidance.MacOS
    }
