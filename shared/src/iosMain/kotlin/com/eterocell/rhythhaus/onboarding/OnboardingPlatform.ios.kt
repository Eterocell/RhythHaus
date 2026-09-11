package com.eterocell.rhythhaus.onboarding

import kotlin.native.OsFamily
import kotlin.native.Platform
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
internal actual fun currentOnboardingPlatform(): OnboardingPlatform =
    if (Platform.osFamily == OsFamily.IOS) OnboardingPlatform.IOS else OnboardingPlatform.MacOS
